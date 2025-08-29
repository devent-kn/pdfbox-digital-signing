package com.example.fbPdf.service;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.cms.Attribute;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.tsp.*;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

@Service
public class TSAClient {

    private static final String TSA_URL = "https://freetsa.org/tsr";

    /**
     * Gửi request tới TSA và trả về TimeStampToken cho chữ ký.
     */
    public TimeStampToken getTimeStampToken(byte[] signature) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(signature);

        TimeStampRequestGenerator tsqGenerator = new TimeStampRequestGenerator();
        tsqGenerator.setCertReq(true);
        BigInteger nonce = BigInteger.valueOf(System.currentTimeMillis());
        TimeStampRequest tsReq = tsqGenerator.generate(TSPAlgorithms.SHA256, digest, nonce);

        byte[] requestBytes = tsReq.getEncoded();

        URL url = new URL(TSA_URL);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/timestamp-query");
        con.setRequestProperty("Content-Transfer-Encoding", "binary");
        con.getOutputStream().write(requestBytes);

        try (InputStream in = con.getInputStream()) {
            TimeStampResponse tsRes = new TimeStampResponse(in);
            tsRes.validate(tsReq);
            return tsRes.getTimeStampToken();
        }
    }

    /**
     * Gắn timestamp vào chữ ký CMS (UnsignedAttributes).
     */
    public CMSSignedData addTimestamp(CMSSignedData signedData) throws Exception {
        SignerInformation signerInfo = signedData.getSignerInfos().getSigners().iterator().next();
        byte[] signature = signerInfo.getSignature();

        TimeStampToken tsToken = getTimeStampToken(signature);

        Attribute timeStampAttr = new Attribute(
                PKCSObjectIdentifiers.id_aa_signatureTimeStampToken,
                new DERSet(ASN1Primitive.fromByteArray(tsToken.getEncoded()))
        );

        ASN1EncodableVector unsignedAttrsVector = signerInfo.getUnsignedAttributes() != null
                ? signerInfo.getUnsignedAttributes().toASN1EncodableVector()
                : new ASN1EncodableVector();

        unsignedAttrsVector.add(timeStampAttr);
        AttributeTable newUnsignedAttrs = new AttributeTable(unsignedAttrsVector);

        SignerInformation newSignerInfo =
                SignerInformation.replaceUnsignedAttributes(signerInfo, newUnsignedAttrs);

        SignerInformationStore newSignerStore = new SignerInformationStore(newSignerInfo);
        return CMSSignedData.replaceSigners(signedData, newSignerStore);
    }
}

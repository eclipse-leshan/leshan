/*******************************************************************************
 * Copyright (c) 2017 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.bootstrap.demo;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.lang.math.RandomUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.eclipse.leshan.server.californium.impl.PkiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class PkiServiceMock implements PkiService {

    private static final Logger LOG = LoggerFactory.getLogger(PkiServiceMock.class);

    private final PrivateKey caKey;
    private final X509Certificate caCert;

    public PkiServiceMock(PrivateKey caKey, X509Certificate caCert) {
        this.caKey = caKey;
        this.caCert = caCert; // caSelfSignedCert?

        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public byte[] getCaCertificates() {
        return null;
    }

    @Override
    public byte[] enroll(byte[] csr, String commonName, Map<String, Object> attributes) {

        // open the PCKS10 (Certificate Signing Request)
        CertificationRequest cr = null;
        try {
            cr = new CertificationRequest(ASN1Sequence.getInstance(csr));
        } catch (IllegalArgumentException e) {
            LOG.error("Could not decode CSR", e);
            throw new IllegalArgumentException("Corrupted CSR");
        }

        try {
            // get public key from CSR
            SubjectPublicKeyInfo publicKeyInfo = cr.getCertificationRequestInfo().getSubjectPublicKeyInfo();
            if (!X9ObjectIdentifiers.id_ecPublicKey.getId()
                    .equals(publicKeyInfo.getAlgorithm().getAlgorithm().getId())) {
                throw new IllegalArgumentException("Only EC public key is supported");
            }
            PublicKey publicKey = KeyFactory.getInstance("ECDSA", "BC")
                    .generatePublic(new X509EncodedKeySpec(publicKeyInfo.getEncoded()));

            // common name
            String csrIdentity = cr.getCertificationRequestInfo().getSubject()
                    .getRDNs(X509ObjectIdentifiers.commonName)[0].getFirst().getValue().toString();

            Calendar cal = Calendar.getInstance();
            Date startDate = cal.getTime();
            cal.add(10, Calendar.YEAR);
            Date expiryDate = cal.getTime();
            BigInteger serialNumber = BigInteger.valueOf(RandomUtils.nextLong());

            X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
            X500Principal subjectName = new X500Principal("CN=" + csrIdentity);

            certGen.setSerialNumber(serialNumber);
            certGen.setIssuerDN(caCert.getSubjectX500Principal());
            certGen.setNotBefore(startDate);
            certGen.setNotAfter(expiryDate);
            certGen.setSubjectDN(subjectName);
            certGen.setPublicKey(publicKey);
            certGen.setSignatureAlgorithm("SHA256withECDSA");

            for (Entry<String, Object> a : attributes.entrySet()) {
                // TODO fix cast
                certGen.addExtension(a.getKey(), false, (ASN1Encodable) a.getValue());
            }

            // TODO
            // certGen.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
            // new AuthorityKeyIdentifierStructure(caCert));
            // certGen.addExtension(X509Extensions.SubjectKeyIdentifier, false,
            // new SubjectKeyIdentifierStructure(keyPair.getPublic());

            return certGen.generate(caKey, "BC").getEncoded();

        } catch (InvalidKeySpecException | InvalidKeyException e) {
            throw new IllegalArgumentException(e);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new IllegalStateException(e);
        } catch (SignatureException | CertificateEncodingException | IOException e) {
            // TODO proper exceptions
            throw new RuntimeException(e);
        }
    }

}

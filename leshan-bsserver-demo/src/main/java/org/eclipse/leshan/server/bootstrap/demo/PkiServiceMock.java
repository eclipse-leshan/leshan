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
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.math.RandomUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.leshan.server.californium.impl.PkiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PkiServiceMock implements PkiService {

    private static final Logger LOG = LoggerFactory.getLogger(PkiServiceMock.class);

    private final PrivateKey caKey;
    private final X509Certificate caCert;

    public PkiServiceMock(PrivateKey caKey, X509Certificate caCert) {
        this.caKey = caKey;
        this.caCert = caCert; // self-signed CA certificate
    }

    @Override
    public byte[] getCaCertificates() {
        try {
            return new CertificateFactory().engineGenerateCertPath(Arrays.asList(caCert)).getEncoded();
        } catch (CertificateException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public byte[] enroll(byte[] csr, String commonName, Map<String, Object> attributes) {

        // open the PCKS10 (Certificate Signing Request)
        JcaPKCS10CertificationRequest cr;
        try {
            LOG.debug("Received CSR:\n{}", toPem("CERTIFICATE REQUEST", csr));
            cr = new JcaPKCS10CertificationRequest(csr);
        } catch (IOException e1) {
            throw new IllegalArgumentException("Corrupted CSR", e1);
        }

        try {
            if (!X9ObjectIdentifiers.id_ecPublicKey.getId()
                    .equals(cr.getSubjectPublicKeyInfo().getAlgorithm().getAlgorithm().getId())) {
                throw new IllegalArgumentException("Only EC public key is supported");
            }

            // get public key
            X509EncodedKeySpec xspec = new X509EncodedKeySpec(cr.getSubjectPublicKeyInfo().getEncoded());
            PublicKey pub = new DefaultJcaJceHelper().createKeyFactory("EC").generatePublic(xspec);

            // use the common name parameter instead?
            String csrIdentity = cr.getSubject().getRDNs(X509ObjectIdentifiers.commonName)[0].getFirst().getValue()
                    .toString();

            Calendar cal = Calendar.getInstance();
            Date startDate = cal.getTime();
            cal.add(Calendar.YEAR, 10);
            Date expiryDate = cal.getTime();
            BigInteger serialNumber = BigInteger.valueOf(RandomUtils.nextLong());

            @SuppressWarnings("deprecation")
            X500Name issuer = new X500Name(PrincipalUtil.getSubjectX509Principal(caCert).getName());
            X500Name subject = new X500Name("CN=" + csrIdentity);
            JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuer, serialNumber, startDate,
                    expiryDate, subject, pub);

            builder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature));
            // TODO attributes/extensions

            ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(caKey);

            return builder.build(signer).getEncoded();

        } catch (IOException | OperatorCreationException | GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private String toPem(String objecType, byte[] content) throws IOException {
        try (StringWriter writer = new StringWriter()) {
            PemWriter pemWriter = new PemWriter(writer);
            pemWriter.writeObject(new PemObject(objecType, content));
            pemWriter.flush();
            pemWriter.close();
            return writer.toString();
        }
    }

}

package org.eclipse.leshan.core.util;

import org.junit.Assert;
import org.junit.Test;

import javax.security.auth.x500.X500Principal;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class X509CertUtilTest {

    private final KeyStore keyStore;

    public X509CertUtilTest() {
        // prepare key store for access
        try {
            // Get certificates from key store
            char[] keyStorePwd = "secret".toCharArray();
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (FileInputStream keyStoreFile = new FileInputStream("./certificates/certificates.jks")) {
                keyStore.load(keyStoreFile, keyStorePwd);
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads X.509 PEM Certificate
     *
     * @param filename File name to load
     * @return Valid X.509 Certificate object or null
     */
    private X509Certificate loadX509PemCertificate(String filename) {
        X509Certificate certificate = null;

        try {
            CertificateFactory fact = CertificateFactory.getInstance("X.509");
            FileInputStream is = new FileInputStream(filename);
            certificate = (X509Certificate) fact.generateCertificate(is);
        } catch (CertificateException | FileNotFoundException e) {
            e.printStackTrace();
        }

        return certificate;
    }

    @Test
    public void principal_simple_cn() {
        X500Principal simple = new X500Principal("CN=simple");
        Assert.assertEquals("simple", X509CertUtil.getPrincipalField(simple, "CN"));
    }

    @Test
    public void principal_devurn_cn() {
        X500Principal dn = new X500Principal("CN=urn:dev:ops:32473-Refrigerator-5002,O=Organization,C=US");
        Assert.assertEquals("urn:dev:ops:32473-Refrigerator-5002", X509CertUtil.getPrincipalField(dn, "CN"));
        Assert.assertEquals("Organization", X509CertUtil.getPrincipalField(dn, "O"));
        Assert.assertEquals("US", X509CertUtil.getPrincipalField(dn, "C"));
    }

    @Test
    public void principal_ieee802_1ar() {
        X500Principal dn = new X500Principal("SERIALNUMBER=P123456,CN=MyDevice,O=Company,C=US");
        Assert.assertEquals("P123456", X509CertUtil.getPrincipalField(dn, "SERIALNUMBER"));
        Assert.assertEquals("MyDevice", X509CertUtil.getPrincipalField(dn, "CN"));
        Assert.assertEquals("Company", X509CertUtil.getPrincipalField(dn, "O"));
        Assert.assertEquals("US", X509CertUtil.getPrincipalField(dn, "C"));
    }

    @Test
    public void principal_rfc2253_examples() {
        X500Principal example1 = new X500Principal("CN=Steve Kille,O=Isode Limited,C=GB");
        Assert.assertEquals("Steve Kille", X509CertUtil.getPrincipalField(example1, "CN"));
        Assert.assertEquals("Isode Limited", X509CertUtil.getPrincipalField(example1, "O"));
        Assert.assertEquals("GB", X509CertUtil.getPrincipalField(example1, "C"));

        X500Principal example2 = new X500Principal("OU=Sales+CN=J. Smith,O=Widget Inc.,C=US");
        Assert.assertEquals("J. Smith", X509CertUtil.getPrincipalField(example2, "CN"));
        Assert.assertEquals("Sales", X509CertUtil.getPrincipalField(example2, "OU"));
        Assert.assertEquals("Widget Inc.", X509CertUtil.getPrincipalField(example2, "O"));
        Assert.assertEquals("US", X509CertUtil.getPrincipalField(example2, "C"));

        X500Principal example3 = new X500Principal("CN=L. Eagle,O=Sue\\, Grabbit and Runn,C=GB");
        Assert.assertEquals("L. Eagle", X509CertUtil.getPrincipalField(example3, "CN"));
        Assert.assertEquals("Sue, Grabbit and Runn", X509CertUtil.getPrincipalField(example3, "O"));
        Assert.assertEquals("GB", X509CertUtil.getPrincipalField(example3, "C"));

        X500Principal example4 = new X500Principal("CN=Before\\0DAfter,O=Test,C=GB");
        Assert.assertEquals("Before\rAfter", X509CertUtil.getPrincipalField(example4, "CN"));
        Assert.assertEquals("Test", X509CertUtil.getPrincipalField(example4, "O"));
        Assert.assertEquals("GB", X509CertUtil.getPrincipalField(example4, "C"));

        X500Principal example5 = new X500Principal("1.3.6.1.4.1.1466.0=#04024869,O=Test,C=GB");
        // Note: ASN.1 tag 0x04 == OCTET STRING
        Assert.assertEquals("#04024869", X509CertUtil.getPrincipalField(example5, "1.3.6.1.4.1.1466.0"));
        Assert.assertEquals("Test", X509CertUtil.getPrincipalField(example5, "O"));
        Assert.assertEquals("GB", X509CertUtil.getPrincipalField(example5, "C"));

        X500Principal example6 = new X500Principal("SURNAME=Lu\\C4\\8Di\\C4\\87");
        // Note: Full blown parser would be able to extract proper string for "SURNAME" -- but we are limited to
        // standard Java's form. Using OID form of "SURNAME" as that is what is given by X500Principal.getName()
        // Note: ASN.1 tag 0x0c == UTF8String
        Assert.assertEquals("#0c074c75c48d69c487", X509CertUtil.getPrincipalField(example6, "2.5.4.4"));
    }

    @Test
    public void x509_simple_dns_name_test() throws KeyStoreException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("server");

        Assert.assertTrue(X509CertUtil.matchSubjectDnsName(certificate, "server.mydomain.com"));
        Assert.assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "another.domain.com"));
        Assert.assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "localhost"));
    }

    @Test
    public void x509_san_dns_name_test() throws KeyStoreException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("server_with_san");

        Assert.assertTrue(X509CertUtil.matchSubjectDnsName(certificate, "server.mydomain.com"));
        Assert.assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "another.domain.com"));
        Assert.assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "localhost"));
    }

    @Test
    public void x509_san_ipv4_test() throws KeyStoreException, UnknownHostException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("server_with_san");

        Assert.assertTrue(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("192.168.1.42")));
        Assert.assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("127.0.0.1")));
        Assert.assertFalse(X509CertUtil.matchSubjectInetAddress(certificate,
                InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334")));
        Assert.assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("2001:db8:1234::")));
        Assert.assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("::1")));
    }

    @Test
    public void x509_san_ipv6_test() throws KeyStoreException, UnknownHostException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("server_with_ipv6");

        Assert.assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("192.168.1.42")));
        Assert.assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("127.0.0.1")));
        Assert.assertTrue(X509CertUtil.matchSubjectInetAddress(certificate,
                InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334")));
        Assert.assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("2001:db8:1234::")));
        Assert.assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("::1")));
    }

    @Test
    public void x509_san_combo_test() throws KeyStoreException, UnknownHostException {
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("localhost");

        Assert.assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "server.mydomain.com"));
        Assert.assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "another.domain.com"));
        Assert.assertTrue(X509CertUtil.matchSubjectDnsName(certificate, "localhost"));

        Assert.assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("192.168.1.42")));
        Assert.assertTrue(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("127.0.0.1")));
        Assert.assertFalse(X509CertUtil.matchSubjectInetAddress(certificate,
                InetAddress.getByName("2001:0db8:85a3:0000:0000:8a2e:0370:7334")));
        Assert.assertFalse(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("2001:db8:1234::")));
        Assert.assertTrue(X509CertUtil.matchSubjectInetAddress(certificate, InetAddress.getByName("::1")));
    }

    @Test
    public void x509_san_wildcard_test() throws KeyStoreException, UnknownHostException {
        X509Certificate certificate = loadX509PemCertificate("./certificates/eclipse.org.pem");

        Assert.assertTrue(X509CertUtil.matchSubjectDnsName(certificate, "eclipse.org"));
        Assert.assertTrue(X509CertUtil.matchSubjectDnsName(certificate, "server.eclipse.org"));
        Assert.assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "server.subdomain.eclipse.org"));
        Assert.assertFalse(X509CertUtil.matchSubjectDnsName(certificate, "sub.server.eclipse.org"));
    }
}

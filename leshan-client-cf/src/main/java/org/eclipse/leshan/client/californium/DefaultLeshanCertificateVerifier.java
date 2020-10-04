package org.eclipse.leshan.client.californium;

import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.x509.CertificateVerifier;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class DefaultLeshanCertificateVerifier implements CertificateVerifier {
    private final Certificate expectedServerCertificate;

    public DefaultLeshanCertificateVerifier(Certificate expectedServerCertificate) {
        this.expectedServerCertificate = expectedServerCertificate;
    }

    @Override
    public void verifyCertificate(CertificateMessage message, DTLSSession session)
            throws HandshakeException {
        // As specify in the LWM2M spec 1.0, we only support "domain-issued certificate" usage
        // Defined in : https://tools.ietf.org/html/rfc6698#section-2.1.1 (3 -- Certificate usage 3)

        // Get server certificate from certificate message
        if (message.getCertificateChain().getCertificates().size() == 0) {
            AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE,
                    session.getPeer());
            throw new HandshakeException(
                    "Certificate chain could not be validated : server cert chain is empty", alert);
        }
        Certificate receivedServerCertificate = message.getCertificateChain().getCertificates().get(0);

        // Validate certificate
        if (!expectedServerCertificate.equals(receivedServerCertificate)) {
            AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE,
                    session.getPeer());
            throw new HandshakeException(
                    "Certificate chain could not be validated: server certificate does not match expected one ('domain-issue certificate' usage)",
                    alert);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}

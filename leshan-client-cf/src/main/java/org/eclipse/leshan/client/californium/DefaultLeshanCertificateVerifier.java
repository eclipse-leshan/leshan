package org.eclipse.leshan.client.californium;

import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.eclipse.californium.scandium.dtls.AlertMessage;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertDescription;
import org.eclipse.californium.scandium.dtls.AlertMessage.AlertLevel;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.CertificateType;
import org.eclipse.californium.scandium.dtls.CertificateVerificationResult;
import org.eclipse.californium.scandium.dtls.ConnectionId;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.HandshakeResultHandler;
import org.eclipse.californium.scandium.dtls.x509.NewAdvancedCertificateVerifier;
import org.eclipse.californium.scandium.util.ServerNames;

public class DefaultLeshanCertificateVerifier implements NewAdvancedCertificateVerifier {

    private final Certificate expectedServerCertificate;
    private final List<CertificateType> supportedCertificateType;

    public DefaultLeshanCertificateVerifier(Certificate expectedServerCertificate) {
        this.expectedServerCertificate = expectedServerCertificate;
        this.supportedCertificateType = new ArrayList<>(1);
        this.supportedCertificateType.add(CertificateType.X_509);
    }

    @Override
    public List<X500Principal> getAcceptedIssuers() {
        return null;
    }

    @Override
    public List<CertificateType> getSupportedCertificateType() {
        return supportedCertificateType;
    }

    @Override
    public CertificateVerificationResult verifyCertificate(ConnectionId cid, ServerNames serverName,
            Boolean clientUsage, boolean truncateCertificatePath, CertificateMessage message, DTLSSession session) {
        // As specify in the LWM2M spec 1.0, we only support "domain-issued certificate" usage
        // Defined in : https://tools.ietf.org/html/rfc6698#section-2.1.1 (3 -- Certificate usage 3)

        // Get server certificate from certificate message
        if (message.getCertificateChain() == null || message.getCertificateChain().getCertificates().size() == 0) {
            AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE,
                    session.getPeer());
            HandshakeException handshakeException = new HandshakeException(
                    "Certificate chain could not be validated : server cert chain is empty", alert);
            return new CertificateVerificationResult(cid, handshakeException, null);
        }
        Certificate receivedServerCertificate = message.getCertificateChain().getCertificates().get(0);

        // Validate certificate
        if (!expectedServerCertificate.equals(receivedServerCertificate)) {
            AlertMessage alert = new AlertMessage(AlertLevel.FATAL, AlertDescription.BAD_CERTIFICATE,
                    session.getPeer());
            HandshakeException handshakeException = new HandshakeException(
                    "Certificate chain could not be validated: server certificate does not match expected one ('domain-issue certificate' usage)",
                    alert);
            return new CertificateVerificationResult(cid, handshakeException, null);
        }

        return new CertificateVerificationResult(cid, message.getCertificateChain(), null);
    }

    @Override
    public void setResultHandler(HandshakeResultHandler resultHandler) {
        // we don't use async mode.
    }
}

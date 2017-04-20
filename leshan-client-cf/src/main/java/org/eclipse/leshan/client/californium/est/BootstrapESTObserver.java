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
package org.eclipse.leshan.client.californium.est;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.pkcs.CertificationRequest;
import org.bouncycastle.asn1.pkcs.CertificationRequestInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.config.NetworkConfig.Keys;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig.Builder;
import org.eclipse.leshan.SecurityMode;
import org.eclipse.leshan.client.californium.impl.CoapEndpoints;
import org.eclipse.leshan.client.observer.LwM2mClientObserverAdapter;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.servers.DmServerInfo;
import org.eclipse.leshan.client.servers.ServerInfo;
import org.eclipse.leshan.client.servers.ServersInfo;
import org.eclipse.leshan.client.servers.ServersInfoExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class in charge of performing the device enrollment (EST - Enrollment over Secure Transport) to acquire a
 * certificate.
 */
public class BootstrapESTObserver extends LwM2mClientObserverAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(BootstrapESTObserver.class);

    private final String endpoint;
    private final Map<Integer, LwM2mObjectEnabler> objectEnablers;

    private final CoapEndpoints coapEndpoints;

    public BootstrapESTObserver(String endpoint, Map<Integer, LwM2mObjectEnabler> objectEnablers,
            CoapEndpoints coapEndpoints) {
        this.objectEnablers = objectEnablers;
        this.endpoint = endpoint;
        this.coapEndpoints = coapEndpoints;
    }

    @Override
    public void onBootstrapSuccess(ServerInfo bsserver) {

        ServersInfo serversInfo = ServersInfoExtractor.getInfo(objectEnablers);

        // EST server is BS server
        InetSocketAddress estServerAddress = serversInfo.bootstrap.getAddress();

        // enroll if one DM server with EST security type (or BS server?)
        DmServerInfo dmInfo = serversInfo.deviceMangements.values().iterator().next();

        if (dmInfo.secureMode == SecurityMode.EST) {

            try {
                // generate key pair
                AlgorithmParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
                kpg.initialize(ecSpec, new SecureRandom());
                KeyPair pair = kpg.generateKeyPair();

                SubjectPublicKeyInfo pkInfo = SubjectPublicKeyInfo
                        .getInstance(ASN1Sequence.getInstance(pair.getPublic().getEncoded()));
                CertificationRequestInfo crInfo = new CertificationRequestInfo(new X500Name("CN=" + endpoint), pkInfo,
                        new DERSet());

                // CSR signature
                Signature signature = Signature.getInstance("SHA256withECDSA");
                signature.initSign(pair.getPrivate());
                signature.update(crInfo.getEncoded(ASN1Encoding.DER));

                CertificationRequest csr = new CertificationRequest(crInfo,
                        new AlgorithmIdentifier(X9ObjectIdentifiers.ecdsa_with_SHA256),
                        new DERBitString(signature.sign()));
                LOG.trace("Certificate request:\n{}", toPem("CERTIFICATE REQUEST", csr.getEncoded()));

                // Enrollment over CoAP
                Request csrRequest = new Request(Code.POST);
                csrRequest.setDestination(estServerAddress.getAddress());
                csrRequest.setDestinationPort(estServerAddress.getPort());
                csrRequest.getOptions().addUriPath("est");
                csrRequest.getOptions().addUriPath("sen");
                csrRequest.setPayload(csr.getEncoded());
                coapEndpoints.getSecureEndpoint().sendRequest(csrRequest);

                Response csrResponse = csrRequest.waitForResponse(5000L);

                if (csrResponse.getCode() != ResponseCode.CONTENT) {
                    LOG.error("Enrollment failed: {}", csrResponse);
                    // return error
                    return;
                }
                LOG.trace("Certificate:\n{}", toPem("CERTIFICATE REQUEST", csrResponse.getPayload()));

                Certificate publicKey = new CertificateFactory()
                        .engineGenerateCertificate(new ByteArrayInputStream(csrResponse.getPayload()));
                LOG.debug("Certificate:\n{}", publicKey);

                // get CA certificates chain
                Request caCertsRequest = new Request(Code.GET);
                caCertsRequest.setDestination(estServerAddress.getAddress());
                caCertsRequest.setDestinationPort(estServerAddress.getPort());
                caCertsRequest.getOptions().addUriPath("est");
                caCertsRequest.getOptions().addUriPath("crts");
                coapEndpoints.getSecureEndpoint().sendRequest(caCertsRequest);

                Response caCertsResponse = caCertsRequest.waitForResponse(5000L);

                if (caCertsResponse.getCode() == ResponseCode.CONTENT) {
                    CertPath caCerts = new CertificateFactory()
                            .engineGenerateCertPath(new ByteArrayInputStream(caCertsResponse.getPayload()));
                    LOG.debug("CA certs: " + caCerts.getCertificates());

                    // replace secure CoAP end-point with new DTLS configuration
                    Builder builder = new DtlsConnectorConfig.Builder();
                    builder.setAddress(coapEndpoints.getSecureEndpoint().getAddress());
                    builder.setMaxConnections(coapEndpoints.getNetworkConfig().getInt(Keys.MAX_ACTIVE_PEERS));
                    builder.setStaleConnectionThreshold(
                            coapEndpoints.getNetworkConfig().getLong(Keys.MAX_PEER_INACTIVITY_PERIOD));

                    List<Certificate> certs = new ArrayList<>();
                    certs.add(publicKey);
                    certs.addAll(caCerts.getCertificates()); // CA certificates chain

                    builder.setIdentity(pair.getPrivate(), certs.toArray(new Certificate[] {}), false);

                    // DM certificate (direct trust)
                    Certificate[] dmCerts = new Certificate[] { new CertificateFactory()
                            .engineGenerateCertificate(new ByteArrayInputStream(dmInfo.serverPublicKey)) };
                    builder.setTrustStore(dmCerts);

                    coapEndpoints.recreateSecureEndpoint(builder.build());
                }

            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
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

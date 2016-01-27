package org.eclipse.leshan.server.californium.sandc_impl;

import org.eclipse.leshan.server.client.ClientRegistry;
import org.eclipse.leshan.server.impl.ClientRegistryImpl;
import org.eclipse.leshan.server.impl.ObservationRegistryImpl;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StandardModelProvider;
import org.eclipse.leshan.server.observation.ObservationRegistry;
import org.eclipse.leshan.server.security.SecurityRegistry;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Created by Jyotsna.Bhonde on 10/23/2015.
 */
public class SandCLeshanServerBuilder {

    /** IANA assigned UDP port for CoAP (so for LWM2M) */
    public static final int PORT = 5683;

    /** IANA assigned UDP port for CoAP with DTLS (so for LWM2M) */
    public static final int PORT_DTLS = 5684;

    private SecurityRegistry securityRegistry;
    private ObservationRegistry observationRegistry;
    private ClientRegistry clientRegistry;
    private LwM2mModelProvider modelProvider;
    private InetSocketAddress localAddress;
    private InetSocketAddress localAddressSecure;

    public SandCLeshanServerBuilder setLocalAddress(String hostname, int port) {
        this.localAddress = new InetSocketAddress(hostname, port);
        return this;
    }

    public SandCLeshanServerBuilder setLocalAddress(InetSocketAddress localAddress) {
        this.localAddress = localAddress;
        return this;
    }

    public SandCLeshanServerBuilder setLocalAddressSecure(String hostname, int port) {
        this.localAddressSecure = new InetSocketAddress(hostname, port);
        return this;
    }

    public SandCLeshanServerBuilder setLocalAddressSecure(InetSocketAddress localAddressSecure) {
        this.localAddressSecure = localAddressSecure;
        return this;
    }

    public SandCLeshanServerBuilder setClientRegistry(ClientRegistry clientRegistry) {
        this.clientRegistry = clientRegistry;
        return this;
    }

    public SandCLeshanServerBuilder setObservationRegistry(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
        return this;
    }

    public SandCLeshanServerBuilder setSecurityRegistry(SecurityRegistry securityRegistry) {
        this.securityRegistry = securityRegistry;
        return this;
    }

    public SandCLeshanServerBuilder setObjectModelProvider(LwM2mModelProvider objectModelProvider) {
        this.modelProvider = objectModelProvider;
        return this;
    }

    public SandCLeshanServer build() {
        if (localAddress == null)
            localAddress = new InetSocketAddress((InetAddress) null, PORT);
        if (localAddressSecure == null)
            localAddressSecure = new InetSocketAddress((InetAddress) null, PORT_DTLS);
        if (clientRegistry == null)
            clientRegistry = new ClientRegistryImpl();
        if (securityRegistry == null)
            securityRegistry = new SecurityRegistryImpl();
        if (observationRegistry == null)
            observationRegistry = new ObservationRegistryImpl();
        if (modelProvider == null) {
            modelProvider = new StandardModelProvider();
        }
        return new SandCLeshanServer(localAddress, localAddressSecure, clientRegistry, securityRegistry,
                observationRegistry, modelProvider);
    }

}

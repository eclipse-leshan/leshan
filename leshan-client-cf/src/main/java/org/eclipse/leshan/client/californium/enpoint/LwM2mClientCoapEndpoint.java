package org.eclipse.leshan.client.californium.enpoint;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.californium.core.coap.MessageObserver;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.leshan.client.californium.CaliforniumConnectionController;
import org.eclipse.leshan.client.californium.CaliforniumIdentityExtractor;
import org.eclipse.leshan.client.endpoint.ClientEndpointToolbox;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpoint;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.californium.AsyncRequestObserver;
import org.eclipse.leshan.core.californium.SyncRequestObserver;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.core.request.UplinkRequest;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.util.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mClientCoapEndpoint implements LwM2mClientEndpoint {

    private final Logger LOG = LoggerFactory.getLogger(LwM2mClientCoapEndpoint.class);

    private final Protocol protocol;
    private final ScheduledExecutorService executor;
    private final Endpoint endpoint;
    private final ClientEndpointToolbox toolbox;
    private final CoapMessageTranslator translator;
    private final CaliforniumIdentityExtractor identityExtractor;
    private final CaliforniumConnectionController connectionController;
    private final LwM2mModel model;

    public LwM2mClientCoapEndpoint(Protocol protocol, Endpoint endpoint, CoapMessageTranslator translator,
            ClientEndpointToolbox toolbox, CaliforniumIdentityExtractor identityExtractor,
            CaliforniumConnectionController connectionController, LwM2mModel model, ScheduledExecutorService executor) {
        this.protocol = protocol;
        this.translator = translator;
        this.toolbox = toolbox;
        this.endpoint = endpoint;
        this.identityExtractor = identityExtractor;
        this.connectionController = connectionController;
        this.model = model;
        this.executor = executor;
    }

    @Override
    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    public URI getURI() {
        try {
            return new URI(protocol.getUriScheme(), null, getInetSocketAddress().getHostString(),
                    getInetSocketAddress().getPort(), null, null, null);
        } catch (URISyntaxException e) {
            // TODO TL : handle this properly
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public InetSocketAddress getInetSocketAddress() {
        return endpoint.getAddress();
    }

    @Override
    public long getMaxCommunicationPeriodFor(long lifetimeInMs) {
        // See https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/283 to better understand.
        // TODO For DTLS, worst Handshake scenario should be taking into account too.

        int floor = 30000; // value from which we stop to adjust communication period using COAP EXCHANGE LIFETIME.

        // To be sure registration doesn't expired, update request should be send considering all CoAP retransmissions
        // and registration lifetime.
        // See https://tools.ietf.org/html/rfc7252#section-4.8.2
        long exchange_lifetime = endpoint.getConfig().get(CoapConfig.EXCHANGE_LIFETIME, TimeUnit.MILLISECONDS);
        if (lifetimeInMs - exchange_lifetime >= floor) {
            return lifetimeInMs - exchange_lifetime;
        } else {
            LOG.warn("Too small lifetime : we advice to not use a lifetime < (COAP EXCHANGE LIFETIME + 30s)");
            // lifetime value is too short, so we do a compromise and we don't remove COAP EXCHANGE LIFETIME completely
            // We distribute the remaining lifetime range [0, exchange_lifetime + floor] on the remaining range
            // [1,floor]s.
            return lifetimeInMs * (floor - 1000) / (exchange_lifetime + floor) + 1000;
        }
    }

    @Override
    public <T extends LwM2mResponse> T send(ServerIdentity server, UplinkRequest<T> lwm2mRequest, long timeoutInMs)
            throws InterruptedException {
        // Create the CoAP request from LwM2m request
        final Request coapRequest = translator.createCoapRequest(server, lwm2mRequest, toolbox, model);

        // Send CoAP request synchronously
        SyncRequestObserver<T> syncMessageObserver = new SyncRequestObserver<T>(coapRequest, timeoutInMs) {
            @Override
            public T buildResponse(Response coapResponse) {
                // Build LwM2m response
                T lwM2mResponse = translator.createLwM2mResponse(server, lwm2mRequest, coapRequest, coapResponse,
                        toolbox);
                return lwM2mResponse;
            }
        };
        coapRequest.addMessageObserver(syncMessageObserver);

        // Send CoAP request asynchronously
        endpoint.sendRequest(coapRequest);

        // Wait for response, then return it
        return syncMessageObserver.waitForResponse();
    }

    @Override
    public <T extends LwM2mResponse> void send(ServerIdentity server, UplinkRequest<T> lwm2mRequest,
            ResponseCallback<T> responseCallback, ErrorCallback errorCallback, long timeoutInMs) {
        Validate.notNull(responseCallback);
        Validate.notNull(errorCallback);

        // Create the CoAP request from LwM2m request
        final Request coapRequest = translator.createCoapRequest(server, lwm2mRequest, toolbox, model);

        // Add CoAP request callback
        MessageObserver obs = new AsyncRequestObserver<T>(coapRequest, responseCallback, errorCallback, timeoutInMs,
                executor) {
            @Override
            public T buildResponse(Response coapResponse) {
                // Build LwM2m response
                T lwM2mResponse = translator.createLwM2mResponse(server, lwm2mRequest, coapRequest, coapResponse,
                        toolbox);
                return lwM2mResponse;
            }
        };
        coapRequest.addMessageObserver(obs);

        // Send CoAP request asynchronously
        endpoint.sendRequest(coapRequest);

    }

    @Override
    public void forceReconnection(ServerIdentity server, boolean resume) {
        connectionController.forceReconnection(endpoint, server, resume);
    }

    public Identity getExtractIdentity(CoapExchange exchange) {
        return identityExtractor.getIdentity(exchange);
    }

    public Endpoint getCoapEndpoint() {
        return endpoint;
    }
}

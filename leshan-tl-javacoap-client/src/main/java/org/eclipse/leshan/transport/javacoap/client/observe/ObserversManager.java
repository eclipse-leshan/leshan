/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.transport.javacoap.client.observe;

import static com.mbed.coap.utils.Validations.require;
import static java.util.Objects.requireNonNull;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mbed.coap.packet.CoapRequest;
import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Code;
import com.mbed.coap.packet.Method;
import com.mbed.coap.packet.SeparateResponse;
import com.mbed.coap.server.CoapServer;
import com.mbed.coap.utils.Filter;
import com.mbed.coap.utils.Service;

public class ObserversManager implements Filter.SimpleFilter<CoapRequest, CoapResponse> {
    // TODO this is largely inspired of com.mbed.coap.server.observe.ObserversManager
    // Some discussion to avoid this here : https://github.com/open-coap/java-coap/issues/56#issuecomment-1677137078
    // Don't know if we will find a way to avoid this.

    private final static Logger LOGGER = LoggerFactory.getLogger(ObserversManager.class);

    private volatile Service<SeparateResponse, Boolean> notificationSender;
    private final AtomicInteger observeSeq = new AtomicInteger(0);
    private final ObserversStore observersStore;

    public ObserversManager(ObserversStore observersStore) {
        this.observersStore = observersStore;
    }

    public void init(Service<SeparateResponse, Boolean> outboundObservation) {
        this.notificationSender = requireNonNull(outboundObservation);
    }

    public void init(CoapServer server) {
        require(!server.isRunning(), "SubscriptionManager should be initialized with non yet running server");
        init(server.outboundResponseService());
    }

    @Override
    public CompletableFuture<CoapResponse> apply(CoapRequest request, Service<CoapRequest, CoapResponse> service) {
        return service.apply(request).thenApply(resp -> subscribe(request, resp));
    }

    private CoapResponse subscribe(CoapRequest req, CoapResponse resp) {
        if (req.getMethod() != Method.GET && req.getMethod() != Method.FETCH) {
            return resp;
        }

        if (resp.getCode() == Code.C205_CONTENT && Objects.equals(0, req.options().getObserve())) {
            observersStore.add(req);
            if (resp.options().getObserve() == null) {
                return resp.options(b -> b.observe(observeSeq.get()));
            }
            return resp;
        } else if (Objects.equals(1, req.options().getObserve())) {
            observersStore.remove(req);
        }
        return resp.options(it -> it.observe(null));
    }

    public void sendObservation(Predicate<CoapRequest> observersFilter,
            Service<CoapRequest, CoapResponse> responseBuilder) {
        StreamSupport.stream(observersStore.spliterator(), false).filter(observersFilter)
                .forEach(coapRequest -> sendObservation(coapRequest, responseBuilder));
    }

    private void sendObservation(CoapRequest observeRequest, Service<CoapRequest, CoapResponse> responseBuilder) {
        int currentObserveSequence = observeSeq.incrementAndGet();
        responseBuilder.apply(observeRequest)
                .thenApply(obsResponse -> toSeparateResponse(obsResponse, currentObserveSequence, observeRequest))
                .thenAccept(separateResponse -> sendObservation(observeRequest, separateResponse));
    }

    private static SeparateResponse toSeparateResponse(CoapResponse obsResponse, int currentObserveSequence,
            CoapRequest subscribeRequest) {
        obsResponse.options().setObserve(currentObserveSequence);
        return obsResponse.toSeparate(subscribeRequest.getToken(), subscribeRequest.getPeerAddress());
    }

    private void sendObservation(CoapRequest observeRequest, SeparateResponse separateResponse) {
        InetSocketAddress peerAddress = separateResponse.getPeerAddress();

        notificationSender.apply(separateResponse).whenComplete((result, exception) -> {
            if (exception != null) {
                observersStore.remove(observeRequest);
                LOGGER.warn("[{}#{}] Removed observation relation, got exception: {}", peerAddress,
                        separateResponse.getToken(), exception.toString());
            } else if (!result) {
                observersStore.remove(observeRequest);
                LOGGER.info("[{}#{}] Removed observation relation, got reset", peerAddress,
                        separateResponse.getToken());
            }
        });

        if (separateResponse.getCode() != Code.C205_CONTENT) {
            observersStore.remove(observeRequest);
        }
    }

}

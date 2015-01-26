/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.CoAP.ResponseCode;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveManager;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.observe.ObservingEndpoint;
import org.eclipse.californium.core.server.MessageDeliverer;
import org.eclipse.californium.core.server.resources.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LwM2mServerMessageDeliverer implements MessageDeliverer {

    private final static Logger LOG = LoggerFactory.getLogger(LwM2mServerMessageDeliverer.class);

    /* The root of all resources */
    private final Resource root;

    /* The manager of the observe mechanism for this server */
    private final ObserveManager observeManager = new ObserveManager();

    /**
     * Constructs a default message deliverer that delivers requests to the resources rooted at the specified root.
     */
    public LwM2mServerMessageDeliverer(final Resource root) {
        this.root = root;
    }

    @Override
    public void deliverRequest(final Exchange exchange) {
        final Request request = exchange.getRequest();
        final List<String> path = request.getOptions().getUriPath();
        final Code code = request.getCode();
        final Resource resource = findResource(path, code);
        if (resource != null) {
            checkForObserveOption(exchange, resource);

            // Get the executor and let it process the request
            final Executor executor = resource.getExecutor();
            if (executor != null) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        resource.handleRequest(exchange);
                    }
                });
            } else {
                resource.handleRequest(exchange);
            }
        } else {
            LOG.info("Did not find resource " + path.toString());
            exchange.sendResponse(new Response(ResponseCode.NOT_FOUND));
        }
    }

    /**
     * Checks whether an observe relationship has to be established or canceled. This is done here to have a
     * server-global observeManager that holds the set of remote endpoints for all resources. This global knowledge is
     * required for efficient orphan handling.
     * 
     * @param exchange the exchange of the current request
     * @param resource the target resource
     * @param path the path to the resource
     */
    private void checkForObserveOption(final Exchange exchange, final Resource resource) {
        final Request request = exchange.getRequest();
        if (request.getCode() != Code.GET) {
            return;
        }

        final InetSocketAddress source = new InetSocketAddress(request.getSource(), request.getSourcePort());

        if (request.getOptions().hasObserve() && resource.isObservable()) {

            if (request.getOptions().getObserve() == 0) {
                // Requests wants to observe and resource allows it :-)
                LOG.info("Initiate an observe relation between " + request.getSource() + ":" + request.getSourcePort()
                        + " and resource " + resource.getURI());
                final ObservingEndpoint remote = observeManager.findObservingEndpoint(source);
                final ObserveRelation relation = new ObserveRelation(remote, resource, exchange);
                remote.addObserveRelation(relation);
                exchange.setRelation(relation);
                // all that's left is to add the relation to the resource which
                // the resource must do itself if the response is successful
            } else if (request.getOptions().getObserve() == 1) {
                final ObserveRelation relation = observeManager.getRelation(source, request.getToken());
                if (relation != null) {
                    relation.cancel();
                }
            }
        }
    }

    /**
     * Searches in the resource tree for the specified path. A parent resource may accept requests to subresources,
     * e.g., to allow addresses with wildcards like <code>coap://example.com:5683/devices/*</code>
     * 
     * @param list the path as list of resource names
     * @return the resource or null if not found
     */
    public Resource findResource(final List<String> list, final Code code) {
        final Resource result = searchResourceTree(list);
        if (result == null && shouldDeliverAbsenteeToParent(list, code)) {
            return searchResourceTree(list.subList(0, list.size() - 1));
        }
        return result;
    }

    private Resource searchResourceTree(final List<String> list) {
        final LinkedList<String> path = new LinkedList<String>(list);
        Resource current = root;
        while (!path.isEmpty() && current != null) {
            final String name = path.removeFirst();
            current = current.getChild(name);
        }
        return current;
    }

    private boolean shouldDeliverAbsenteeToParent(final List<String> list, final Code code) {
        return code == Code.POST && list.size() == 2;
    }

    @Override
    public void deliverResponse(final Exchange exchange, final Response response) {
        if (response == null) {
            throw new NullPointerException();
        }
        if (exchange == null) {
            throw new NullPointerException();
        }
        if (exchange.getRequest() == null) {
            throw new NullPointerException();
        }
        exchange.getRequest().setResponse(response);
    }
}

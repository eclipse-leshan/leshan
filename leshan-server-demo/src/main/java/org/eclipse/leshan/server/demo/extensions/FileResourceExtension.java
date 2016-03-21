/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
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
 *    Achim Kraus (Bosch Software Innovations GmbH) - Initial contribution
 ******************************************************************************/
package org.eclipse.leshan.server.demo.extensions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple file resource.
 * 
 * Enables the LWM2M server to provide files to clients via COAP.
 * 
 * Usage: "coap://host/file/filename" (or "coaps://")
 * 
 * where "file" is the COAP name of this resource, and filename is the filename of the file in the file-root directory.
 */
public class FileResourceExtension extends CoapResource implements LeshanServerExtension {

    /**
     * Configuration key for files root directory.
     * 
     * @see #dataRoot
     */
    public static final String CONFIG_ROOT = "ROOT";
    /**
     * Configuration key for maximum file length.
     * 
     * @see #maxFileLength
     */
    public static final String CONFIG_MAX_FILE_LENGTH = "MAX_FILE_LENGTH";

    private static final Logger LOG = LoggerFactory.getLogger(FileResourceExtension.class);
    /**
     * Default value for files root directory.
     * 
     * @see #dataRoot
     */
    private static final String DEFAULT_ROOT = "files";
    /**
     * Default value for maximum file length.
     * 
     * @see #maxFileLength
     */
    private static final long DEFAULT_MAX_FILE_LENGTH = 1000000;

    private volatile boolean enabled;
    /**
     * Maximum file length.
     */
    private volatile long maxFileLength = DEFAULT_MAX_FILE_LENGTH;
    /**
     * Files root directory.
     */
    private volatile File dataRoot = new File(DEFAULT_ROOT);

    public FileResourceExtension() {
        super("file");
    }

    /*
     * Override the default behavior so that requests to sub resources (typically /file/{file-name}) are handled by
     * /file resource.
     */
    @Override
    public Resource getChild(String name) {
        return this;
    }

    @Override
    public void handleRequest(Exchange exchange) {
        try {
            super.handleRequest(exchange);
        } catch (Exception e) {
            LOG.error("Exception while handling a request on the /bs resource", e);
            exchange.sendResponse(new Response(CoAP.ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public void handleGET(final CoapExchange exchange) {
        Request request = exchange.advanced().getRequest();
        LOG.debug("Get received : {}", request);

        if (!enabled || null == dataRoot)
            exchange.respond(CoAP.ResponseCode.NOT_FOUND);

        String myURI = getURI() + "/";
        String path = "/" + request.getOptions().getUriPathString();
        if (!path.startsWith(myURI)) {
            LOG.warn("Request {} doesn't match {}!", path, myURI);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
            return;
        }
        path = path.substring(myURI.length());
        LOG.info("Send file {}", path);
        File file = new File(dataRoot, path);
        if (!file.exists() || !file.isFile()) {
            LOG.warn("File {} doesn't exist!", file.getAbsolutePath());
            exchange.respond(CoAP.ResponseCode.NOT_FOUND);
            return;
        }
        if (!file.canRead()) {
            LOG.warn("File {} is not readable!", file.getAbsolutePath());
            exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
            return;
        }
        long length = file.length();
        if (length > maxFileLength) {
            LOG.warn("File {} is too large {} (max.: {})!", file.getAbsolutePath(), length, maxFileLength);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
            return;
        }
        try {
            byte[] content = new byte[(int) length];
            try (InputStream in = new FileInputStream(file)) {
                int r = in.read(content);
                if (length == r) {
                    Response response = new Response(CoAP.ResponseCode.CONTENT);
                    response.setPayload(content);
                    response.getOptions().setSize2((int) length);
                    exchange.respond(response);
                } else {
                    LOG.warn("File {} could not be read in!", file.getAbsolutePath());
                    exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
                }
            } catch (IOException ex) {
                LOG.warn("File {} : {}", file.getAbsolutePath(), ex);
                exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
            }
        } catch (OutOfMemoryError error) {
            LOG.warn("Out of Memory Error {}!", length);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void setup(LeshanServer lwServer, ExtensionConfig configuration, LeshanServerExtensionsManager manager) {
        String root = configuration.get(CONFIG_ROOT, DEFAULT_ROOT);
        maxFileLength = configuration.get(CONFIG_MAX_FILE_LENGTH, DEFAULT_MAX_FILE_LENGTH);
        dataRoot = new File(root);
        lwServer.getCoapServer().add(this);
        LOG.info("file resources rootdir: " + dataRoot.getAbsolutePath() + ", max. bytes: " + maxFileLength);
    }

    @Override
    public void start() {
        enabled = true;
        LOG.info("Extension file resources enabled");
    }

    @Override
    public void stop() {
        enabled = false;
        LOG.info("Extension file resources disabled");
    }
}

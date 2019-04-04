/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Rikard Höglund (RISE SICS)
 *
 *******************************************************************************/
package org.eclipse.leshan.client.object;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link LwM2mInstanceEnabler} for the OSCORE Security (21) object.
 */
public class Oscore extends BaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(Security.class);

    private byte[] masterSecret;
    private String senderId;
    private String recipientId;
    private int aeadAlgorithm;
    private int hmacAlgorithm;

    public Oscore() {

    }

    public Oscore(byte[] masterSecret, String senderId, String recipientId, int aeadAlgorithm, int hmacAlgorithm) {
        super();
        this.masterSecret = masterSecret.clone();
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.aeadAlgorithm = aeadAlgorithm;
        this.hmacAlgorithm = hmacAlgorithm;
    }

    @Override
    public WriteResponse write(ServerIdentity identity, int resourceId, LwM2mResource value) {
        LOG.debug("Write on resource {}: {}", resourceId, value);
        // extend
        return WriteResponse.notFound();
    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        LOG.debug("Read on resource {}", resourceid);
        // extend
        return ReadResponse.notFound();
    }

    public byte[] getMasterSecret() {
        return masterSecret;
    }

    public void setMasterSecret(byte[] masterSecret) {
        this.masterSecret = masterSecret;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public int getAeadAlgorithm() {
        return aeadAlgorithm;
    }

    public void setAeadAlgorithm(int aeadAlgorithm) {
        this.aeadAlgorithm = aeadAlgorithm;
    }

    public int getHmacAlgorithm() {
        return hmacAlgorithm;
    }

    public void setHmacAlgorithm(int hmacAlgorithm) {
        this.hmacAlgorithm = hmacAlgorithm;
    }

}

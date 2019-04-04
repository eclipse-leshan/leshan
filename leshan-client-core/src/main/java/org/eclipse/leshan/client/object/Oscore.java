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
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *
 *******************************************************************************/
package org.eclipse.leshan.client.object;

import static org.eclipse.leshan.core.LwM2mId.*;

import java.util.Arrays;
import java.util.List;

import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel.Type;
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

    private final static List<Integer> supportedResources = Arrays.asList(OSCORE_Master_Secret, OSCORE_Sender_ID,
            OSCORE_Recipient_ID, OSCORE_AEAD_Algorithm, OSCORE_HMAC_Algorithm, OSCORE_Master_Salt);

    private String masterSecret;
    private String senderId;
    private String recipientId;
    private int aeadAlgorithm;
    private int hkdfAlgorithm;
    private String masterSalt;

    public Oscore() {

    }

    /**
     * Default constructor.
     */
    public Oscore(int instanceId, String masterSecret, String senderId, String recipientId, int aeadAlgorithm,
            int hkdfAlgorithm, String masterSalt) {
        super(instanceId);
        this.masterSecret = masterSecret;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.aeadAlgorithm = aeadAlgorithm;
        this.hkdfAlgorithm = hkdfAlgorithm;
        this.masterSalt = masterSalt;
    }

    /**
     * Constructor providing some default values.
     *
     * aeadAlgorithm = 10; //AES_CCM_16_64_128m hmacAlgorithm = -10; //HKDF_HMAC_SHA_256, masterSalt = "";
     *
     */
    public Oscore(int instanceId, String masterSecret, String senderId, String recipientId) {
        this(instanceId, masterSecret, senderId, recipientId, 10, -10, "");
    }

    @Override
    public WriteResponse write(ServerIdentity identity, boolean replace, int resourceId, LwM2mResource value) {
        LOG.debug("Write on resource {}: {}", resourceId, value);

        // restricted to BS server?

        switch (resourceId) {

        case OSCORE_Master_Secret:
            if (value.getType() != Type.STRING) {
                return WriteResponse.badRequest("invalid type");
            }
            masterSecret = (String) value.getValue();
            return WriteResponse.success();

        case OSCORE_Sender_ID:
            if (value.getType() != Type.STRING) {
                return WriteResponse.badRequest("invalid type");
            }
            senderId = (String) value.getValue();
            return WriteResponse.success();

        case OSCORE_Recipient_ID:
            if (value.getType() != Type.STRING) {
                return WriteResponse.badRequest("invalid type");
            }
            recipientId = (String) value.getValue();
            return WriteResponse.success();

        case OSCORE_AEAD_Algorithm:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            aeadAlgorithm = ((Long) value.getValue()).intValue();
            return WriteResponse.success();

        case OSCORE_HMAC_Algorithm:
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            hkdfAlgorithm = ((Long) value.getValue()).intValue();
            return WriteResponse.success();

        case OSCORE_Master_Salt:
            if (value.getType() != Type.STRING) {
                return WriteResponse.badRequest("invalid type");
            }
            masterSalt = (String) value.getValue();
            return WriteResponse.success();

        default:
            return super.write(identity, replace, resourceId, value);
        }

    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        LOG.debug("Read on resource {}", resourceid);
        // only accessible for internal read?

        switch (resourceid) {

        case OSCORE_Master_Secret:
            return ReadResponse.success(resourceid, masterSecret);

        case OSCORE_Sender_ID:
            return ReadResponse.success(resourceid, senderId);

        case OSCORE_Recipient_ID:
            return ReadResponse.success(resourceid, recipientId);

        case OSCORE_AEAD_Algorithm:
            return ReadResponse.success(resourceid, aeadAlgorithm);

        case OSCORE_HMAC_Algorithm:
            return ReadResponse.success(resourceid, hkdfAlgorithm);

        case OSCORE_Master_Salt:
            return ReadResponse.success(resourceid, masterSalt);

        default:
            return super.read(identity, resourceid);
        }
    }

    @Override
    public List<Integer> getAvailableResourceIds(ObjectModel model) {
        return supportedResources;
    }

}

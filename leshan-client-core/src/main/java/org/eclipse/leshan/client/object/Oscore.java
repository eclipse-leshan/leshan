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
import org.eclipse.leshan.core.oscore.AeadAlgorithm;
import org.eclipse.leshan.core.oscore.HkdfAlgorithm;
import org.eclipse.leshan.core.oscore.InvalidOscoreSettingException;
import org.eclipse.leshan.core.oscore.OscoreSetting;
import org.eclipse.leshan.core.oscore.OscoreValidator;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link LwM2mInstanceEnabler} for the OSCORE Security (21) object.
 * <p>
 * Note it must be used with a minimum 2.0 version of the object because previously String was used instead of byte[]
 * for recipientId, SenderId, MasterKey, MasterSalt.
 * <p>
 * See for more details : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/521
 */
public class Oscore extends BaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(Security.class);

    private final static List<Integer> supportedResources = Arrays.asList(OSCORE_MASTER_SECRET, OSCORE_SENDER_ID,
            OSCORE_RECIPIENT_ID, OSCORE_AEAD_ALGORITHM, OSCORE_HMAC_ALGORITHM, OSCORE_MASTER_SALT);

    private byte[] masterSecret;
    private byte[] senderId;
    private byte[] recipientId;
    private AeadAlgorithm aeadAlgorithm;
    private HkdfAlgorithm hkdfAlgorithm;
    private byte[] masterSalt;

    public Oscore() {

    }

    /**
     * Default constructor.
     */
    public Oscore(int instanceId, OscoreSetting oscoreSetting) {
        super(instanceId);
        try {
            new OscoreValidator().validateOscoreSetting(oscoreSetting);
        } catch (InvalidOscoreSettingException e) {
            throw new IllegalArgumentException("Invalid " + oscoreSetting, e);
        }
        this.masterSecret = oscoreSetting.getMasterSecret();
        this.senderId = oscoreSetting.getSenderId();
        this.recipientId = oscoreSetting.getRecipientId();
        this.aeadAlgorithm = oscoreSetting.getAeadAlgorithm();
        this.hkdfAlgorithm = oscoreSetting.getHkdfAlgorithm();
        this.masterSalt = oscoreSetting.getMasterSalt();
    }

    @Override
    public WriteResponse write(ServerIdentity identity, boolean replace, int resourceId, LwM2mResource value) {
        if (!identity.isSystem())
            LOG.debug("Write on resource {}: {}", resourceId, value);

        switch (resourceId) {
        case OSCORE_MASTER_SECRET:
            if (value.getType() != Type.OPAQUE) {
                return WriteResponse.badRequest("invalid type");
            }
            masterSecret = (byte[]) value.getValue();
            return WriteResponse.success();

        case OSCORE_SENDER_ID:
            if (value.getType() != Type.OPAQUE) {
                return WriteResponse.badRequest("invalid type");
            }
            senderId = (byte[]) value.getValue();
            return WriteResponse.success();

        case OSCORE_RECIPIENT_ID:
            if (value.getType() != Type.OPAQUE) {
                return WriteResponse.badRequest("invalid type");
            }
            recipientId = (byte[]) value.getValue();
            return WriteResponse.success();

        case OSCORE_AEAD_ALGORITHM: {
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            Long longValue = (Long) value.getValue();
            aeadAlgorithm = AeadAlgorithm.fromValue(longValue);
            if (aeadAlgorithm != null) {
                return WriteResponse.success();
            } else {
                return WriteResponse.badRequest("unknown algorithm " + longValue);
            }
        }
        case OSCORE_HMAC_ALGORITHM: {
            if (value.getType() != Type.INTEGER) {
                return WriteResponse.badRequest("invalid type");
            }
            Long longValue = (Long) value.getValue();
            hkdfAlgorithm = HkdfAlgorithm.fromValue(longValue);
            if (hkdfAlgorithm != null) {
                return WriteResponse.success();
            } else {
                return WriteResponse.badRequest("unknown algorithm " + longValue);
            }
        }
        case OSCORE_MASTER_SALT:
            if (value.getType() != Type.OPAQUE) {
                return WriteResponse.badRequest("invalid type");
            }
            masterSalt = (byte[]) value.getValue();
            return WriteResponse.success();

        default:
            return super.write(identity, replace, resourceId, value);
        }

    }

    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
        if (!identity.isSystem())
            LOG.debug("Read on resource {}", resourceid);

        switch (resourceid) {

        case OSCORE_MASTER_SECRET:
            return ReadResponse.success(resourceid, masterSecret);

        case OSCORE_SENDER_ID:
            return ReadResponse.success(resourceid, senderId);

        case OSCORE_RECIPIENT_ID:
            return ReadResponse.success(resourceid, recipientId);

        case OSCORE_AEAD_ALGORITHM:
            if (aeadAlgorithm == null) {
                return ReadResponse.notFound();
            }
            return ReadResponse.success(resourceid, aeadAlgorithm.getValue());

        case OSCORE_HMAC_ALGORITHM:
            if (hkdfAlgorithm == null) {
                return ReadResponse.notFound();
            }
            return ReadResponse.success(resourceid, hkdfAlgorithm.getValue());

        case OSCORE_MASTER_SALT:
            if (masterSalt == null) {
                return ReadResponse.notFound();
            }
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

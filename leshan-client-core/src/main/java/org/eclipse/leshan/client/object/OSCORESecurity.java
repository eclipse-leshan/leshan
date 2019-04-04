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

import org.eclipse.leshan.client.request.ServerIdentity;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple {@link LwM2mInstanceEnabler} for the OSCORE Security (0) object.
 */
public class OSCORESecurity extends BaseInstanceEnabler {

    private static final Logger LOG = LoggerFactory.getLogger(Security.class);

    byte[] OSCORE_master_secret;
    String OSCORE_sender_id;
    String OSCORE_recipient_id;
    int OSCORE_AEAD_algorithm;
    int OSCORE_HMAC_algorithm;

    public OSCORESecurity() {

    }

	public OSCORESecurity(byte[] OSCORE_master_secret, String OSCORE_sender_id, String OSCORE_recipient_id,
			int OSCORE_AEAD_algorithm, int OSCORE_HMAC_algorithm) {
		super();
		this.OSCORE_master_secret = OSCORE_master_secret.clone();
		this.OSCORE_sender_id = OSCORE_sender_id;
		this.OSCORE_recipient_id = OSCORE_recipient_id;
		this.OSCORE_AEAD_algorithm = OSCORE_AEAD_algorithm;
		this.OSCORE_HMAC_algorithm = OSCORE_HMAC_algorithm;
	}

    @Override
    public WriteResponse write(ServerIdentity identity, int resourceId, LwM2mResource value) {
    	LOG.debug("Write on resource {}: {}", resourceId, value);
    	// extend
		return null;
    }
    
    @Override
    public ReadResponse read(ServerIdentity identity, int resourceid) {
    	// extend
		return null;
    }

	public byte[] getOSCOREMasterSecret() {
		return OSCORE_master_secret;
	}

	public void setOSCOREMasterSecret(byte[] OSCORE_master_secret) {
		this.OSCORE_master_secret = OSCORE_master_secret;
	}

	public String getOSCORESenderId() {
		return OSCORE_sender_id;
	}

	public void setOSCORESenderId(String OSCORE_sender_id) {
		this.OSCORE_sender_id = OSCORE_sender_id;
	}

	public String getOSCORERecipientId() {
		return OSCORE_recipient_id;
	}

	public void setOSCORERecipientId(String OSCORE_recipient_id) {
		this.OSCORE_recipient_id = OSCORE_recipient_id;
	}

	public int getOSCOREAeadAlgorithm() {
		return OSCORE_AEAD_algorithm;
	}

	public void setOSCOREAeadAlgorithm(int OSCORE_AEAD_algorithm) {
		this.OSCORE_AEAD_algorithm = OSCORE_AEAD_algorithm;
	}

	public int getOSCOREHmacAlgorithm() {
		return OSCORE_HMAC_algorithm;
	}

	public void setOSCOREHmacAlgorithm(int OSCORE_HMAC_algorithm) {
		this.OSCORE_HMAC_algorithm = OSCORE_HMAC_algorithm;
	}

}

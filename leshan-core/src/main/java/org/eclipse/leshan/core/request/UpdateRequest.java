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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.LinkObject;
import org.eclipse.leshan.core.response.UpdateResponse;
import org.eclipse.leshan.util.Validate;

/**
 * A Lightweight M2M request for updating the LWM2M Client properties required by the LWM2M Server to contact the LWM2M
 * Client.
 */
public class UpdateRequest implements UplinkRequest<UpdateResponse> {

    private final Long lifeTimeInSec;
    private final String smsNumber;
    private final BindingMode bindingMode;
    private final String registrationId;
    private final LinkObject[] objectLinks;

    /**
     * Sets all fields.
     * 
     * @param registrationId the ID under which the client is registered
     * @param lifetime the number of seconds the client would like its registration to be valid
     * @param smsNumber the SMS number the client can receive messages under
     * @param binding the binding mode(s) the client supports
     * @param objectLinks the objects and object instances the client hosts/supports
     * @throws NullPointerException if the registration ID is <code>null</code>
     */
    public UpdateRequest(String registrationId, Long lifetime, String smsNumber, BindingMode binding,
            LinkObject[] objectLinks) {

        Validate.notNull(registrationId);
        this.registrationId = registrationId;
        this.objectLinks = objectLinks;
        this.lifeTimeInSec = lifetime;
        this.bindingMode = binding;
        this.smsNumber = smsNumber;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public LinkObject[] getObjectLinks() {
        return objectLinks;
    }

    public Long getLifeTimeInSec() {
        return lifeTimeInSec;
    }

    public String getSmsNumber() {
        return smsNumber;
    }

    public BindingMode getBindingMode() {
        return bindingMode;
    }

    @Override
    public void accept(UplinkRequestVisitor visitor) {
        visitor.visit(this);
    }
}

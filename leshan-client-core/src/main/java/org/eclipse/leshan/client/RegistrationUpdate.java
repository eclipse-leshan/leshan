/*******************************************************************************
 * Copyright (c) 2019 Sierra Wireless and others.
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
package org.eclipse.leshan.client;

import java.util.Map;

import org.eclipse.leshan.Link;
import org.eclipse.leshan.core.request.BindingMode;

public class RegistrationUpdate {

    private final Long lifeTimeInSec;
    private final String smsNumber;
    private final BindingMode bindingMode;
    private final Link[] objectLinks;
    private final Map<String, String> additionalAttributes;

    public RegistrationUpdate(Long lifeTimeInSec, String smsNumber, BindingMode bindingMode, Link[] objectLinks,
            Map<String, String> additionalAttributes) {
        this.lifeTimeInSec = lifeTimeInSec;
        this.smsNumber = smsNumber;
        this.bindingMode = bindingMode;
        this.objectLinks = objectLinks;
        this.additionalAttributes = additionalAttributes;
    }

    public RegistrationUpdate() {
        this(null, null, null, null, null);
    }

    public RegistrationUpdate(Long lifeTimeInSec) {
        this(lifeTimeInSec, null, null, null, null);
    }

    public RegistrationUpdate(String smsNumber) {
        this(null, smsNumber, null, null, null);
    }

    public RegistrationUpdate(BindingMode bindingMode) {
        this(null, null, bindingMode, null, null);
    }

    public RegistrationUpdate(Link[] objectLinks) {
        this(null, null, null, objectLinks, null);
    }

    public RegistrationUpdate(Map<String, String> additionalAttributes) {
        this(null, null, null, null, additionalAttributes);
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

    public Link[] getObjectLinks() {
        return objectLinks;
    }

    public Map<String, String> getAdditionalAttributes() {
        return additionalAttributes;
    }
}

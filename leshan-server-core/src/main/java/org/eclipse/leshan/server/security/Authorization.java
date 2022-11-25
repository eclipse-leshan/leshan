/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.server.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.server.registration.Registration;

/**
 * {@link Authorization} provided by {@link Authorizer}.
 * <p>
 * Some Application Data get be attached and will be used to create the {@link Registration}
 */
public class Authorization {

    private static Authorization APPROVED = new Authorization(true, null);
    private static Authorization DECLINED = new Authorization(false, null);

    private final boolean approved;
    private final Map<String, String> applicationData;

    protected Authorization(boolean approved, Map<String, String> applicationData) {
        if (!approved && applicationData != null) {
            throw new IllegalStateException("application data can not be attached to 'declined' Authorization");
        }
        this.approved = approved;
        if (applicationData == null) {
            this.applicationData = Collections.emptyMap();
        } else {
            this.applicationData = Collections.unmodifiableMap(new HashMap<>(applicationData));
        }

    }

    public static Authorization approved() {
        return APPROVED;
    }

    public static Authorization approved(Map<String, String> applicationData) {
        return new Authorization(true, applicationData);
    }

    public static Authorization declined() {
        return DECLINED;
    }

    public boolean isApproved() {
        return approved;
    }

    public boolean isDeclined() {
        return !approved;
    }

    public Map<String, String> getApplicationData() {
        return applicationData;
    }

    public boolean hasApplicationData() {
        return !applicationData.isEmpty();
    }
}

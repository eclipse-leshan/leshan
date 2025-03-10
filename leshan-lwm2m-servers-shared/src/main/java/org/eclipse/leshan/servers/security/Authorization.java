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
package org.eclipse.leshan.servers.security;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link Authorization} provided by authorizer.
 * <p>
 * Some Custom Data can be attached and will be attached to the Registration or BootstrapSession.
 */
public class Authorization {

    private static Authorization APPROVED = new Authorization(true, null);
    private static Authorization DECLINED = new Authorization(false, null);

    private final boolean approved;
    private final Map<String, String> customData;

    protected Authorization(boolean approved, Map<String, String> customData) {
        if (!approved && customData != null) {
            throw new IllegalStateException("custom data can not be attached to 'declined' Authorization");
        }
        this.approved = approved;
        if (customData == null) {
            this.customData = Collections.emptyMap();
        } else {
            this.customData = Collections.unmodifiableMap(new HashMap<>(customData));
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

    public Map<String, String> getCustomData() {
        return customData;
    }

    public boolean hasCustomData() {
        return !customData.isEmpty();
    }
}

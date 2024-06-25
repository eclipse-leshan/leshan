/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.core.model;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.LwM2mId;

/**
 * A registry which contains all version of LWM2M object defined in core LWM2M specification.
 * <p>
 * When there is inconsistency between 2 patch version of the specification. (e.g. LWM2M v1.1 and v1.1.1 does not
 * contains same version of CONNECTIVITY_MONITORING object), then we use the last backward compatible one.
 * <p>
 * See : https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/561
 */
public class LwM2mCoreObjectVersionRegistry {

    protected Map<LwM2mVersion, Map<Integer, Version>> lwm2mVerionToObjectRegistry;

    public LwM2mCoreObjectVersionRegistry() {
        lwm2mVerionToObjectRegistry = new HashMap<>();

        // Add version of core object from LWM2M v1.0
        {
            Map<Integer, Version> objectIdToVersion = new HashMap<>();
            lwm2mVerionToObjectRegistry.put(LwM2mVersion.V1_0, objectIdToVersion);

            objectIdToVersion.put(LwM2mId.SECURITY, new Version(1, 0));
            objectIdToVersion.put(LwM2mId.SERVER, new Version(1, 0));
            objectIdToVersion.put(LwM2mId.ACCESS_CONTROL, new Version(1, 0));
            objectIdToVersion.put(LwM2mId.DEVICE, new Version(1, 0));
            objectIdToVersion.put(LwM2mId.CONNECTIVITY_MONITORING, new Version(1, 0));
            objectIdToVersion.put(LwM2mId.FIRMWARE, new Version(1, 0));
            objectIdToVersion.put(LwM2mId.LOCATION, new Version(1, 0));
            objectIdToVersion.put(LwM2mId.CONNECTIVITY_STATISTICS, new Version(1, 0));
        }

        // Add version of core object from LWM2M v1.1
        {
            Map<Integer, Version> objectIdToVersion = new HashMap<>();
            lwm2mVerionToObjectRegistry.put(LwM2mVersion.V1_1, objectIdToVersion);

            // Since LWM2M v1.0
            objectIdToVersion.put(LwM2mId.SECURITY, new Version(1, 1));
            objectIdToVersion.put(LwM2mId.SERVER, new Version(1, 1));
            objectIdToVersion.put(LwM2mId.ACCESS_CONTROL, new Version(1, 0));
            objectIdToVersion.put(LwM2mId.DEVICE, //
                    // new Version(1, 0)); ---> in lwm2m v1.1
                    new Version(1, 1)); // ---> in lwm2m v1.1.1
            objectIdToVersion.put(LwM2mId.CONNECTIVITY_MONITORING, //
                    // new Version(1, 1)); ---> in lwm2m v1.1
                    new Version(1, 2)); // ---> in lwm2m v1.1.1
            objectIdToVersion.put(LwM2mId.FIRMWARE, new Version(1, 0));
            objectIdToVersion.put(LwM2mId.LOCATION, new Version(1, 0));
            objectIdToVersion.put(LwM2mId.CONNECTIVITY_STATISTICS, new Version(1, 0));

            // Added in v1.1
            objectIdToVersion.put(LwM2mId.OSCORE, new Version(1, 0));
        }
    }

    public Version getDefaultVersion(int objectId, LwM2mVersion lwM2mVersion) {
        Map<Integer, Version> objectIdToVersion = lwm2mVerionToObjectRegistry.get(lwM2mVersion);
        if (objectIdToVersion != null) {
            return objectIdToVersion.get(objectId);
        }
        return null;
    }

    public boolean isCoreObject(int objectId, LwM2mVersion lwM2mVersion) {
        Map<Integer, Version> objectIdToVersion = lwm2mVerionToObjectRegistry.get(lwM2mVersion);
        return objectIdToVersion.get(objectId) != null;
    }

    public boolean isDefaultVersion(Version objectVersion, int objectId, LwM2mVersion lwM2mVersion) {
        Map<Integer, Version> objectIdToVersion = lwm2mVerionToObjectRegistry.get(lwM2mVersion);
        if (objectIdToVersion != null) {
            Version registeredVersion = objectIdToVersion.get(objectId);
            if (registeredVersion == null)
                return false;
            else
                return registeredVersion.equals(objectVersion);
        }
        return false;
    }
}

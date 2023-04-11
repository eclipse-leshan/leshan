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
package org.eclipse.leshan.server.gateway;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Define some Registration Application Data used to handle Gateway.
 */
public class GatewayAppData {

    /**
     * If this is "Iot Device" Registration, this application data is used to store the 'registration id' of the
     * "Gateway".
     */
    public final static String GATEWAY_REGID = "gtw_gateway_regid";

    /**
     * If this is "Iot Device" Registration, this application data is the prefix used to identify the Iot Device' on the
     * "Gateway".
     */
    public final static String IOT_DEVICE_PREFIX = "gtw_iotdevice_prefix";

    /**
     * If this is "Gateway" Registration , this application data is used to store the list of the "Iot device endpoint
     * names" exposed but this Gateway.
     */
    public final static String IOT_DEVICES_ENDPOINT_NAMES = "gtw_iotdevice_endpointnames";

    private final static String IOT_DEVICES_ENDPOINT_NAMES_SEPARATOR = "\n";

    public static List<String> getIotDevicesEndpointNamesFromString(String iotDevicesListAsString) {
        if (iotDevicesListAsString == null)
            return Collections.emptyList();
        return Arrays.asList(iotDevicesListAsString.split(IOT_DEVICES_ENDPOINT_NAMES_SEPARATOR));
    }

    public static String iotDevicesEndpointNamesToString(List<String> iotDevices) {
        return String.join(IOT_DEVICES_ENDPOINT_NAMES_SEPARATOR, iotDevices);
    }
}

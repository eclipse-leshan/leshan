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
package org.eclipse.leshan.transport.javacoap.observation;

import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;

import com.mbed.coap.packet.CoapResponse;
import com.mbed.coap.packet.Opaque;

public class ObservationUtil {

    public static SingleObservation createSingleObservation(String registationID, ObserveRequest lwm2mRequest,
            Opaque token, CoapResponse coapResponse) {
        ContentFormat contentFormat = null;
        if (coapResponse != null) {
            contentFormat = ContentFormat.fromCode(coapResponse.options().getContentFormat());
        }
        return new SingleObservation(new ObservationIdentifier(token.getBytes()), registationID, lwm2mRequest.getPath(),
                contentFormat, null, null);

    }
}

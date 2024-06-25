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
package org.eclipse.leshan.transport.javacoap.server.observation;

import java.util.Optional;

import org.eclipse.leshan.core.observation.CompositeObservation;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.observation.SingleObservation;

public class ObservationUtil {

    // TODO should we support rootpath ?
    public static Optional<String> getPath(Observation observation) {
        if (observation instanceof SingleObservation) {
            return Optional.of(((SingleObservation) observation).getPath().toString());
        } else if (observation instanceof CompositeObservation) {
            return Optional.of("/");
        } else if (observation == null) {
            return Optional.empty();
        } else {
            throw new IllegalStateException(String.format("Unexpected kind of observation : %s is not supported",
                    observation.getClass().getSimpleName()));
        }
    }
}

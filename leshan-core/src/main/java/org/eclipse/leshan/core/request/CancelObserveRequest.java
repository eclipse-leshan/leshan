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

import java.util.Collections;
import java.util.Map;

import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.response.ReadResponse;

/**
 * A Lightweight M2M request for observing changes of a specific Resource, Resources within an Object Instance or for
 * all the Object Instances of an Object within the LWM2M Client.
 */
public class CancelObserveRequest extends AbstractDownlinkRequest<ReadResponse> {

    private ContentFormat format;
    private byte[] id;

    /* Additional information relative to this observe request */
    private Map<String, String> context;

    /**
     * Creates a request for cancel observing.
     * 
     * @param observation to be canceled
     */
    public CancelObserveRequest(ContentFormat format, Observation observation) {
        super(observation.getPath());
        this.format = format;
        this.id = observation.getId();
        this.context = observation.getContext();
    }

    /**
     * @return the desired format of the resource to read
     */
    public ContentFormat getFormat() {
        return format;
    }

    /**
     * @return the desired format of the resource to read
     */
    public byte[] getId() {
        return id;
    }

    /**
     * @return an unmodifiable map containing the additional information relative to this observe request.
     */
    public Map<String, String> getContext() {
        if (context == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(context);
    }

    @Override
    public void accept(DownlinkRequestVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public final String toString() {
        return String.format("ObserveRequest [path=%s format=%s]", getPath(), format);
    }

}

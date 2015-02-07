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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.response;

import java.util.Objects;

import org.eclipse.leshan.ResponseCode;

public class CreateResponse extends BaseLwM2mResponse {

    private final String location;

    private CreateResponse(final ResponseCode code, final String location) {
        super(code, new byte[0]);
        this.location = location;
    }

    private CreateResponse(final ResponseCode code) {
        this(code, null);
    }

    public static CreateResponse success(final int instanceId) {
        return new CreateResponse(ResponseCode.CREATED, Integer.toString(instanceId));
    }

    public static CreateResponse methodNotAllowed() {
        return new CreateResponse(ResponseCode.METHOD_NOT_ALLOWED);
    }

    public static CreateResponse invalidResource() {
        return new CreateResponse(ResponseCode.BAD_REQUEST);
    }

    public String getLocation() {
        return location;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof CreateResponse) || !super.equals(o)) {
            return false;
        }
        final CreateResponse other = (CreateResponse) o;
        return Objects.equals(location, other.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), location);
    }

}

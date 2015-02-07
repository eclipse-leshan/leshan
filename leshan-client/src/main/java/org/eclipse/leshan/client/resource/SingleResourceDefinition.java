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
package org.eclipse.leshan.client.resource;

public class SingleResourceDefinition implements LwM2mClientResourceDefinition {

    private final int id;
    private final LwM2mClientResource resource;
    private final boolean required;

    public SingleResourceDefinition(final int id, final LwM2mClientResource resource, final boolean required) {
        this.id = id;
        this.resource = resource;
        this.required = required;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public LwM2mClientResource createResource() {
        return resource;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

}

/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.server.model;

import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.server.client.Client;

public class StandardModelProvider implements LwM2mModelProvider {

    private final LwM2mModel model;

    public StandardModelProvider() {
        this.model = new LwM2mModel(ObjectLoader.loadDefault());
    }

    @Override
    public LwM2mModel getObjectModel(Client client) {
        // same model for all clients
        return model;
    }
}

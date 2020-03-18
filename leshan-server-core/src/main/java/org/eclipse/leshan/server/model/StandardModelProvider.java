/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
package org.eclipse.leshan.server.model;

import org.eclipse.leshan.core.model.ObjectLoader;

/**
 * A versioned model provider which uses the default model embedded in Leshan.
 * 
 * @see VersionedModelProvider
 */
public class StandardModelProvider extends VersionedModelProvider {

    public StandardModelProvider() {
        super(ObjectLoader.loadDefault());
    }
}

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

import org.eclipse.leshan.core.model.ObjectLoader;

/**
 * A static model provider which uses the default model embedded in Leshan.
 * <p>
 * The MODELS_FOLDER environment variable can be used to add more model definitions.</p>
 * The MODELS_FOLDER should be set with the folder path where your custom models are available. Currently the Leshan
 * JSON format and OMA DDF file format are supported.
 */
public class StandardModelProvider extends StaticModelProvider {

    public StandardModelProvider() {
        super(ObjectLoader.loadDefault());
    }
}

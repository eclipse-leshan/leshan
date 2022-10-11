/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.client.endpoint;

import org.eclipse.leshan.core.link.LinkSerializer;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeParser;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.eclipse.leshan.core.node.codec.LwM2mDecoder;
import org.eclipse.leshan.core.node.codec.LwM2mEncoder;

public class ClientEndpointToolbox {

    private final LwM2mDecoder decoder;
    private final LwM2mEncoder encoder;
    private final LinkSerializer linkSerializer;
    private final LwM2mModel model;
    private final LwM2mAttributeParser attributeParser;

    public ClientEndpointToolbox(LwM2mDecoder decoder, LwM2mEncoder encoder, LinkSerializer linkSerializer,
            LwM2mModel model, LwM2mAttributeParser attributeParser) {
        this.decoder = decoder;
        this.encoder = encoder;
        this.linkSerializer = linkSerializer;
        this.model = model;
        this.attributeParser = attributeParser;
    }

    public LwM2mDecoder getDecoder() {
        return decoder;
    }

    public LwM2mEncoder getEncoder() {
        return encoder;
    }

    public LinkSerializer getLinkSerializer() {
        return linkSerializer;
    }

    public LwM2mModel getModel() {
        return model;
    }

    public LwM2mAttributeParser getAttributeParser() {
        return attributeParser;
    }
}

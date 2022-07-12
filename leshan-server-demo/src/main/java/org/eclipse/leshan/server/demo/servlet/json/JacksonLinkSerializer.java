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
package org.eclipse.leshan.server.demo.servlet.json;

import java.io.IOException;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.attributes.Attribute;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class JacksonLinkSerializer extends StdSerializer<Link> {

    protected JacksonLinkSerializer(Class<Link> t) {
        super(t);
    }

    public JacksonLinkSerializer() {
        this(null);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public void serialize(Link link, JsonGenerator gen, SerializerProvider provider) throws IOException {
        ObjectNode olink = JsonNodeFactory.instance.objectNode();

        // add url
        olink.put("url", link.getUriReference());

        // add attributes
        ObjectNode oAttributes = JsonNodeFactory.instance.objectNode();
        olink.set("attributes", oAttributes);
        for (Attribute attr : link.getAttributes()) {
            oAttributes.put(attr.getName(), attr.getCoreLinkValue());
        }
        gen.writeTree(olink);
    }
}

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
package org.eclipse.leshan.core.link.lwm2m;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.leshan.core.link.DefaultLinkParser;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.LinkParser;
import org.eclipse.leshan.core.link.attributes.AttributeModel;
import org.eclipse.leshan.core.link.attributes.Attributes;
import org.eclipse.leshan.core.link.attributes.DefaultAttributeParser;
import org.eclipse.leshan.core.link.attributes.ResourceTypeAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.link.lwm2m.attributes.MixedLwM2mAttributeSet;
import org.eclipse.leshan.core.node.InvalidLwM2mPathException;
import org.eclipse.leshan.core.node.LwM2mPath;

public class DefaultLwM2mLinkParser implements LinkParser {

    private LinkParser parser;

    public DefaultLwM2mLinkParser() {
        // Define all supported Attributes
        Collection<AttributeModel<?>> suppportedAttributes = new ArrayList<AttributeModel<?>>();
        suppportedAttributes.addAll(Attributes.ALL);
        suppportedAttributes.addAll(LwM2mAttributes.ALL);

        // Create default link Parser
        this.parser = new DefaultLinkParser(new DefaultAttributeParser(suppportedAttributes));
    }

    @Override
    public Link[] parse(byte[] bytes) throws LinkParseException {
        Link[] links = parser.parse(bytes);

        // search root resource
        String rootPath = "/";
        for (Link link : links) {
            ResourceTypeAttribute rt = link.getAttributes().get(Attributes.RT);
            if (rt != null && rt.getValue().contains("oma.lwm2m")) {
                rootPath = link.getUriReference();
                break;
            }
        }

        // convert link to MixedLwM2mLink
        for (int i = 0; i < links.length; i++) {
            String path = links[i].getUriReference();
            if (path.startsWith(rootPath)) {

                // create lwm2m path
                LwM2mPath lwm2mPath;
                try {
                    lwm2mPath = LwM2mPath.parse(path, rootPath);
                } catch (InvalidLwM2mPathException e) {
                    String strLink = new String(bytes, StandardCharsets.UTF_8);
                    throw new LinkParseException(e, "Unable to parse link %s in %", links[i], strLink);
                }

                // create attributes
                MixedLwM2mAttributeSet attributes;
                try {
                    attributes = new MixedLwM2mAttributeSet(links[i].getAttributes().getAttributes());

                    // validate Attribute for this path
                    attributes.validate(lwm2mPath);
                } catch (IllegalStateException e) {
                    String strLink = new String(bytes, StandardCharsets.UTF_8);
                    throw new LinkParseException(e, "Unable to parse link %s in %s", links[i], strLink);
                }

                // create link and replace it
                links[i] = new MixedLwM2mLink(rootPath, LwM2mPath.parse(path, rootPath),
                        new MixedLwM2mAttributeSet(links[i].getAttributes().getAttributes()));
            }
        }
        return links;
    }
}

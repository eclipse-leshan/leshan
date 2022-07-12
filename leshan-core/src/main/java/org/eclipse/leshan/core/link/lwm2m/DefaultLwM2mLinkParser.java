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
import org.eclipse.leshan.core.link.attributes.Attribute;
import org.eclipse.leshan.core.link.attributes.AttributeModel;
import org.eclipse.leshan.core.link.attributes.Attributes;
import org.eclipse.leshan.core.link.attributes.DefaultAttributeParser;
import org.eclipse.leshan.core.link.attributes.ResourceTypeAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.link.lwm2m.attributes.MixedLwM2mAttributeSet;
import org.eclipse.leshan.core.node.InvalidLwM2mPathException;
import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * A Default Link Parser which is able to create more LWM2M flavored link.
 */
public class DefaultLwM2mLinkParser implements LwM2mLinkParser {

    private LinkParser parser;

    public DefaultLwM2mLinkParser() {
        // Define all supported Attributes
        Collection<AttributeModel<?>> suppportedAttributes = new ArrayList<AttributeModel<?>>();
        suppportedAttributes.addAll(Attributes.ALL);
        suppportedAttributes.addAll(LwM2mAttributes.ALL);

        // Create default link Parser
        this.parser = new DefaultLinkParser(new DefaultAttributeParser(suppportedAttributes));
    }

    public DefaultLwM2mLinkParser(Collection<? extends AttributeModel<?>> suppportedAttributes) {
        // Create default link Parser
        this.parser = new DefaultLinkParser(new DefaultAttributeParser(suppportedAttributes));
    }

    public DefaultLwM2mLinkParser(LinkParser internalLinkParser) {
        this.parser = internalLinkParser;
    }

    @Override
    public LwM2mLink[] parseLwM2mLinkFromCoreLinkFormat(byte[] bytes, String rootpath) throws LinkParseException {
        Link[] links = parser.parseCoreLinkFormat(bytes);
        LwM2mLink[] lwm2mLinks = new LwM2mLink[links.length];

        // define default lwm2m root path
        if (rootpath == null) {
            rootpath = "/";
        }

        // convert link to LwM2mLink
        for (int i = 0; i < links.length; i++) {
            String path = links[i].getUriReference();

            // create lwm2m path
            LwM2mPath lwm2mPath;
            try {
                lwm2mPath = LwM2mPath.parse(path, rootpath);
            } catch (InvalidLwM2mPathException e) {
                String strLink = new String(bytes, StandardCharsets.UTF_8);
                throw new LinkParseException(e, "Unable to parse link %s in %s", links[i], strLink);
            }

            try {
                // create attributes
                Collection<Attribute> attributes = links[i].getAttributes().asCollection();
                Collection<LwM2mAttribute<?>> lwm2mAttributes = new ArrayList<>(attributes.size());
                for (Attribute attribute : attributes) {
                    if (!(attribute instanceof LwM2mAttribute)) {
                        String strLink = new String(bytes, StandardCharsets.UTF_8);
                        throw new LinkParseException("Attribute %s is not a known LWM2M Attribute in %s",
                                attribute.getName(), strLink);
                    }
                    lwm2mAttributes.add((LwM2mAttribute<?>) attribute);
                }

                // validate Attribute for this path
                LwM2mAttributeSet lwm2mAttrSet = new LwM2mAttributeSet(lwm2mAttributes);
                lwm2mAttrSet.validate(lwm2mPath);

                // create link and replace it
                lwm2mLinks[i] = new LwM2mLink(rootpath, new LwM2mPath(path), lwm2mAttrSet);
            } catch (IllegalArgumentException e) {
                String strLink = new String(bytes, StandardCharsets.UTF_8);
                throw new LinkParseException(e, "Unable to parse link %s in %s", links[i], strLink);
            }
        }
        return lwm2mLinks;
    }

    @Override
    public Link[] parseCoreLinkFormat(byte[] bytes) throws LinkParseException {
        Link[] links = parser.parseCoreLinkFormat(bytes);

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
                // if it starts by rootPath this should be a LwM2mLink :

                // create lwm2m path
                LwM2mPath lwm2mPath;
                try {
                    lwm2mPath = LwM2mPath.parse(path, rootPath);
                } catch (InvalidLwM2mPathException e) {
                    String strLink = new String(bytes, StandardCharsets.UTF_8);
                    throw new LinkParseException(e, "Unable to parse link %s in %s", links[i], strLink);
                }

                try {
                    // create attributes
                    MixedLwM2mAttributeSet attributes = new MixedLwM2mAttributeSet(
                            links[i].getAttributes().asCollection());

                    // validate Attribute for this path
                    attributes.validate(lwm2mPath);

                    // create link and replace it
                    links[i] = new MixedLwM2mLink(rootPath, LwM2mPath.parse(path, rootPath), attributes);
                } catch (IllegalArgumentException e) {
                    String strLink = new String(bytes, StandardCharsets.UTF_8);
                    throw new LinkParseException(e, "Unable to parse link %s in %s", links[i], strLink);
                }
            }
        }
        return links;
    }
}

/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.integration.tests.util.assertion;

import org.assertj.core.api.AbstractObjectArrayAssert;
import org.assertj.core.api.ObjectArrayAssert;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkParseException;
import org.eclipse.leshan.core.link.lwm2m.DefaultLwM2mLinkParser;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLink;
import org.eclipse.leshan.core.link.lwm2m.LwM2mLinkParser;

public class LinkArrayAssert extends AbstractObjectArrayAssert<ObjectArrayAssert<Link>, Link> {

    public static final LwM2mLinkParser linkParser = new DefaultLwM2mLinkParser();

    public LinkArrayAssert(Link[] actual) {
        super(actual, LinkArrayAssert.class);
    }

    public static LinkArrayAssert assertThat(Link[] actual) {
        return new LinkArrayAssert(actual);
    }

    public LinkArrayAssert isLikeLinks(String expectedLinksAsString) {
        isNotNull();

        try {
            // TODO ideally we should refactor this method to ignore attribute order.
            Link[] expectedLink = linkParser.parseCoreLinkFormat(expectedLinksAsString.getBytes());
            containsExactlyInAnyOrder(expectedLink);
        } catch (LinkParseException e) {
            throw new IllegalStateException(e);
        }

        return this;
    }

    public LinkArrayAssert isLikeLwM2mLinks(String expectedLinksAsString) {
        isNotNull();

        try {
            // TODO ideally we should refactor this method to ignore attribute order.
            LwM2mLink[] expectedLink = linkParser.parseLwM2mLinkFromCoreLinkFormat(expectedLinksAsString.getBytes(),
                    null);
            containsExactlyInAnyOrder(expectedLink);
        } catch (LinkParseException e) {
            throw new IllegalStateException(e);
        }

        return this;
    }

    @Override
    protected ObjectArrayAssert<Link> newObjectArrayAssert(Link[] array) {
        return new ObjectArrayAssert<>(array);
    }
}

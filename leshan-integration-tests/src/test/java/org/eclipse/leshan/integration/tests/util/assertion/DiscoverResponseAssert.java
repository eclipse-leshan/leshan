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

import org.eclipse.leshan.core.response.DiscoverResponse;

public class DiscoverResponseAssert extends AbstractLwM2mResponseAssert<DiscoverResponseAssert, DiscoverResponse> {

    public DiscoverResponseAssert(DiscoverResponse actual) {
        super(actual, DiscoverResponseAssert.class);
    }

    public DiscoverResponseAssert hasObjectLinksLike(String links) {
        LinkArrayAssert.assertThat(actual.getObjectLinks()).isLikeLwM2mLinks(links);
        return this;
    }

    public static DiscoverResponseAssert assertThat(DiscoverResponse actual) {
        return new DiscoverResponseAssert(actual);
    }
}

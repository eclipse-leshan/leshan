/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Orange - Make LinkParser extensible.
 *******************************************************************************/
package org.eclipse.leshan.core.link.linkextension;

import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.link.LinkParamValue;
import org.eclipse.leshan.core.link.Parser;

/**
 * A CoRE Link link-extension parser interface defined in RFC6690
 * (https://datatracker.ietf.org/doc/html/RFC6690#section-2).
 */
public interface LinkExtensionParser extends Parser<Map<String, LinkParamValue>> {

    /**
     * Method should split content by delimiter respecting escaped characters.
     *
     * @param content content to parse.
     * @param delimiter character around content is split.
     * @return content divided by delimiter.
     */
    List<String> extractLinkExtensions(String content, char delimiter);

}

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
package org.eclipse.leshan.core.link.linkvalue;

import java.util.List;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.Parser;

/**
 * A CoRE Link link-value parser interface defined in RFC6690 (https://datatracker.ietf.org/doc/html/RFC6690#section-2).
 */
public interface LinkValueParser extends Parser<Link> {

    /**
     * Method should split content by delimiter respecting escaped characters.
     *
     * @param content content to parse.
     * @param delimiter character around content is split.
     * @return content divided by delimiter.
     */
    List<String> extractLinks(String content, char delimiter);

}

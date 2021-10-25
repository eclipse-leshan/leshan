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

import org.eclipse.leshan.core.link.Parser;

/**
 * A CoRE Link URI-reference parser interface defined in RFC3986
 * (https://datatracker.ietf.org/doc/html/rfc3986#appendix-A).
 */
public interface UriReferenceParser extends Parser<String> {

    /**
     * Method should extract URI-reference from the content and leave remaining content.
     *
     * @param content content to process.
     * @return object ExtractionResult contains extracted uriReference and the rest of the content.
     */
    ExtractionResult extractUriReference(String content);

    class ExtractionResult {
        public String uriReference;
        public String remaining;
    }

}

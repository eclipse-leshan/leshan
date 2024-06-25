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
package org.eclipse.leshan.core.link.lwm2m.attributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.leshan.core.link.attributes.Attribute;
import org.eclipse.leshan.core.link.attributes.AttributeModel;
import org.eclipse.leshan.core.link.attributes.DefaultAttributeParser;
import org.eclipse.leshan.core.link.attributes.InvalidAttributeException;

/**
 * A default implementation of {@link LwM2mAttributeParser}
 */
public class DefaultLwM2mAttributeParser extends DefaultAttributeParser implements LwM2mAttributeParser {

    public DefaultLwM2mAttributeParser() {
        super(LwM2mAttributes.ALL);
    }

    public DefaultLwM2mAttributeParser(Collection<? extends AttributeModel<?>> knownAttributes) {
        super(knownAttributes);
    }

    @Override
    public Collection<LwM2mAttribute<?>> parseUriQuery(String uriQueries) throws InvalidAttributeException {
        if (uriQueries == null)
            return null;

        // We use split with limit = -1 to be sure "split" will discard trailing empty strings
        String[] queriesArray = uriQueries.split("&", -1);
        return parseQueryParams(queriesArray);
    }

    @Override
    public Collection<LwM2mAttribute<?>> parseQueryParams(String... queryParams) throws InvalidAttributeException {
        return parseQueryParams(Arrays.asList(queryParams));
    }

    @Override
    public Collection<LwM2mAttribute<?>> parseQueryParams(Collection<String> queryParams)
            throws InvalidAttributeException {
        ArrayList<LwM2mAttribute<?>> attributes = new ArrayList<>();

        for (String param : queryParams) {
            int indexOfEqual = param.indexOf('=');
            Attribute attr;
            if (indexOfEqual == -1) {
                attr = parseQueryParamValue(param, null);
            } else if (indexOfEqual == param.length() - 1) {
                throw new InvalidAttributeException("Cannot parse query param '%s', value is expected after '=", param);
            } else {
                String paramName = param.substring(0, indexOfEqual);
                String paramValue = param.substring(indexOfEqual + 1);
                attr = parseQueryParamValue(paramName, paramValue);
            }

            if (attr instanceof LwM2mAttribute<?>) {
                attributes.add((LwM2mAttribute<?>) attr);
            } else {
                throw new InvalidAttributeException("Cannot parse query param '%s', param %s is not a LWM2M attribute",
                        param, attr.getName());
            }
        }
        return attributes;
    }

    @Override
    public Attribute parseQueryParamValue(String attributeName, String attributeValue)
            throws InvalidAttributeException {

        if (attributeName == null || attributeName.isEmpty()) {
            throw new InvalidAttributeException("unable to parse an attribute without name");
        }

        // search model
        AttributeModel<?> model = getKnownAttributes().get(attributeName);
        if (model == null || !(model instanceof LwM2mAttributeModel<?>)) {
            throw new InvalidAttributeException("%s attribute is unknown or not a LWM2M attribute", attributeName);
        }
        LwM2mAttributeModel<?> lwm2mModel = (LwM2mAttributeModel<?>) model;

        if (attributeValue == null) {
            // handle value less attribute.
            if (lwm2mModel.queryParamCanBeValueless()) {
                return model.createEmptyAttribute();
            } else {
                throw new InvalidAttributeException("%s attribute must have a value when used as query param",
                        attributeName);
            }
        } else {
            // There nothing clear in specification about format which should be used in Query
            // So we reuse the CoRE link format (hopping it's OK)
            return parseCoreLinkValue(attributeName, attributeValue);
        }
    }
}

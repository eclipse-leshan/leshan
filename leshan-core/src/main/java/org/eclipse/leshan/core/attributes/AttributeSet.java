/*******************************************************************************
 * Copyright (c) 2013-2018 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Daniel Persson (Husqvarna Group) - Attribute support
 *******************************************************************************/
package org.eclipse.leshan.core.attributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.util.Validate;

/**
 * A collection of {@link Attribute} instances that are handled as a collection
 * that must adhere to rules that are specified in LwM2m, e.g. that the 'pmin' attribute
 * must be less than the 'pmax' attribute, if they're both part of the same AttributeSet.
 */
public class AttributeSet {
    
    private Map<AttributeName, Attribute> attributeMap = new LinkedHashMap<>();
    
    public AttributeSet(Attribute...attributes) {
        if (attributes != null && attributes.length > 0) {
            for (Attribute attr : attributes) {
                // Check for duplicates
                if (attributeMap.containsKey(attr.getName())) {
                    throw new IllegalArgumentException(String.format("Cannot create attribute set with duplicates (attr: '%s')",
                            attr.getCoRELinkParam())); 
                }
                attributeMap.put(attr.getName(), attr);
            }
        }
    }
    
    /**
     * Creates an attribute set from a list of query params.
     */
    public AttributeSet(String[] queryParams) {
        for (String param : queryParams) {
            String[] keyAndValue = param.split("=");
            if (keyAndValue.length != 2) {
                throw new IllegalArgumentException(String.format("Cannot parse query param '%s'", param));
            }
            Attribute attr = Attribute.create(keyAndValue[0], keyAndValue[1]);
            attributeMap.put(attr.getName(), attr);
        }
    }

    public void validate(AssignationLevel assignationLevel) {
        // Can all attributes be assigned to this level?
        for (Attribute attr : attributeMap.values()) {
            if (!attr.canBeAssignedTo(assignationLevel)) {
                throw new IllegalArgumentException(String.format("Attribute '%s' cannot be assigned to level %s",
                        attr.getCoRELinkParam(), assignationLevel.name()));
            }
        }
        Attribute pmin = attributeMap.get(AttributeName.MINIMUM_PERIOD);
        Attribute pmax = attributeMap.get(AttributeName.MAXIMUM_PERIOD);
        if ((pmin != null) && (pmax != null) && (Long) pmin.getValue() > (Long) pmax.getValue()) {
            throw new IllegalArgumentException(String.format("Cannot write attributes where '%s' > '%s'",
                    pmin.getCoRELinkParam(), pmax.getCoRELinkParam()));
        }
    }

    /**
     * Returns a new AttributeSet, containing only the attributes that have a matching
     * Attachment level. 
     * @param attachment the Attachment level to filter by
     * @return a new {@link AttributeSet} containing the filtered attributes
     */
    public AttributeSet filter(Attachment attachment) {
        List<Attribute> attrs = new ArrayList<>();
        for (Attribute attr : getAttributes()) {
            if (attr.getAttachment() == attachment) {
                attrs.add(attr);
            }
        }
        return new AttributeSet(attrs.toArray(new Attribute[attrs.size()]));
    }
    
    /**
     * Creates a new AttributeSet by merging another AttributeSet onto this instance.
     * @param attributes the AttributeSet that should be merged onto this instance. Attributes in this
     * set will overwrite existing attribute values, if present. If this is null, the
     * new attribute set will effectively be a clone of the existing one
     * @return the merged AttributeSet 
     */
    public AttributeSet merge(AttributeSet attributes) {
        Map<AttributeName, Attribute> merged = new LinkedHashMap<>();
        for (Attribute attr : getAttributes()) {
            merged.put(attr.getName(), attr);
        }
        if (attributes != null) {
            for (Attribute attr : attributes.getAttributes()) {
                merged.put(attr.getName(), attr);
            }
        }
        return new AttributeSet(merged.values().toArray(new Attribute[merged.size()]));        
    }

    /**
     * Returns the attributes as a map with the CoRELinkParam as key and the attribute value as map value.
     * @return the attributes map
     */
    public Map<String, Object> getMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Attribute attr : attributeMap.values()) {
            result.put(attr.getCoRELinkParam(), attr.getValue());
        }
        return result;
    }
    
    public Attribute[] getAttributes() {
        return attributeMap.values().toArray(new Attribute[attributeMap.size()]);
    }
    
    public String[] toQueryParams() {
        List<String> queries = new LinkedList<>();
        for (Attribute attr : attributeMap.values()) {
            queries.add(String.format("%s=%s", attr.getCoRELinkParam(), attr.getValue()));
        }
        return queries.toArray(new String[queries.size()]);
    }

    @Override
    public String toString() {
        return String.join("&", toQueryParams()); 
    }
}

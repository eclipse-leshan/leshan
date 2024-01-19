/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.link.attributes.Attribute;
import org.eclipse.leshan.core.link.attributes.AttributeSet;
import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * An {@link AttributeSet} containing {@link LwM2mAttribute} but also tolerate not LWM2M {@link Attribute}.
 * <p>
 * It must adhere to rules that are specified in LwM2m, e.g. that the 'pmin' attribute must be less than the 'pmax'
 * attribute, if they're both part of the same AttributeSet.
 */
public class MixedLwM2mAttributeSet extends AttributeSet {

    private Iterable<LwM2mAttribute<?>> lwm2mAttributes;

    public MixedLwM2mAttributeSet(Attribute... attributes) {
        this(Arrays.asList(attributes));
    }

    public MixedLwM2mAttributeSet(Collection<? extends Attribute> attributes) {
        super(attributes);
        this.lwm2mAttributes = new Iterable<LwM2mAttribute<?>>() {

            @Override
            public Iterator<LwM2mAttribute<?>> iterator() {
                final Iterator<? extends Attribute> it = asCollection().iterator();

                return new Iterator<LwM2mAttribute<?>>() {
                    private LwM2mAttribute<?> lastAttribute;

                    @Override
                    public boolean hasNext() {
                        while (it.hasNext()) {
                            Attribute next = it.next();
                            if (next instanceof LwM2mAttribute) {
                                lastAttribute = (LwM2mAttribute<?>) next;
                                return true;
                            } // else we ignore it and continue to check
                        }
                        return false;
                    }

                    @Override
                    public LwM2mAttribute<?> next() {
                        if (lastAttribute != null) {
                            LwM2mAttribute<?> res = lastAttribute;
                            lastAttribute = null;
                            return res;
                        } else {
                            Attribute next = it.next();
                            if (next instanceof LwM2mAttribute) {
                                return (LwM2mAttribute<?>) next;
                            } else {
                                return this.next();
                            }
                        }
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }
        };
    }

    public void validate(LwM2mPath path) {
        // Can all attributes be assigned to this path
        for (LwM2mAttribute<?> attr : getLwM2mAttributes()) {
            String errorMessage = attr.getModel().getApplicabilityError(path, null);
            if (errorMessage != null) {
                throw new IllegalArgumentException(errorMessage);
            }
        }
    }

    // TODO not sure we still need this function
    public void validate(AssignationLevel assignationLevel) {
        // Can all attributes be assigned to this level?
        for (LwM2mAttribute<?> attr : getLwM2mAttributes()) {
            if (!attr.canBeAssignedTo(assignationLevel)) {
                throw new IllegalArgumentException(String.format("Attribute '%s' cannot be assigned to level %s",
                        attr.getName(), assignationLevel.name()));
            }
        }
    }

    /**
     * Returns a new AttributeSet, containing only the attributes that have a matching Attachment level.
     *
     * @param attachment the Attachment level to filter by
     * @return a new {@link LwM2mAttributeSet} containing the filtered attributes
     */
    public LwM2mAttributeSet filter(Attachment attachment) {
        List<LwM2mAttribute<?>> attrs = new ArrayList<>();
        for (LwM2mAttribute<?> attr : getLwM2mAttributes()) {
            if (attr.getAttachment() == attachment) {
                attrs.add(attr);
            }
        }
        return new LwM2mAttributeSet(attrs);
    }

    /**
     * Creates a new AttributeSet by merging another AttributeSet onto this instance.
     *
     * @param attributes the AttributeSet that should be merged onto this instance. Attributes in this set will
     *        overwrite existing attribute values, if present. If this is null, the new attribute set will effectively
     *        be a clone of the existing one
     * @return the merged AttributeSet
     */
    public LwM2mAttributeSet merge(LwM2mAttributeSet attributes) {
        Map<String, LwM2mAttribute<?>> merged = new LinkedHashMap<>();
        for (LwM2mAttribute<?> attr : getLwM2mAttributes()) {
            merged.put(attr.getName(), attr);
        }
        if (attributes != null) {
            for (LwM2mAttribute<?> attr : attributes.getLwM2mAttributes()) {
                merged.put(attr.getName(), attr);
            }
        }
        return new LwM2mAttributeSet(merged.values());
    }

    /**
     * Like {@link #merge(LwM2mAttributeSet)} except if a given {@link LwM2mAttribute} has no value, it will remove this
     * attribute from final result.
     */
    public LwM2mAttributeSet apply(LwM2mAttributeSet attributes) {
        Map<String, LwM2mAttribute<?>> merged = new LinkedHashMap<>();
        for (LwM2mAttribute<?> attr : getLwM2mAttributes()) {
            merged.put(attr.getName(), attr);
        }
        if (attributes != null) {
            for (LwM2mAttribute<?> attr : attributes.getLwM2mAttributes()) {
                if (attr.hasValue()) {
                    merged.put(attr.getName(), attr);
                } else {
                    merged.remove(attr.getName());
                }
            }
        }
        return new LwM2mAttributeSet(merged.values());
    }

    /**
     * Returns the attributes as a map with the CoRELinkParam as key and the attribute value as map value.
     *
     * @return the attributes map
     */
    public Map<String, Object> getMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Attribute attr : asCollection()) {
            result.put(attr.getName(), attr.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    public Iterable<LwM2mAttribute<?>> getLwM2mAttributes() {
        return lwm2mAttributes;
    }

    public LwM2mAttribute<?> getLwM2mAttribute(String attrName) {
        Attribute attribute = get(attrName);
        if (attribute instanceof LwM2mAttribute) {
            return (LwM2mAttribute<?>) attribute;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> LwM2mAttribute<T> getLwM2mAttribute(LwM2mAttributeModel<T> model) {
        Attribute attribute = get(model.getName());
        if (attribute instanceof LwM2mAttribute) {
            if (((LwM2mAttribute<?>) attribute).getModel().equals(model)) {
                return (LwM2mAttribute<T>) attribute;
            }
        }
        return null;
    }

    public String[] toQueryParams() {
        List<String> queries = new LinkedList<>();
        for (Attribute attr : asCollection()) {
            if (attr.getValue() != null) {
                queries.add(String.format("%s=%s", attr.getName(), attr.getValue()));
            } else {
                if (attr instanceof LwM2mAttribute<?>
                        && !((LwM2mAttribute<?>) attr).getModel().queryParamCanBeValueless()) {
                    throw new IllegalStateException(String.format(
                            "Attribute %s can not have null value when serialized in query params", attr.getName()));
                }
                queries.add(attr.getName());
            }
        }
        return queries.toArray(new String[queries.size()]);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String[] queryParams = toQueryParams();
        for (int a = 0; a < queryParams.length; a++) {
            sb.append(a < queryParams.length - 1 ? queryParams[a] + "&" : queryParams[a]);
        }
        return sb.toString();
    }
}

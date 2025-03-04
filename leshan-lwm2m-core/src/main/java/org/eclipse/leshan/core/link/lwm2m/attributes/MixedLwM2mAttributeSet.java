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

import java.math.BigDecimal;
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
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * An {@link AttributeSet} containing {@link LwM2mAttribute} but also tolerate not LWM2M {@link Attribute}.
 * <p>
 * It must adhere to rules that are specified in LwM2m, e.g. that the 'pmin' attribute must be less than the 'pmax'
 * attribute, if they're both part of the same AttributeSet.
 */
public class MixedLwM2mAttributeSet extends AttributeSet {

    private final Iterable<LwM2mAttribute<?>> lwm2mAttributes = () -> {
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
    };

    public MixedLwM2mAttributeSet(Attribute... attributes) {
        this(Arrays.asList(attributes));
    }

    public MixedLwM2mAttributeSet(Collection<? extends Attribute> attributes) {
        super(attributes);
    }

    public void validate() throws InvalidAttributesException {
        // check some consistency about attribute set
        // pmin SHOULD BE LESSER or EQUAL TO pmax
        // https://datatracker.ietf.org/doc/html/draft-ietf-core-dynlink-07#section-4.2
        LwM2mAttribute<Long> pmin = this.getLwM2mAttribute(LwM2mAttributes.MINIMUM_PERIOD);
        LwM2mAttribute<Long> pmax = this.getLwM2mAttribute(LwM2mAttributes.MAXIMUM_PERIOD);
        if ((pmin != null) && (pmax != null) //
                && pmin.hasValue() && pmax.hasValue() //
                && !(pmin.getValue() <= pmax.getValue())) {
            throw new InvalidAttributesException("Attributes doesn't fulfill '%s'<= '%s' condition", pmin.getName(),
                    pmax.getName());
        }

        // epmin SHOULD BE LESSER or EQUAL TO epmax
        // https://datatracker.ietf.org/doc/html/draft-ietf-core-conditional-attributes-06#section-3.2.4
        LwM2mAttribute<Long> epmin = this.getLwM2mAttribute(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD);
        LwM2mAttribute<Long> epmax = this.getLwM2mAttribute(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD);
        if ((epmin != null) && (epmax != null) //
                && epmin.hasValue() && epmax.hasValue() //
                && !(epmin.getValue() <= epmax.getValue())) {
            throw new InvalidAttributesException("Attributes doesn't fulfill '%s'<= '%s' condition", epmin.getName(),
                    epmax.getName());
        }

        // "lt" value < "gt" value MUST BE TRUE
        // https://www.openmobilealliance.org/release/LightweightM2M/V1_2_1-20221209-A/HTML-Version/OMA-TS-LightweightM2M_Core-V1_2_1-20221209-A.html#7-3-0-73-Attributes
        LwM2mAttribute<BigDecimal> lt = this.getLwM2mAttribute(LwM2mAttributes.LESSER_THAN);
        LwM2mAttribute<BigDecimal> gt = this.getLwM2mAttribute(LwM2mAttributes.GREATER_THAN);
        if ((lt != null) && (gt != null) //
                && lt.hasValue() && gt.hasValue() //
                && !(lt.getValue().compareTo(gt.getValue()) < 0)) {
            // && !(lt.getValue() < gt.getValue())) {
            throw new InvalidAttributesException("Attributes doesn't fulfill '%s'< '%s' condition", lt.getName(),
                    gt.getName());
        }

        // ("lt" value + 2*"st" values) <"gt" value MUST BE TRUE
        // https://www.openmobilealliance.org/release/LightweightM2M/V1_2_1-20221209-A/HTML-Version/OMA-TS-LightweightM2M_Core-V1_2_1-20221209-A.html#7-3-0-73-Attributes
        LwM2mAttribute<BigDecimal> st = this.getLwM2mAttribute(LwM2mAttributes.STEP);
        if ((lt != null) && (gt != null) && (st != null) && lt.hasValue() && gt.hasValue() && st.hasValue() //
                && !(lt.getValue().add(st.getValue().multiply(new BigDecimal(2))).compareTo(gt.getValue()) < 0)) {
            throw new InvalidAttributesException(
                    "Attributes doesn't fulfill  (\"lt\" value + 2*\"st\" values) <\"gt\") condition");
        }
    }

    public void validate(LwM2mPath path) throws InvalidAttributesException {
        validate(path, null);
    }

    public void validate(LwM2mPath path, ObjectModel objectModel) throws InvalidAttributesException {
        // Can all attributes be assigned to this path
        for (LwM2mAttribute<?> attr : getLwM2mAttributes()) {
            String errorMessage = attr.getModel().getApplicabilityError(path, objectModel);
            if (errorMessage != null) {
                throw new InvalidAttributesException(errorMessage);
            }
        }
        validate();
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
                if (attr.hasValue()) {
                    merged.put(attr.getName(), attr);
                }
            }
        }
        return new LwM2mAttributeSet(merged.values());
    }

    /**
     * Creates a new AttributeSet by merging given attributes.
     *
     * @param attributes the array that should be merged onto this instance. Attributes in this array will overwrite
     *        existing attribute values, if present. If this is null, the new attribute set will effectively be a clone
     *        of the existing one
     * @return the merged AttributeSet
     */
    public LwM2mAttributeSet merge(LwM2mAttribute<?>... attributes) {
        return this.merge(new LwM2mAttributeSet(attributes));
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
        if ((attribute instanceof LwM2mAttribute) //
                && (((LwM2mAttribute<?>) attribute).getModel().equals(model))) {
            return (LwM2mAttribute<T>) attribute;
        }
        return null;

    }

    public String[] toQueryParams() {
        List<String> queries = new LinkedList<>();
        for (Attribute attr : asCollection()) {
            if (attr instanceof LwM2mAttribute<?>) {
                queries.add(((LwM2mAttribute<?>) attr).toQueryParamFormat());
            } else {
                throw new IllegalStateException(
                        String.format("only LwM2mAttribute can be converted to query parameters, attribute %s is a %s",
                                attr.getName(), attr.getClass().getSimpleName()));
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

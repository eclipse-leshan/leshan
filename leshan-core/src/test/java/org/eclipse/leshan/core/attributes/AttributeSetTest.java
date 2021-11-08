/*******************************************************************************
 * Copyright (c) 2013-2018 Sierra Wireless and others.
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
 *     Daniel Persson (Husqvarna Group) - Attribute support
 *******************************************************************************/
package org.eclipse.leshan.core.attributes;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Map;

import org.junit.Test;

public class AttributeSetTest {

    @Test
    public void should_provide_query_params() {
        AttributeSet sut = new AttributeSet(new LwM2mAttribute(LwM2mAttributeModel.OBJECT_VERSION, "1.1"),
                new LwM2mAttribute(LwM2mAttributeModel.MINIMUM_PERIOD, 5L), new LwM2mAttribute(LwM2mAttributeModel.MAXIMUM_PERIOD, 60L),
                new LwM2mAttribute(LwM2mAttributeModel.EVALUATE_MINIMUM_PERIOD, 30L),
                new LwM2mAttribute(LwM2mAttributeModel.EVALUATE_MAXIMUM_PERIOD, 45L));
        assertEquals("ver=1.1&pmin=5&pmax=60&epmin=30&epmax=45", sut.toString());

        AttributeSet res = AttributeSet.parse(sut.toString());
        assertEquals(sut, res);
    }

    @Test
    public void no_value_to_unset() {
        AttributeSet sut = new AttributeSet(new LwM2mAttribute(LwM2mAttributeModel.MINIMUM_PERIOD),
                new LwM2mAttribute(LwM2mAttributeModel.MAXIMUM_PERIOD), new LwM2mAttribute(LwM2mAttributeModel.EVALUATE_MINIMUM_PERIOD),
                new LwM2mAttribute(LwM2mAttributeModel.EVALUATE_MAXIMUM_PERIOD));
        assertEquals("pmin&pmax&epmin&epmax", sut.toString());

        AttributeSet res = AttributeSet.parse(sut.toString());
        assertEquals(sut, res);
    }

    @Test
    public void should_get_map() {
        AttributeSet sut = new AttributeSet(new LwM2mAttribute(LwM2mAttributeModel.OBJECT_VERSION, "1.1"),
                new LwM2mAttribute(LwM2mAttributeModel.MINIMUM_PERIOD, 5L), new LwM2mAttribute(LwM2mAttributeModel.MAXIMUM_PERIOD, 60L),
                new LwM2mAttribute(LwM2mAttributeModel.EVALUATE_MINIMUM_PERIOD, 30L),
                new LwM2mAttribute(LwM2mAttributeModel.EVALUATE_MAXIMUM_PERIOD, 45L));
        Map<String, Object> map = sut.getMap();
        assertEquals("1.1", map.get("ver"));
        assertEquals(5L, map.get("pmin"));
        assertEquals(60L, map.get("pmax"));
        assertEquals(45L, map.get("epmax"));
        assertEquals(30L, map.get("epmin"));
    }

    @Test
    public void should_merge() {
        AttributeSet sut = new AttributeSet(new LwM2mAttribute(LwM2mAttributeModel.OBJECT_VERSION, "1.1"),
                new LwM2mAttribute(LwM2mAttributeModel.MINIMUM_PERIOD, 5L), new LwM2mAttribute(LwM2mAttributeModel.MAXIMUM_PERIOD, 60L));
        AttributeSet set2 = new AttributeSet(new LwM2mAttribute(LwM2mAttributeModel.MINIMUM_PERIOD, 10L),
                new LwM2mAttribute(LwM2mAttributeModel.MAXIMUM_PERIOD, 120L));

        AttributeSet merged = sut.merge(set2);

        Map<String, Object> map = merged.getMap();
        assertEquals("1.1", map.get("ver"));
        assertEquals(10L, map.get("pmin"));
        assertEquals(120L, map.get("pmax"));

        // Assert that the original attribute sets are untouched
        map = sut.getMap();
        assertEquals(5L, map.get("pmin"));
        map = set2.getMap();
        assertEquals(null, map.get("ver"));
    }

    @Test
    public void should_to_string() {
        AttributeSet sut = new AttributeSet(new LwM2mAttribute(LwM2mAttributeModel.OBJECT_VERSION, "1.1"),
                new LwM2mAttribute(LwM2mAttributeModel.MINIMUM_PERIOD, 5L), new LwM2mAttribute(LwM2mAttributeModel.MAXIMUM_PERIOD, 60L),
                new LwM2mAttribute(LwM2mAttributeModel.EVALUATE_MINIMUM_PERIOD, 30L),
                new LwM2mAttribute(LwM2mAttributeModel.EVALUATE_MAXIMUM_PERIOD, 45L));

        assertEquals("ver=1.1&pmin=5&pmax=60&epmin=30&epmax=45", sut.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_on_duplicates() {
        new AttributeSet(new LwM2mAttribute(LwM2mAttributeModel.OBJECT_VERSION, "1.1"),
                new LwM2mAttribute(LwM2mAttributeModel.MINIMUM_PERIOD, 5L), new LwM2mAttribute(LwM2mAttributeModel.MINIMUM_PERIOD, 60L));
    }

    @Test
    public void should_validate_assignation() {
        AttributeSet sut = new AttributeSet(new LwM2mAttribute(LwM2mAttributeModel.MINIMUM_PERIOD, 5L),
                new LwM2mAttribute(LwM2mAttributeModel.MAXIMUM_PERIOD, 60L));
        Collection<LwM2mAttribute> attributes = sut.getAttributes();
        assertEquals(2, attributes.size());
        sut.validate(AssignationLevel.RESOURCE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_on_invalid_assignation_level() {
        AttributeSet sut = new AttributeSet(new LwM2mAttribute(LwM2mAttributeModel.OBJECT_VERSION, "1.1"),
                new LwM2mAttribute(LwM2mAttributeModel.MINIMUM_PERIOD, 5L), new LwM2mAttribute(LwM2mAttributeModel.MAXIMUM_PERIOD, 60L));

        // OBJECT_VERSION cannot be assigned on resource level
        sut.validate(AssignationLevel.RESOURCE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_on_invalid_pmin_pmax() {
        AttributeSet sut = new AttributeSet(new LwM2mAttribute(LwM2mAttributeModel.MINIMUM_PERIOD, 50L),
                new LwM2mAttribute(LwM2mAttributeModel.MAXIMUM_PERIOD, 49L));

        // pmin cannot be greater then pmax
        sut.validate(AssignationLevel.RESOURCE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void should_throw_on_invalid_epmin_epmax() {
        AttributeSet sut = new AttributeSet(new LwM2mAttribute(LwM2mAttributeModel.EVALUATE_MINIMUM_PERIOD, 50L),
                new LwM2mAttribute(LwM2mAttributeModel.EVALUATE_MAXIMUM_PERIOD, 49L));

        // pmin cannot be greater then pmax
        sut.validate(AssignationLevel.RESOURCE);
    }
}

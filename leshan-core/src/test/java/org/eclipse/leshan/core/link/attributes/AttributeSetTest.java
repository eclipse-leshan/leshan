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
package org.eclipse.leshan.core.link.attributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.util.Collection;
import java.util.Map;

import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.link.lwm2m.attributes.AssignationLevel;
import org.eclipse.leshan.core.link.lwm2m.attributes.DefaultLwM2mAttributeParser;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeParser;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.junit.jupiter.api.Test;

public class AttributeSetTest {
    private static LwM2mAttributeParser parser = new DefaultLwM2mAttributeParser();

    @Test
    public void should_provide_query_params() throws InvalidAttributeException {
        LwM2mAttributeSet sut = new LwM2mAttributeSet(
                LwM2mAttributes.create(LwM2mAttributes.OBJECT_VERSION, new Version("1.1")),
                LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 5L),
                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 60L),
                LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD, 30L),
                LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD, 45L));
        assertEquals("ver=1.1&pmin=5&pmax=60&epmin=30&epmax=45", sut.toString());

        LwM2mAttributeSet res = new LwM2mAttributeSet(parser.parseUriQuery(sut.toString()));
        assertEquals(sut, res);
    }

    @Test
    public void no_value_to_unset() throws InvalidAttributeException {
        LwM2mAttributeSet sut = new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD),
                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD),
                LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD),
                LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD));
        assertEquals("pmin&pmax&epmin&epmax", sut.toString());

        LwM2mAttributeSet res = new LwM2mAttributeSet(parser.parseUriQuery(sut.toString()));
        assertEquals(sut, res);
    }

    @Test
    public void should_get_map() {
        LwM2mAttributeSet sut = new LwM2mAttributeSet(
                LwM2mAttributes.create(LwM2mAttributes.OBJECT_VERSION, new Version("1.1")),
                LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 5L),
                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 60L),
                LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD, 30L),
                LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD, 45L));
        Map<String, Object> map = sut.getMap();
        assertEquals(new Version("1.1"), map.get("ver"));
        assertEquals(5L, map.get("pmin"));
        assertEquals(60L, map.get("pmax"));
        assertEquals(45L, map.get("epmax"));
        assertEquals(30L, map.get("epmin"));
    }

    @Test
    public void should_merge() {
        LwM2mAttributeSet sut = new LwM2mAttributeSet(
                LwM2mAttributes.create(LwM2mAttributes.OBJECT_VERSION, new Version("1.1")),
                LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 5L),
                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 60L));
        LwM2mAttributeSet set2 = new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 10L),
                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 120L));

        LwM2mAttributeSet merged = sut.merge(set2);

        Map<String, Object> map = merged.getMap();
        assertEquals(new Version("1.1"), map.get("ver"));
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
        LwM2mAttributeSet sut = new LwM2mAttributeSet(
                LwM2mAttributes.create(LwM2mAttributes.OBJECT_VERSION, new Version("1.1")),
                LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 5L),
                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 60L),
                LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD, 30L),
                LwM2mAttributes.create(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD, 45L));

        assertEquals("ver=1.1&pmin=5&pmax=60&epmin=30&epmax=45", sut.toString());
    }

    @Test
    public void should_throw_on_duplicates() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.OBJECT_VERSION, new Version("1.1")),
                    LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 5L),
                    LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 60L));
        });
    }

    @Test
    public void should_validate_assignation() {
        LwM2mAttributeSet sut = new LwM2mAttributeSet(LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 5L),
                LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 60L));
        Collection<Attribute> attributes = sut.asCollection();
        assertEquals(2, attributes.size());
        sut.validate(AssignationLevel.RESOURCE);
    }

    @Test
    public void should_throw_on_invalid_assignation_level() {
        assertThrowsExactly(IllegalArgumentException.class, () -> {
            LwM2mAttributeSet sut = new LwM2mAttributeSet(
                    LwM2mAttributes.create(LwM2mAttributes.OBJECT_VERSION, new Version("1.1")),
                    LwM2mAttributes.create(LwM2mAttributes.MINIMUM_PERIOD, 5L),
                    LwM2mAttributes.create(LwM2mAttributes.MAXIMUM_PERIOD, 60L));

            // OBJECT_VERSION cannot be assigned on resource level
            sut.validate(AssignationLevel.RESOURCE);
        });
    }
}

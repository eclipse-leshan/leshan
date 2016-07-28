/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.eclipse.leshan.ObserveSpec;
import org.junit.Test;

public class ObserveSpecParserTest {

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFormat() {
        ObserveSpec.parse(Arrays.asList("a=b=c"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidKey() {
        ObserveSpec.parse(Arrays.asList("a=b"));
    }

    @Test
    public void testCancel() {
        testCorrectSpec(new ObserveSpec.Builder().cancel().build(), "cancel");
    }

    @Test
    public void testGreaterThan6() {
        testCorrectSpec(new ObserveSpec.Builder().greaterThan(6).build(), "gt=6.0");
    }

    @Test
    public void testGreaterThan8() {
        testCorrectSpec(new ObserveSpec.Builder().greaterThan(8).build(), "gt=8.0");
    }

    @Test
    public void testLessThan8() {
        testCorrectSpec(new ObserveSpec.Builder().lessThan(8).build(), "lt=8.0");
    }

    @Test
    public void testLessThan8AndGreaterThan14() {
        testCorrectSpec(new ObserveSpec.Builder().greaterThan(14).lessThan(8).build(), "lt=8.0", "gt=14.0");
    }

    @Test
    public void testAllTheThings() {
        final ObserveSpec spec = new ObserveSpec.Builder().greaterThan(14).lessThan(8).minPeriod(5).maxPeriod(10)
                .step(1).build();
        testCorrectSpec(spec, "gt=14.0", "lt=8.0", "pmin=5", "pmax=10", "st=1.0");
    }

    @Test(expected = IllegalStateException.class)
    public void testOutOfOrderPminPmax() {
        ObserveSpec.parse(Arrays.asList("pmin=50", "pmax=10"));
    }

    private void testCorrectSpec(final ObserveSpec expected, final String... inputs) {
        final List<String> queries = Arrays.asList(inputs);
        final ObserveSpec actual = ObserveSpec.parse(queries);
        assertSameSpecs(expected, actual);
    }

    private void assertSameSpecs(final ObserveSpec expected, final ObserveSpec actual) {
        assertEquals(expected.toString(), actual.toString());
    }

}

/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LwM2mAttributesTest {

    private static Stream<Arguments> supportNullAttributes() {
        return Stream.of(//
                Arguments.of(LwM2mAttributes.MINIMUM_PERIOD), //
                Arguments.of(LwM2mAttributes.MAXIMUM_PERIOD), //
                Arguments.of(LwM2mAttributes.EVALUATE_MINIMUM_PERIOD), //
                Arguments.of(LwM2mAttributes.EVALUATE_MAXIMUM_PERIOD), //
                Arguments.of(LwM2mAttributes.LESSER_THAN), //
                Arguments.of(LwM2mAttributes.GREATER_THAN), //
                Arguments.of(LwM2mAttributes.STEP) //
        );
    }

    @ParameterizedTest(name = "Tests {index} : {0}")
    @MethodSource("supportNullAttributes")
    void check_attribute_can_be_created_with_null_value(LwM2mAttributeModel<?> model) {
        LwM2mAttribute<?> attribute = LwM2mAttributes.create(model);
        assertNotNull(attribute);
        assertFalse(attribute.hasValue());
        assertNull(attribute.getValue());
        attribute = LwM2mAttributes.create(model, null);
        assertNotNull(attribute);
        assertFalse(attribute.hasValue());
        assertNull(attribute.getValue());
    }

    private static Stream<Arguments> doesntSupportNullAttributes() {
        return Stream.of(//
                Arguments.of(LwM2mAttributes.DIMENSION), //
                Arguments.of(LwM2mAttributes.ENABLER_VERSION), //
                Arguments.of(LwM2mAttributes.OBJECT_VERSION), //
                Arguments.of(LwM2mAttributes.SHORT_SERVER_ID), //
                Arguments.of(LwM2mAttributes.SERVER_URI)//
        );
    }

    @ParameterizedTest(name = "Tests {index} : {0}")
    @MethodSource("doesntSupportNullAttributes")
    void check_attribute_can_not_be_created_with_null_value(LwM2mAttributeModel<?> model) {
        assertThrows(IllegalArgumentException.class, () -> LwM2mAttributes.create(model));
        assertThrows(IllegalArgumentException.class, () -> LwM2mAttributes.create(model, null));
    }
}

/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.client.notification.checker;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.util.datatype.ULong;

/**
 * A {@link CriteriaBasedOnValueChecker} for {@link LwM2mResource} of {@link Type#UNSIGNED_INTEGER}
 */
public class UnsignedIntegerChecker implements CriteriaBasedOnValueChecker {

    @Override
    public boolean shouldTriggerNotificationBasedOnValueChange(LwM2mAttributeSet attributes, Object lastSentValue,
            Object newValue) {
        BigInteger lastSentULong = ((ULong) lastSentValue).toBigInteger();
        BigInteger newULong = ((ULong) newValue).toBigInteger();
        boolean hasNumericalAttributes = false;

        if (attributes.contains(LwM2mAttributes.STEP)) {
            hasNumericalAttributes = true;

            // Handle Step
            BigDecimal step = attributes.get(LwM2mAttributes.STEP).getValue();
            BigInteger stepRounded = step.setScale(0, RoundingMode.CEILING).toBigIntegerExact();
            if (lastSentULong.subtract(newULong).abs().subtract(stepRounded).signum() >= 0) {
                return true;
            }
        }

        if (attributes.contains(LwM2mAttributes.LESSER_THAN)) {
            hasNumericalAttributes = true;

            // Handle LESSER_THAN
            BigDecimal lessThan = attributes.get(LwM2mAttributes.LESSER_THAN).getValue();
            BigInteger lessThanRounded = lessThan.setScale(0, RoundingMode.CEILING).toBigIntegerExact();
            if (lastSentULong.compareTo(lessThanRounded) >= 0 && newULong.compareTo(lessThanRounded) < 0) {
                return true;
            }
        }

        if (attributes.contains(LwM2mAttributes.GREATER_THAN)) {
            hasNumericalAttributes = true;

            // Handle GREATER_THAN
            BigDecimal greaterThan = attributes.get(LwM2mAttributes.GREATER_THAN).getValue();
            BigInteger greaterThanRounded = greaterThan.setScale(0, RoundingMode.FLOOR).toBigIntegerExact();
            if (lastSentULong.compareTo(greaterThanRounded) <= 0 && newULong.compareTo(greaterThanRounded) > 0) {
                return true;
            }
        }

        // if we have numerical attribute we can send notification else if one condition matches we already return true;
        return !hasNumericalAttributes;
    }
}

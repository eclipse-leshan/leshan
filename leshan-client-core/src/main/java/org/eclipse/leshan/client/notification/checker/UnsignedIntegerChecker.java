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
import org.eclipse.leshan.core.util.datatype.ULong;

public class UnsignedIntegerChecker implements CriteriaBasedOnValueChecker {

    @Override
    public boolean shouldTriggerNotificationBasedOnValueChange(LwM2mAttributeSet attributes, Object lastSentValue,
            Object newValue) {
        BigInteger lastSentULong = ((ULong) lastSentValue).toBigInteger();
        BigInteger newULong = ((ULong) newValue).toBigInteger();

        if (attributes.contains(LwM2mAttributes.STEP)) {
            // Handle Step
            BigInteger step = new BigDecimal(attributes.get(LwM2mAttributes.STEP).getValue())
                    .setScale(0, RoundingMode.CEILING).toBigIntegerExact();
            return lastSentULong.subtract(newULong).abs().subtract(step).signum() >= 0;
        } else if (attributes.contains(LwM2mAttributes.LESSER_THAN)) {
            // Handle LESSER_THAN
            BigDecimal lessThan = new BigDecimal(attributes.get(LwM2mAttributes.LESSER_THAN).getValue());
            BigInteger lessThanRounded = lessThan.setScale(0, RoundingMode.CEILING).toBigIntegerExact();
            return lastSentULong.compareTo(lessThanRounded) >= 0 && newULong.compareTo(lessThanRounded) < 0;
        } else if (attributes.contains(LwM2mAttributes.GREATER_THAN)) {
            // Handle LESSER_THAN
            BigDecimal lessThan = new BigDecimal(attributes.get(LwM2mAttributes.GREATER_THAN).getValue());
            BigInteger lessThanRounded = lessThan.setScale(0, RoundingMode.FLOOR).toBigIntegerExact();
            return lastSentULong.compareTo(lessThanRounded) <= 0 && newULong.compareTo(lessThanRounded) > 0;
        }
        return true;
    }
}

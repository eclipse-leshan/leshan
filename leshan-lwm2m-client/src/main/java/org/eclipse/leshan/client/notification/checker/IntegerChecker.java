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

import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeSet;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributes;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;

/**
 * A {@link CriteriaBasedOnValueChecker} for {@link LwM2mResource} of {@link Type#INTEGER}
 */
public class IntegerChecker implements CriteriaBasedOnValueChecker {

    @Override
    public boolean shouldTriggerNotificationBasedOnValueChange(LwM2mAttributeSet attributes, Object lastSentValue,
            Object newValue) {
        BigDecimal lastSentLong = new BigDecimal((Long) lastSentValue);
        BigDecimal newLong = new BigDecimal((Long) newValue);
        boolean hasNumericalAttributes = false;

        if (attributes.contains(LwM2mAttributes.STEP)) {
            hasNumericalAttributes = true;

            // Handle Step
            BigDecimal step = attributes.get(LwM2mAttributes.STEP).getValue();
            if (lastSentLong.subtract(newLong).abs().subtract(step).signum() >= 0) {
                return true;
            }
        }

        if (attributes.contains(LwM2mAttributes.LESSER_THAN)) {
            hasNumericalAttributes = true;

            BigDecimal lessThan = attributes.get(LwM2mAttributes.LESSER_THAN).getValue();
            if (lastSentLong.compareTo(lessThan) >= 0 && newLong.compareTo(lessThan) < 0) {
                return true;
            }
        }

        if (attributes.contains(LwM2mAttributes.GREATER_THAN)) {
            hasNumericalAttributes = true;

            BigDecimal greaterThan = attributes.get(LwM2mAttributes.GREATER_THAN).getValue();
            if (lastSentLong.compareTo(greaterThan) <= 0 && newLong.compareTo(greaterThan) > 0) {
                return true;
            }
        }
        // if we have numerical attribute we can send notification else if one condition matches we already return true;
        return !hasNumericalAttributes;
    }
}

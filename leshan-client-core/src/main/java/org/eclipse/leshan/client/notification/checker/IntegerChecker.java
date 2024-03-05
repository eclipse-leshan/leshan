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
        Long lastSentLong = (Long) lastSentValue;
        Long newLong = (Long) newValue;
        boolean hasNumericalAttributes = false;

        if (attributes.contains(LwM2mAttributes.STEP)) {
            hasNumericalAttributes = true;

            if (Math.abs(lastSentLong - newLong) >= attributes.get(LwM2mAttributes.STEP).getValue()) {
                return true;
            }
        }

        if (attributes.contains(LwM2mAttributes.LESSER_THAN)) {
            hasNumericalAttributes = true;

            long lessThan = (long) Math.ceil(attributes.get(LwM2mAttributes.LESSER_THAN).getValue());
            if (lastSentLong >= lessThan && newLong < lessThan) {
                return true;
            }
        }

        if (attributes.contains(LwM2mAttributes.GREATER_THAN)) {
            hasNumericalAttributes = true;

            long greaterThan = (long) Math.floor(attributes.get(LwM2mAttributes.GREATER_THAN).getValue());
            if (lastSentLong <= greaterThan && newLong > greaterThan) {
                return true;
            }
        }
        // if we have numerical attribute we can send notification else if one condition matches we already return true;
        return !hasNumericalAttributes;
    }

}

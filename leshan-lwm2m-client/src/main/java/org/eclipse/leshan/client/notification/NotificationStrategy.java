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
package org.eclipse.leshan.client.notification;

import org.eclipse.leshan.core.link.lwm2m.attributes.InvalidAttributesException;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttributeModel;
import org.eclipse.leshan.core.link.lwm2m.attributes.NotificationAttributeTree;
import org.eclipse.leshan.core.node.LwM2mChildNode;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mPath;

/**
 * This class is responsible to implement how write attribute are assigned to given LWM2M node and how it should be
 * handle.
 * <p>
 * This aims to provide some flexibility because LWM2M specification isn't really clear on this topic.
 *
 * @see <a href="https://github.com/OpenMobileAlliance/OMA_LwM2M_for_Developers/issues/478"> Discussion about Write
 *      Attributes on OMA LwM2M for Developers Github repository </a>
 * @see DefaultNotificationStrategy
 */
public interface NotificationStrategy {

    /**
     * Create a {@link NotificationAttributeTree} when observe relation is initiated with information needed later.
     *
     * @param path The path of the LWM2M node to observe
     * @param attributes The whole {@link NotificationAttributeTree} currently attached to the object targeted by the
     *        given path.
     *
     * @throws InvalidAttributesException if current attributes configuration is inconsistent.
     */
    NotificationAttributeTree selectNotificationsAttributes(LwM2mPath path, NotificationAttributeTree attributes)
            throws InvalidAttributesException;

    /**
     * @param attributes The attributes returned by
     *        {@link #selectNotificationsAttributes(LwM2mPath, NotificationAttributeTree)}
     * @param path The path of the LWM2M node to observe
     * @param model The {@link LwM2mAttributeModel} for which we want to know if the given attribute is assigned
     */
    boolean hasAttribute(NotificationAttributeTree attributes, LwM2mPath path, LwM2mAttributeModel<?> model);

    /**
     * @param attributes The attributes returned by
     *        {@link #selectNotificationsAttributes(LwM2mPath, NotificationAttributeTree)}
     * @param path The path of the LWM2M node to observe
     * @param model he {@link LwM2mAttributeModel} for which we want to get the value
     */
    <T> T getAttributeValue(NotificationAttributeTree attributes, LwM2mPath path, LwM2mAttributeModel<T> model);

    /**
     * @param attributes The attributes returned by
     *        {@link #selectNotificationsAttributes(LwM2mPath, NotificationAttributeTree)}
     * @param path The path of the LWM2M node to observe
     * @return <code>True</code> some attributes assigned are based on value (e.g. gt, lt, st)
     */
    boolean hasCriteriaBasedOnValue(NotificationAttributeTree attributes, LwM2mPath path);

    /**
     * @param attributes The attributes returned by
     *        {@link #selectNotificationsAttributes(LwM2mPath, NotificationAttributeTree)}
     * @param path The path of the LWM2M node to observe
     * @param lastSentNode The last LwM2mNode sent in a notification for this observe relation.
     * @param newNode The a new value for which we should decide if a new notification should be sent following criteria
     *        based on value.
     * @return <code>True</code> if a new notification should be sent.
     */
    boolean shouldTriggerNotificationBasedOnValueChange(NotificationAttributeTree attributes, LwM2mPath path,
            LwM2mNode lastSentNode, LwM2mChildNode newNode);
}

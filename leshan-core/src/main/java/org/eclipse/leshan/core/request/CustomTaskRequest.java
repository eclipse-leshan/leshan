/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.request;

import org.eclipse.leshan.core.CustomTaskContainer;

/**
 * A Lightweight M2M request for retrieving the values of resources from a LWM2M Client.
 *
 * The request can be used to retrieve the value(s) of one or all attributes of one particular or all instances of a
 * particular object type.
 */
public class CustomTaskRequest {
     public static void handleCustomTask(String taskName) { checkRequest(taskName); }
     
     static void checkRequest(String taskName) { if (taskName.equals("NoAnswerRequest")) { requestUpdateNoAnswer(); }
     }

     static void requestUpdateNoAnswer() { CustomTaskContainer.getInstance().requestUpdateAnswer = false; }
}

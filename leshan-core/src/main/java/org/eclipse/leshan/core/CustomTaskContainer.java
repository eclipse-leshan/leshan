/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v2.0 and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html and the Eclipse Distribution
 * License is available at http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors: Sierra Wireless - initial API and implementation Achim Kraus (Bosch Software Innovations GmbH) - use
 * Identity as destination Rokwoon Kim (contracted with NTELS) - use registrationIdProvider
 *******************************************************************************/
package org.eclipse.leshan.core;

public class CustomTaskContainer {
    public boolean requestUpdateAnswer = true;

    private static CustomTaskContainer instance = null;

    private void Container() {
    }

    public static CustomTaskContainer getInstance() {
        if (instance == null) {
            instance = new CustomTaskContainer();
        }
        return instance;
    }
}

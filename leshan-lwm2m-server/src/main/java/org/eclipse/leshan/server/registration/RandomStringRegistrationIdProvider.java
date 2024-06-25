/*******************************************************************************
 * Copyright (c) 2018 NTELS and others.
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
 *     Rokwoon Kim (contracted with NTELS) - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.util.RandomStringUtils;

public class RandomStringRegistrationIdProvider implements RegistrationIdProvider {

    @Override
    public String getRegistrationId(RegisterRequest registerRequest) {
        return RandomStringUtils.random(10, true, true);
    }

}

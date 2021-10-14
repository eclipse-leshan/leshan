/*******************************************************************************
 * Copyright (c) 2021 Bosch.IO GmbH and others.
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
 *    Bosch IO.GmbH - initial creation
 ******************************************************************************/
package org.eclipse.leshan.core.californium.config;

import org.eclipse.californium.elements.config.EnumDefinition;
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole;

/**
 * LwM2M specific configuration definitions.
 */
public final class Lwm2mConfig {

    public static final EnumDefinition<DtlsRole> LWM2M_DTLS_ROLE = new EnumDefinition<>("LWM2M_DTLS_ROLE",
            "DTLS role for LwM2M. For details, especially about the smart default, please refer to Leshan documentation.",
            null, DtlsRole.values());

}

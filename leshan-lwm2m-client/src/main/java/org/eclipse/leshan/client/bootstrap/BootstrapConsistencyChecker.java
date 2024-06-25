/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.client.bootstrap;

import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;

/**
 * This class is responsible to check the consistency state of the client after at the end of a bootstrap session.
 */
public interface BootstrapConsistencyChecker {

    /**
     * check if current state of the client is consistent
     *
     * @param objectEnablers all objects supported by the client.
     * @return <code>null</code> if the current state is consistent or a list of issues
     */
    List<String> checkconfig(Map<Integer, LwM2mObjectEnabler> objectEnablers);

}

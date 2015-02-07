/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Zebra Technologies - initial API and implementation
 *******************************************************************************/
package CaliforniumClientIdentifier;

import static org.junit.Assert.*;

import org.eclipse.leshan.client.californium.impl.CaliforniumClientIdentifier;
import org.junit.Test;

public class CaliforniumClientIdentifierTest {

    @Test
    public void two_instances_are_equal() {
        String oneLocation = "/rd/something";
        String oneEndpoint = "dfasdfs";
        CaliforniumClientIdentifier idOne = new CaliforniumClientIdentifier(oneLocation, oneEndpoint);
        CaliforniumClientIdentifier idTwo = new CaliforniumClientIdentifier(oneLocation, oneEndpoint);

        assertEquals(idOne, idTwo);
        assertEquals(idOne.hashCode(), idTwo.hashCode());
    }

}

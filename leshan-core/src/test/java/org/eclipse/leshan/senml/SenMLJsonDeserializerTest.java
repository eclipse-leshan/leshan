/*******************************************************************************
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
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.senml;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SenMLJsonDeserializerTest extends AbstractSenMLTest {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Test
    public void deserialize_device_object() {
        String dataString = givenSenMLJsonExample();
        log.debug(dataString.trim());
        SenMLPack pack = SenMLJson.fromSenMLJson(dataString);
        log.debug(pack.toString());

        String outString = SenMLJson.toSenMLJson(pack);
        Assert.assertEquals(dataString.trim(), outString);
    }
}

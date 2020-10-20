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

import org.eclipse.leshan.senml.json.minimaljson.SenMLJsonMinimalEncoderDecoder;
import org.junit.Assert;
import org.junit.Test;

public class SenMLJsonDeserializerTest extends AbstractSenMLTest {

    private SenMLEncoder encoder;
    private SenMLDecoder decoder;

    public SenMLJsonDeserializerTest() {
        encoder = new SenMLJsonMinimalEncoderDecoder();
    }

    @Test
    public void deserialize_device_object_with_minimalJson() throws SenMLException {
        String dataString = givenSenMLJsonExample();
        SenMLPack pack = decoder.fromSenML(dataString.getBytes());

        String outString = new String(encoder.toSenML(pack));
        Assert.assertEquals(dataString.trim(), outString);
    }

}

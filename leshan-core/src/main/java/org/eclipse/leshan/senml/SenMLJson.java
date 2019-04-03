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

import com.eclipsesource.json.Json;

/**
 * Helper for encoding/decoding SenML JSON format
 */
public class SenMLJson {
    private static final SenMLPackSerDes serDes = new SenMLPackSerDes();

    public static String toJsonSenML(SenMLPack pack) {
        return serDes.serialize(pack);
    }

    public static SenMLPack fromJsonSenML(String jsonString) {
        return serDes.deserialize(Json.parse(jsonString).asArray());
    }
}

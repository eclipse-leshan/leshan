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

package org.eclipse.leshan.senml.json.minimaljson;

import org.eclipse.leshan.senml.SenMLPack;

import com.eclipsesource.json.Json;

/**
 * Helper for encoding/decoding SenML JSON format
 */
public class SenMLJson {
    private static final SenMLJsonPackSerDes serDes = new SenMLJsonPackSerDes();

    public static String toSenMLJson(SenMLPack pack) {
        return serDes.serializeToJson(pack);
    }

    public static SenMLPack fromSenMLJson(String jsonString) {
        return serDes.deserializeFromJson(Json.parse(jsonString).asArray());
    }
}

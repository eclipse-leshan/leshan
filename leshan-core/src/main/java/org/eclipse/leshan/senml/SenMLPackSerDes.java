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

import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.util.Hex;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class SenMLPackSerDes {

    public String serialize(SenMLPack pack) {
        JsonArray jsonArray = new JsonArray();

        for (int i = 0; i < pack.getRecords().size(); i++) {
            JsonObject jsonObj = new JsonObject();

            if (i == 0 && pack.getBaseName() != null) {
                jsonObj.add("bn", pack.getBaseName());
            }
            if (i == 0 && pack.getBaseTime() != null) {
                jsonObj.add("bt", pack.getBaseTime());
            }

            SenMLRecord records = pack.getRecords().get(i);
            if (records.getName() != null && records.getName().length() > 0) {
                jsonObj.add("n", records.getName());
            }

            if (records.getTime() != null) {
                jsonObj.add("t", records.getTime());
            }

            Type type = records.getType();
            if (type != null) {
                switch (records.getType()) {
                case FLOAT:
                case INTEGER:
                    jsonObj.add("v", records.getFloatValue().floatValue());
                    break;
                case BOOLEAN:
                    jsonObj.add("vb", records.getBooleanValue());
                    break;
                case OBJLNK:
                    jsonObj.add("vd", records.getObjectLinkValue());
                    break;
                case OPAQUE:
                    jsonObj.add("vd", Hex.encodeHexString(records.getOpaqueValue()));
                case STRING:
                    jsonObj.add("vs", records.getStringValue());
                    break;
                case TIME:
                    jsonObj.add("v", records.getTimeValue());
                default:
                    break;
                }
            }

            jsonArray.add(jsonObj);
        }

        return jsonArray.toString();
    }
    
}

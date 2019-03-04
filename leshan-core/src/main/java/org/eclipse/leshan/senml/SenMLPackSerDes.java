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
import org.eclipse.leshan.util.Base64;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

public class SenMLPackSerDes {

    public String serialize(SenMLPack pack) {
        JsonArray jsonArray = new JsonArray();

        for (SenMLRecord record : pack.getRecords()) {
            JsonObject jsonObj = new JsonObject();

            if (record.getBaseName() != null && record.getBaseName().length() > 0) {
                jsonObj.add("bn", record.getBaseName());
            }

            if (record.getBaseTime() != null) {
                jsonObj.add("bt", record.getBaseTime());
            }

            if (record.getName() != null && record.getName().length() > 0) {
                jsonObj.add("n", record.getName());
            }

            if (record.getTime() != null) {
                jsonObj.add("t", record.getTime());
            }

            Type type = record.getType();
            if (type != null) {
                switch (record.getType()) {
                case FLOAT:
                    jsonObj.add("v", record.getFloatValue().floatValue());
                    break;
                case BOOLEAN:
                    jsonObj.add("vb", record.getBooleanValue());
                    break;
                case OBJLNK:
                    jsonObj.add("vlo", record.getObjectLinkValue());
                    break;
                case OPAQUE:
                    jsonObj.add("vd", Base64.encodeBase64String(record.getOpaqueValue()));
                case STRING:
                    jsonObj.add("vs", record.getStringValue());
                    break;
                default:
                    break;
                }
            }

            jsonArray.add(jsonObj);
        }

        return jsonArray.toString();
    }
}

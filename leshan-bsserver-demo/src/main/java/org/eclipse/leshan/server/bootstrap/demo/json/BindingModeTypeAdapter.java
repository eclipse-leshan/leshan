/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.server.bootstrap.demo.json;

import java.io.IOException;
import java.util.EnumSet;

import org.eclipse.leshan.core.request.BindingMode;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class BindingModeTypeAdapter extends TypeAdapter<EnumSet<BindingMode>> {

    @Override
    public void write(JsonWriter out, EnumSet<BindingMode> value) throws IOException {
        out.value(BindingMode.toString(value));
    }

    @Override
    public EnumSet<BindingMode> read(JsonReader in) throws IOException {
        return BindingMode.parse(in.nextString());
    }
}

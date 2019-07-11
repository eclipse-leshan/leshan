/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
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
 *     Rikard Höglund (RISE SICS) - Additions to support OSCORE
 *******************************************************************************/
package org.eclipse.leshan.server;

import org.eclipse.californium.oscore.HashMapCtxDB;

// TODO OSCORE : remove this class and static access.
public class OscoreHandler {

    private static HashMapCtxDB db;

    public static HashMapCtxDB getContextDB() {
        if (db == null) {
            db = new HashMapCtxDB();
        }
        return db;
    }
}
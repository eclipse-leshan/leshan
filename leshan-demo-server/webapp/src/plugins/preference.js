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
 *******************************************************************************/
import { reactive } from 'vue'
import { useLocalStorage } from "@vueuse/core";

const _preferences = reactive({
    timeout: useLocalStorage("timeout", 5),
    singleFormat: useLocalStorage("singleFormat", "TEXT"),
    multiFormat: useLocalStorage("multiFormat", "TLV"),
    compositePathFormat: useLocalStorage("compositePathFormat", "SENML_CBOR"),
    compositeNodeFormat: useLocalStorage("CompositeNodeFormat", "SENML_CBOR"),
    compositeObjects: useLocalStorage("compositeObjects", [
        { name: "myCompositeObject", paths: ["/3/0/1", "/3/0/2"] },
    ]),
})

export default {
    install: (app) => {
        app.config.globalProperties.$pref = _preferences;
    }
}
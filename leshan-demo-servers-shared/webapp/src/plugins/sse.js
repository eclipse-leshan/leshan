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

import VueSSE from "vue-sse";

export default {
  install(app, config = {}) {
    const leshanDefaultConfig = {
      format: 'json',
    };

    // Merge both config
    const finalConfig = { ...leshanDefaultConfig, ...config };

    // Install plugin with given config
    app.use(VueSSE, finalConfig);
  }
};
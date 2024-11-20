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

// import Vue from "vue";
// import "@leshan-demo-servers-shared/plugins/axios";
// import "./plugins/icons";
// import "@leshan-demo-servers-shared/plugins/dialog";
// import "@leshan-demo-servers-shared/plugins/dayjs";
// import "@leshan-demo-servers-shared//plugins/sse";
// import "@leshan-demo-servers-shared/plugins/preferences";
// import App from "./App.vue";
// import vuetify from "@leshan-demo-servers-shared/plugins/vuetify";
// import router from "./router";

// Vue.config.productionTip = false;

// new Vue({
//   vuetify,
//   router,
//   render: (h) => h(App),
// }).$mount("#app");


// Plugins
import { registerPlugins } from '@/plugins'

// Components
import App from './App.vue'

// Composables
import { createApp } from 'vue'

const app = createApp(App)

registerPlugins(app)

app.mount('#app')

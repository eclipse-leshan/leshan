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

import Vue from "vue";
import "./plugins/axios";
import "./plugins/store";
import "./plugins/icons";
import "./plugins/sse";
import "./plugins/moment";
import "./plugins/preferences";
import "./plugins/dialog";
import App from "./App.vue";
import vuetify from "./plugins/vuetify";
import router from "./router";

Vue.config.productionTip = false;

/**
 * directive to hide content without changing layout unlike v-show or v-if
 */
Vue.directive("visible", function (el, binding) {
  el.style.visibility = binding.value ? "visible" : "hidden";
});

new Vue({
  vuetify,
  router,
  render: (h) => h(App),
}).$mount("#app");

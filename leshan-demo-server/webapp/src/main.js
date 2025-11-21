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
// import "./plugins/store";
// import "./plugins/icons";
// import "@leshan-demo-servers-shared/plugins/sse";
// import "@leshan-demo-servers-shared/plugins/dayjs";
// import "@leshan-demo-servers-shared/plugins/preferences";
// import "@leshan-demo-servers-shared/plugins/dialog";
// import App from "./App.vue";
// import vuetify from "@leshan-demo-servers-shared/plugins/vuetify";
// import router from "./router";

// Vue.config.productionTip = false;



// let v = new Vue({
//   vuetify,
//   router,
//   render: (h) => h(App),
// }).$mount("#app");

// /** Add Leshan Server Demo specific axios interceptor */
// v.$axios.interceptors.response.use(function (response) {
//   if (response.data.delayed) {
//     // show request will be delayed
//     let msg = `<strong>Device is not awake</strong>
//          </br>Request will be delayed until device is awake again.
//          </br><strong>Leshan Server Demo</strong> is only able to delayed the last request.`;

//     Vue.prototype.$dialog.notify.info(msg, {
//       position: "bottom-right",
//       timeout: 5000,
//     });
//   }
//   return response;
// });


// Plugins
import { registerPlugins } from '@/plugins'

// Components
import App from './App.vue'

// Composables
import { createApp } from 'vue'

const app = createApp(App)

registerPlugins(app)

// register directive
app.directive('visible', (el, binding) => {
    // directive to hide content without changing layout unlike v-show or v-if
    el.style.visibility = binding.value ? "visible" : "hidden";
})

/** Add Leshan Server Demo specific axios interceptor */
app.config.globalProperties.axios.interceptors.response.use(function (response) {
  console.log(response)
  if (response.data.delayed) {
    // show request will be delayed
    let msg = `<strong>Device is not awake</strong>
         </br>Request will be delayed until device is awake again.
         </br><strong>Leshan Server Demo</strong> is only able to delayed the last request.`;

    app.config.globalProperties.$notify.create({
      htmlContent: msg,
      location: "bottom right",
      timeout: 5000,
    });
  }
  return response;
});

app.mount('#app')

import Vue from 'vue'
import './plugins/axios'
import './plugins/dialog'
import './plugins/moment'
import './plugins/sse'
import './plugins/preferences'
import App from './App.vue'
import vuetify from './plugins/vuetify';
import router from './router'

Vue.config.productionTip = false

new Vue({
  vuetify,
  router,
  render: h => h(App)
}).$mount('#app')

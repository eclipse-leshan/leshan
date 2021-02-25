import Vue from 'vue'
import './plugins/axios'
import './plugins/sse'
import './plugins/moment'
import './plugins/preferences'
import './plugins/dialog'
import App from './App.vue'
import vuetify from './plugins/vuetify';
import router from './router'

Vue.config.productionTip = false

new Vue({
  vuetify,
  router,
  render: h => h(App)
}).$mount('#app')

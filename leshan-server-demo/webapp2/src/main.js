import Vue from 'vue'
import './plugins/axios'
import './plugins/store'
import './plugins/sse'
import './plugins/moment'
import './plugins/preferences'
import './plugins/dialog'
import App from './App.vue'
import vuetify from './plugins/vuetify';
import router from './router'

Vue.config.productionTip = false

/**
 * directive to hide content without changing layout unlike v-show or v-if
 */
Vue.directive('visible', function(el, binding) {
  el.style.visibility = binding.value ? 'visible' : 'hidden';
});


new Vue({
  vuetify,
  router,
  render: h => h(App)
}).$mount('#app')



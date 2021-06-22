import Vue from "vue";
import VueRouter from "vue-router";
import Bootstrap from "../views/Bootstrap.vue";
import About from "../views/About.vue";
import Client from "../views/Client.vue"

Vue.use(VueRouter);

const routes = [
  {
    path: "/",
    redirect: "/bootstrap",
  },
  {
    path: "/bootstrap",
    name: "Bootstrap",
    component: Bootstrap,
  },
  {
    path: "/bootstrap/:endpoint",
    component: Client,
  },
  {
    path: "/about",
    name: "About",
    component:About,
  }
];

const router = new VueRouter({
  routes,
});

export default router;

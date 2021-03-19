import Vue from "vue";
import VueRouter from "vue-router";
import Clients from "../views/Clients.vue";
import Client from "../views/Client.vue";
import ObjectView from "../views/ObjectView.vue";
import Security from "../views/Security.vue";
import About from "../views/About.vue";

Vue.use(VueRouter);

const routes = [
  {
    path: "/",
    redirect: "/clients",
  },
  {
    path: "/clients",
    name: "Clients",
    component: Clients,
  },
  {
    path: "/clients/:endpoint",
    component: Client,
    children: [
      {
        path: ":objectid",
        component: ObjectView,
      },
    ],
  },
  {
    path: "/security",
    name: "Security",
    component: Security,
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

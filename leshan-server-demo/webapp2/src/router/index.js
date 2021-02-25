import Vue from "vue";
import VueRouter from "vue-router";
import Clients from "../views/Clients.vue";
import Client from "../views/Client.vue";
import ObjectView from "../views/ObjectView.vue";

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
    path: "/about",
    name: "About",
    // route level code-splitting
    // this generates a separate chunk (about.[hash].js) for this route
    // which is lazy-loaded when the route is visited.
    component: () =>
      import(/* webpackChunkName: "about" */ "../views/About.vue"),
  },
];

const router = new VueRouter({
  routes,
});

export default router;

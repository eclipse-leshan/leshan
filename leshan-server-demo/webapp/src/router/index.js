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
import VueRouter from "vue-router";

Vue.use(VueRouter);

function lazyLoad(view) {
  return () => import(`@/views/${view}.vue`);
}

function lazyLoadFromServerCoreDemo(view) {
  return () => import(`@leshan-server-core-demo/views/${view}.vue`);
}

const routes = [
  {
    path: "/",
    redirect: "/clients",
  },
  {
    path: "/clients/:endpoint",
    component: lazyLoad("Client"),
    children: [
      {
        path: "composite",
        component: lazyLoad("CompositeOperationView"),
        children: [
          {
            path: ":compositeObjectName",
            component: lazyLoad("CompositeObjectView"),
          },
        ],
      },
      {
        path: ":objectid",
        component: lazyLoad("ObjectView"),
      },
    ],
  },
  {
    path: "/clients",
    name: "Clients",
    component: lazyLoad("Clients"),
  },
  {
    path: "/security",
    name: "Security",
    component: lazyLoad("Security"),
  },
  {
    path: "/server",
    name: "Server",
    component: lazyLoadFromServerCoreDemo("Server"),
    props: {
      pubkeyFileName: "serverPubKey.der",
      certFileName: "serverCertificate.der",
    },
  },
  {
    path: "/about",
    name: "About",
    component: lazyLoad("About"),
  },
];

const router = new VueRouter({
  routes,
});

export default router;

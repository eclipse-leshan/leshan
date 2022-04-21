<!-----------------------------------------------------------------------------
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
  ----------------------------------------------------------------------------->
<template>
  <v-row class="fill-height" no-gutters>
    <v-col cols="12" v-if="deregister">
      <div style="height: 100px"></div>
      <v-card elevation="0" class="text-center">
        <v-card-text class="fill-height">
          <v-icon x-large> {{ $icons.mdiExitRun }}</v-icon>
          <v-card-title class="justify-center">
            {{ $route.params.endpoint }} is deregistered.
          </v-card-title>
          <v-card-subtitle> Waiting for new registration ? </v-card-subtitle>
        </v-card-text>
      </v-card>
    </v-col>
    <v-col
      cols="12"
      md="2"
      v-if="registration"
      style="background-color: #fafafa"
    >
      <!-- registration info -->
      <v-sheet color="grey lighten-5" class="pa-4" width="100%">
        <h3>
          {{ $route.params.endpoint }}
          <client-info :registration="registration" small tooltipbottom />
        </h3>
        <div>
          <div>Reg. ID: {{ registration.registrationId }}</div>
          <div>
            Registered:
            {{ registration.registrationDate | moment("MMM D, h:mm:ss A") }}
          </div>
          <div>
            Updated:
            {{ registration.lastUpdate | moment("MMM D, h:mm:ss A") }}
          </div>
        </div>
        <v-divider />
        <client-setting />
      </v-sheet>

      <v-divider />

      <!-- Composite Operations-->
      <span>
        <v-list dense v-if="registration && registration.lwM2mVersion != '1.0'">
          <v-list-item-group>
            <v-list-item
              :to="'/clients/' + $route.params.endpoint + '/composite'"
            >
              <v-list-item-icon class="me-2">
                <v-icon>{{ $icons.mdiFormatListCheckbox }}</v-icon>
              </v-list-item-icon>
              <v-list-item-content>
                <v-list-item-title>Composite Operations</v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </v-list-item-group>
        </v-list>
        <v-divider />
      </span>

      <!-- object selector -->
      <object-selector :objects="objectdefs" v-show="objectdefs" />
      <v-divider></v-divider>
    </v-col>
    <v-col no-gutters cols="12" md="10">
      <!-- object viewer -->
      <div v-if="$route.params.objectid">
        <!-- objectView case -->
        <router-view
          v-if="objectdefs"
          :objectdef="objectdef"
          :instances="instances"
        ></router-view>
      </div>
      <div v-else>
        <!-- compositeOperationView case -->
        <router-view
          v-if="objectdefs && registration"
          :objectdefs="objectdefs"
          :allInstances="registration.availableInstances"
        ></router-view>
      </div>
    </v-col>
  </v-row>
</template>

<script>
import ClientInfo from "../components/ClientInfo.vue";
import ClientSetting from "../components/ClientSetting.vue";
import ObjectSelector from "../components/object/ObjectSelector.vue";

// get models for this endpoint
export default {
  components: { ObjectSelector, ClientSetting, ClientInfo },
  name: "Client",
  data: () => ({
    deregister: false,
    registration: null,
    objectdefs: [],
  }),
  methods: {
    updateModels: function () {
      this.axios
        .get(
          "api/objectspecs/" + encodeURIComponent(this.$route.params.endpoint)
        )
        .then((response) => (this.objectdefs = Object.freeze(response.data)));
    },
  },
  computed: {
    objectdef: function () {
      return this.objectdefs.find((o) => o.id == this.$route.params.objectid);
    },
    instances: function () {
      if (this.registration) {
        let instances =
          this.registration.availableInstances[this.$route.params.objectid];
        if (instances) return instances;
      }
      return [];
    },
  },
  watch: {
    // TODO handle URL changes.
    /*$route(to, from) {
      console.log("route", to, from);
    },*/
    registration(newReg, oldReg) {
      // reinit state for this endpoint
      if (newReg) {
        if (!oldReg) {
          this.$store.initState(newReg.endpoint);
        } else if (oldReg.endpoint !== newReg.endpoint) {
          this.$store.initState(newReg.endpoint);
        }
      }
    },
  },
  mounted() {
    this.sse = this.$sse
      .create({
        url: "api/event?ep=" + encodeURIComponent(this.$route.params.endpoint),
      })
      .on("REGISTRATION", (reg) => {
        this.registration = reg;
        this.deregister = false;
        this.updateModels();
      })
      .on("UPDATED", (msg) => {
        let previousReg = this.registration;
        this.registration = msg.registration;
        if (
          JSON.stringify(this.registration.objectLinks) !==
          JSON.stringify(previousReg.objectLinks)
        ) {
          this.updateModels();
        }
      })
      .on("DEREGISTRATION", () => {
        this.registration = null;
        this.objectdefs = null;
        this.deregister = true;
      })
      .on("SLEEPING", () => {
        this.registration.sleeping = true;
      })
      .on("SEND", (msg) => {
        for (let path in msg.val) {
          this.$store.newNode(this.$route.params.endpoint, path, msg.val[path]);
        }
      })
      .on("NOTIFICATION", (msg) => {
        if (msg.kind == "composite") {
          this.$store.newNodes(this.$route.params.endpoint, msg.val);
          this.$store.setCompositePathsObserved(
            this.$route.params.endpoint,
            msg.paths,
            true
          );
        } else if (msg.kind == "single") {
          if (msg.val.kind === "instance") {
            this.$store.newInstanceValue(
              this.$route.params.endpoint,
              msg.res,
              msg.val.resources,
              false
            );
          } else if (msg.val.kind === "singleResource") {
            this.$store.newSingleResourceValue(
              this.$route.params.endpoint,
              msg.res,
              msg.val.value,
              false
            );
          } else if (msg.val.kind === "multiResource") {
            this.$store.newMultiResourceValue(
              this.$route.params.endpoint,
              msg.res,
              msg.val.values,
              false
            );
          } else if (msg.val.kind === "resourceInstance") {
            this.$store.newResourceInstanceValueFromPath(
              this.$route.params.endpoint,
              msg.res,
              msg.val.value,
              false
            );
          }
          this.$store.setObserved(this.$route.params.endpoint, msg.res, true);
        }
      })
      .on("error", (err) => {
        console.error("sse unexpected error", err);
      });

    this.sse.connect().catch((err) => {
      console.error("Failed to connect to server", err);
    });

    // get registration
    this.axios
      .get("api/clients/" + encodeURIComponent(this.$route.params.endpoint))
      .then((response) => {
        (this.registration = response.data), // get models for this endpoint
          this.updateModels();
      })
      .catch(() => {
        this.registration = null;
        this.deregister = true;
      });
  },
  beforeDestroy() {
    this.sse.disconnect();
  },
};
</script>

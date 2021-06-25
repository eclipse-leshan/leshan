<template>
  <v-row class="fill-height" no-gutters>
    <v-col cols="12" v-if="deregister">
      <div style="height: 100px;"></div>
      <v-card elevation="0" class="text-center">
        <v-card-text class="fill-height">
          <v-icon x-large> mdi-exit-run</v-icon>
          <v-card-title class="justify-center">
            {{ $route.params.endpoint }} is deregistered.
          </v-card-title>
          <v-card-subtitle>
            Waiting for new registration ?
          </v-card-subtitle>
        </v-card-text>
      </v-card>
    </v-col>
    <v-col
      cols="12"
      md="2"
      v-if="registration"
      style="background-color:#fafafa"
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

      <!-- object selector -->
      <object-selector :objects="objectdefs" v-show="objectdefs" />
      <v-divider></v-divider>
    </v-col>
    <v-col no-gutters cols="12" md="10">
      <!-- object viewer -->
      <router-view
        v-if="objectdefs"
        :objectdef="objectdefs.find((o) => o.id == $route.params.objectid)"
        :instances="instances"
      ></router-view>
    </v-col>
  </v-row>
</template>

<script>
import ClientInfo from "../components/ClientInfo.vue";
import ClientSetting from "../components/ClientSetting.vue";
import ObjectSelector from "../components/ObjectSelector.vue";

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
    updateModels: function() {
      this.axios
        .get(
          "api/objectspecs/" + encodeURIComponent(this.$route.params.endpoint)
        )
        .then((response) => (this.objectdefs = Object.freeze(response.data)));
    },
  },
  computed: {
    // a computed getter
    instances: function() {
      // should probably be returned by REST API
      // TODO we need to handle rootpath
      let instances = [];
      if (this.registration) {
        this.registration.objectLinks.forEach((o) => {
          let ids = o.url.split("/");
          if (ids.length === 3 && ids[1] == this.$route.params.objectid)
            instances.push({ id: ids[2] });
        });
      }
      return instances;
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
      .on("NOTIFICATION", (msg) => {
        if (msg.val.resources) {
          this.$store.newInstanceValue(
            this.$route.params.endpoint,
            msg.res,
            msg.val.resources,
            false
          );
        } else if (msg.val.value) {
          this.$store.newResourceValue(
            this.$route.params.endpoint,
            msg.res,
            msg.val.value,
            false
          );
        }
        this.$store.setObserved(this.$route.params.endpoint, msg.res, true);
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

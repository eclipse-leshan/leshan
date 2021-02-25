<template>
  <v-row class="fill-height" no-gutters>
    <v-col cols="12" md="2">
      <!-- registration info -->
      <v-sheet color="grey lighten-5" class="pa-4" width="100%">
        <div>
          <h3>Client {{ $route.params.endpoint }}</h3>
          <div v-if="registration">
            <div>Registered with {{ registration.registrationId }}</div>
            <div>Using LWM2M v{{ registration.lwM2mVersion }}</div>
            <div>
              Last Reg. Update at
              {{
                registration.lastUpdate
                  | moment("MMMM Do, h:mm:ss a")
              }}
            </div>
          </div>
        </div>
        <v-divider />
        <client-setting />  
      </v-sheet>

      <v-divider />

      <!-- object selector -->
      <object-selector :objects="objects" />

      <v-divider></v-divider>
    </v-col>

    <v-col no-gutters cols="12" md="10">
      <!-- object viewer -->
      <router-view
        :object="objects.find((o) => o.id == $route.params.objectid)"
        :instances="instances"
      ></router-view>
    </v-col>
  </v-row>
</template>

<script>
import ClientSetting from "../components/ClientSetting.vue";
import ObjectSelector from "../components/ObjectSelector.vue";

// get models for this endpoint
export default {
  components: { ObjectSelector, ClientSetting },
  name: "Client",
  data: () => ({
    registration: null,
    objects: [],
  }),
  methods: {
    updateModels: function() {
      this.axios
        .get(
          "api/objectspecs/" + encodeURIComponent(this.$route.params.endpoint)
        )
        .then((response) => (this.objects = response.data))
        .catch((error) => console.log(error));
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
    /*$route(to, from) {
      console.log("route", to, from);
    },*/
  },
  mounted() {
    this.$sse(
      "api/event?ep=" + encodeURIComponent(this.$route.params.endpoint),
      {
        format: "json",
      }
    )
      .then((sse) => {
        this.sse = sse; // store sse to close in on Destroy

        sse.onError((e) => {
          console.error("lost connection; giving up!", e);
        });

        sse.subscribe("REGISTRATION", (reg) => {
          this.registration = reg;
          this.updateModels();
        });
        sse.subscribe("UPDATED", (msg) => {
          let previousReg = this.registration;
          this.registration = msg.registration;
          if (
            JSON.stringify(this.registration.objectLinks) !==
            JSON.stringify(previousReg.objectLinks)
          ) {
            this.updateModels();
          }
        });
        sse.subscribe("DEREGISTRATION", () => {
          this.registration = null;
        });
      })
      .catch((err) => {
        console.error("Failed to connect to server", err);
      });

    // get registration
    this.axios
      .get("api/clients/" + encodeURIComponent(this.$route.params.endpoint))
      .then((response) => (this.registration = response.data))
      .catch((error) => console.log(error));

    // get models for this endpoint
    this.updateModels();
  },
  beforeDestroy() {
    this.sse.close();
  },
};
</script>

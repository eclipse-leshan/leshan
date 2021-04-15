<template>
  <v-dialog
    v-model="show"
    hide-overlay
    fullscreen
    transition="dialog-bottom-transition"
  >
    <v-card>
      <!-- Title -->
      <v-card-title class="headline grey lighten-2">
        <span>Add Client Configuration</span>
      </v-card-title>

      <!-- Form -->
      <v-stepper v-model="currentStep">
        <v-stepper-header>
          <v-stepper-step
            :complete="currentStep > 1"
            step="1"
            @click="currentStep = 1"
          >
            Endpoint Name
          </v-stepper-step>
          <v-divider></v-divider>
          <v-stepper-step
            :complete="currentStep > 2"
            step="2"
            @click="currentStep = 2"
          >
            LWM2M Server Configuration
          </v-stepper-step>
          <v-divider></v-divider>
          <v-stepper-step
            :complete="currentStep > 3"
            step="3"
            @click="currentStep = 3"
          >
            LWM2M Bootstrap Server Configuration
          </v-stepper-step>
        </v-stepper-header>

        <v-stepper-items>
          <v-stepper-content step="1">
            <v-card class="mb-12" elevation="0">
              <v-card-text class="pb-0">
                <p>
                  To allow a client to bootstrap to this server you need to
                  create a configuration for it.
                </p>
                <p>
                  This wizard is pretty limitted. It create a configuration
                  which starts by deleting objects <code>/0</code> and
                  <code>/1</code>, then write instance for those objects for 1
                  LWM2M server and 1 LWM2M BootstrapServer with provided data.
                </p>
                <p>
                  How the client is supposed to connect to this bootstrap server
                  <a
                    href="https://github.com/eclipse/leshan/issues/690#issuecomment-490949978"
                    target="_blank"
                    >is guessed</a
                  >
                  from the
                  <strong>LWM2M Bootstrap Server Configuration</strong>.
                </p>
              </v-card-text>
              <v-form ref="form1" v-model="valid[1]">
                <v-text-field
                  v-model="config.endpoint"
                  :rules="[(v) => !!v || 'Endpoint is required']"
                  label="Your Client Endpoint Name"
                  required
                  autofocus
                ></v-text-field>
              </v-form>
            </v-card>
          </v-stepper-content>

          <v-stepper-content step="2">
            <v-card class="mb-12" elevation="0">
              <v-card-text class="pb-0">
                <p>
                  This information will be used to add a
                  <strong>LWM2M Server</strong> to your LWM2M Client during the
                  bootstrap Session by writing 1 instance for objects
                  <code>/0</code> and <code>/1</code>.
                </p>
              </v-card-text>
              <v-form ref="form2" v-model="valid[2]">
                <server-input
                  v-model="config.dm"
                  :defaultNoSecValue="defval.dm.url.nosec"
                  :defaultSecureValue="defval.dm.url.sec"
                />
              </v-form>
            </v-card>
          </v-stepper-content>
          <v-stepper-content step="3">
            <v-card class="mb-12" elevation="0">
              <v-card-text class="pb-0">
                <p>
                  This information will be used to add a
                  <strong>LWM2M Bootstrap Server</strong> to your LWM2M Client
                  during the bootstrap Session by writing an instance for object
                  <code>/0</code>.
                </p>
                <p>
                  This data will also be used
                  <a
                    href="https://github.com/eclipse/leshan/issues/690#issuecomment-490949978"
                    target="_blank"
                    >to know how the client must connect to this server</a
                  >.
                </p>
              </v-card-text>
              <v-form ref="form3" v-model="valid[3]">
                <server-input
                  v-model="config.bs"
                  :defaultNoSecValue="defval.bs.url.nosec"
                  :defaultSecureValue="defval.bs.url.sec"
                />
              </v-form>
            </v-card>
          </v-stepper-content>
        </v-stepper-items>
      </v-stepper>

      <!-- Buttons -->
      <v-card-actions>
        <v-spacer></v-spacer>
        <v-btn
          elevation="0"
          @click="currentStep = currentStep + 1"
          :disabled="!valid[currentStep] || currentStep == nbSteps"
        >
          Next
        </v-btn>
        <v-btn
          elevation="0"
          @click="$emit('add', applyDefault(config))"
          :disabled="!valid[currentStep]"
        >
          Add
        </v-btn>
        <v-btn
          text
          @click="currentStep = currentStep - 1"
          :disabled="currentStep == 1"
        >
          Previous
        </v-btn>
        <v-btn text @click="close">
          Cancel
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script>
import ServerInput from "./bsconfig/ServerInput.vue";

export default {
  components: { ServerInput },
  props: { value: Boolean /*open/close dialog*/ },
  data() {
    return {
      nbSteps: 3,
      config: {}, // local state for current config
      valid: [],
      currentStep: 1,
      defval: {
        dm: { url: {} },
        bs: { url: {} },
      },
    };
  },
  computed: {
    show: {
      get() {
        return this.value;
      },
      set(value) {
        this.$emit("input", value);
      },
    },
  },
  beforeMount() {
    this.axios
      .get("api/server/endpoint")
      .then((response) => {
        this.defval.dm.url.nosec = `coap://${location.hostname}:5683`;
        this.defval.dm.url.sec = `coaps://${location.hostname}:5684`;
        this.defval.bs.url.nosec = `coap://${location.hostname}:${response.data.unsecuredEndpointPort}`;
        this.defval.bs.url.sec = `coaps://${location.hostname}:${response.data.securedEndpointPort}`;
      })
      .catch((error) => console.log(error));
  },
  watch: {
    value(v) {
      if (v) {
        // reset validation and set initial value when dialog opens
        this.config = {
          endpoint: null,
          dm: { mode: "no_sec" },
          bs: { mode: "no_sec" },
        };
        this.currentStep = 1;
        for (let i = 1; i <= this.nbSteps; i++) {
          if (this.$refs["form" + i]) this.$refs["form" + i].resetValidation();
          this.valid[i] = false;
        }
      }
    },
  },
  methods: {
    applyDefault(c) {
      // do a deep copy
      // we should maybe rather use cloneDeep from lodash
      let res = JSON.parse(JSON.stringify(c));
      if (!res.dm.url) {
        res.dm.url =
          res.dm.mode == "no_sec"
            ? this.defval.dm.url.nosec
            : this.defval.dm.url.sec;
      }
      if (!res.bs.url) {
        res.bs.url =
          res.bs.mode == "no_sec"
            ? this.defval.bs.url.nosec
            : this.defval.bs.url.sec;
      }
      return res;
    },
    close() {
      this.show = false;
    },
  },
};
</script>

<template>
  <div>
    <h4 class="pa-1">
      <span class="d-flex">
        <span class="align-self-center"> Instance {{ instanceId }} : </span>

        <instance-control
          :objectdef="objectdef"
          :endpoint="endpoint"
          :id="instanceId"
          :path="instancePath"
          class="mr-auto"
        />

        <v-icon class="pr-3" small v-show="state.observed[instancePath]" :title="'Instance ' + instancePath + ' observed'"
          >mdi-eye-outline</v-icon
        >
      </span>
    </h4>

    <v-expansion-panels
      accordion
      multiple
      dense
      tile
      v-if="objectdef && instanceId && endpoint"
    >
      <v-expansion-panel v-for="resource in resources" :key="resource.path">
        <v-expansion-panel-header style="min-height:32px" class="pa-3">
          <!-- min-width is needed for avoid shift about truncated text 
          see : https://stackoverflow.com/a/36247448/5088764
          -->
          <v-container class="pa-0" style="min-width:0">
            <v-row>
              <v-col cols="7" lg="3" align-self="center" class="pa-2"
                >{{ resource.def.name }}
              </v-col>
              <v-col
                cols="4"
                lg="1"
                align-self="center"
                class="pa-2 text--disabled"
                >{{ resource.path }}
              </v-col>
              <v-col
                cols="1"
                lg="1"
                align-self="center"
                class="pa-2 text--disabled"
              >
                <v-icon
                  x-small
                  v-show="state.observed[resource.path]"
                  :title="'Resource ' + resource.path + ' observed'"
                  >mdi-eye-outline</v-icon
                ></v-col
              >

              <v-col cols="7" lg="3" align-self="center" class="pa-0">
                <resource-control
                  :resourcedef="resource.def"
                  :endpoint="endpoint"
                  :path="resource.path"
                />
              </v-col>
              <v-col
                cols="5"
                lg="4"
                align-self="center"
                class="pa-0"
                :class="{
                  'text--disabled': state.data[resource.path]
                    ? state.data[resource.path].supposed
                    : false,
                }"
              >
                <div class="pr-3 text-truncate">
                  {{
                    state.data[resource.path]
                      ? state.data[resource.path].val
                      : null
                  }}
                </div>
              </v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-header>
        <v-expansion-panel-content style="white-space: pre-wrap"
          >{{ resource.def.description
          }}<br v-if="resource.def.description2" />{{
            resource.def.description2
          }}
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </div>
</template>
<script>
import InstanceControl from "./InstanceControl.vue";
import ResourceControl from "./ResourceControl.vue";
export default {
  components: { ResourceControl, InstanceControl },
  props: {
    objectdef: Object,
    instanceId: String,
    endpoint: String,
  },

  computed: {
    state() {
      return this.$store.state[this.endpoint];
    },
    resources() {
      return this.objectdef.resourcedefs.map((r) => {
        return { path: this.resourcePath(r.id), def: r };
      });
    },
    instancePath() {
      return `/${this.objectdef.id}/${this.instanceId}`;
    },
  },
  methods: {
    resourcePath(resourceId) {
      return `/${this.objectdef.id}/${this.instanceId}/${resourceId}`;
    },
  },
};
</script>

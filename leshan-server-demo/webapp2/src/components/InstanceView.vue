<template>
  <div>
    <h4 class="pa-1">
      Instance {{ instanceId }} :
      <instance-control
        :objectdef="object"
        :endpoint="endpoint"
        :id="instanceId"
        :path="instancePath"
        @input="handleNewValue"
      />
    </h4>

    <v-expansion-panels
      accordion
      multiple
      dense
      tile
      v-model="expanded[object.id]"
    >
      <v-expansion-panel v-for="resource in resources" :key="resource.path">
        <v-expansion-panel-header style="min-height:32px" class="pa-3">
          <!-- min-width is needed for avoid shift about truncated text 
          see : https://stackoverflow.com/a/36247448/5088764
          -->
          <v-container class="pa-0" style="min-width:0">
            <v-row>
              <v-col cols="7" md="3" align-self="center" class="pa-2"
                >{{ resource.def.name }}
              </v-col>
              <v-col
                cols="5"
                md="2"
                align-self="center"
                class="pa-2 text--disabled"
                >{{ resource.path }}</v-col
              >
              <v-col cols="6" md="2" align-self="center" class="pa-0">
                <resource-control
                  :resourcedef="resource.def"
                  :endpoint="endpoint"
                  :path="resource.path"
                  v-model="values[resource.path]"
                />
              </v-col>
              <v-col
                cols="6"
                md="5"
                align-self="center"
                class="pa-0"
                :class="{
                  'text--disabled': values[resource.path]
                    ? values[resource.path].supposed
                    : false,
                }"
              >
                <div class="pr-3 text-truncate">
                  {{ values[resource.path] ? values[resource.path].val : null }}
                </div>
              </v-col>
            </v-row>
          </v-container>
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          {{ resource.def.description }}
          {{ resource.def.description2 }}
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
    object: Object,
    instanceId: String,
    endpoint: String,
    showTitle: Boolean,
    value: Object,
  },
  data() {
    return {
      expanded: {},
    };
  },
  computed: {
    values() {
      return this.value;
    },
    resources() {
      return this.object.resourcedefs.map((r) => {
        return { path: this.resourcePath(r.id), def: r };
      });
    },
    instancePath() {
      return `/${this.object.id}/${this.instanceId}`;
    },
  },
  methods: {
    resourcePath(resourceId) {
      return `/${this.object.id}/${this.instanceId}/${resourceId}`;
    },
    handleNewValue(vals) {
      this.$emit("input", Object.assign({}, this.value, vals));
    },
  },
};
</script>

<template>
  <div>
    <h4 class="pa-1" v-if="showTitle">Instance {{ instanceId }} :</h4>
    <v-expansion-panels
      accordion
      multiple
      dense
      tile
      v-model="expanded[object.id]"
    >
      <v-expansion-panel v-for="resource in resources" :key="resource.path">
        <v-expansion-panel-header style="min-height:32px" class="pa-3">
          <v-container class="pa-0">
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
                md="2"
                align-self="center"
                class="pa-0 "
                :class="{
                 'text--disabled': values[resource.path]
                    ? values[resource.path].supposed
                    : false,
                }"
              >
                {{ values[resource.path] ? values[resource.path].val : null }}
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
import ResourceControl from "./ResourceControl.vue";
export default {
  components: { ResourceControl },
  props: {
    object: Object,
    instanceId: String,
    endpoint: String,
    showTitle: Boolean,
    data: Object
  },
  data() {
    return {
      values: this.data,
      expanded: {},
    };
  },
  computed: {
    resources() {
      return this.object.resourcedefs.map((r) => {
        return { path: this.path(r.id), def: r };
      });
    },
  },
  methods: {
    path(resourceId) {
      return `/${this.object.id}/${this.instanceId}/${resourceId}`;
    },
  },
};
</script>

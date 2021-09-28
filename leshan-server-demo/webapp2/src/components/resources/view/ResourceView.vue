<template>
  <v-row color="grey lighten-5">
    <v-col cols="12" lg="6" xl="5">
      <v-card elevation="0" class="grey lighten-4">
        <!-- Model -->
        <v-card-subtitle class="pb-0 text-decoration-underline">
          Model :
        </v-card-subtitle>
        <v-card-text>
          <v-simple-table dense>
            <template v-slot:default>
              <thead>
                <tr>
                  <th class="text-left">
                    Operations
                  </th>
                  <th class="text-left">
                    Instances
                  </th>
                  <th class="text-left">
                    Mandatory
                  </th>
                  <th class="text-left">
                    Type
                  </th>
                  <th class="text-left">
                    Range or enumeration
                  </th>
                  <th class="text-left">
                    Units
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr style="pointer-events: none" class="text-capitalize">
                  <td>{{ resourcedef.operations }}</td>
                  <td>{{ resourcedef.instancetype }}</td>
                  <td>{{ resourcedef.mandatory }}</td>
                  <td>{{ resourcedef.type }}</td>
                  <td>{{ resourcedef.range }}</td>
                  <td>{{ resourcedef.units }}</td>
                </tr>
              </tbody>
            </template>
          </v-simple-table>
        </v-card-text>

        <!-- description -->
        <v-card-subtitle class="pb-0 text-decoration-underline">
          Description :
        </v-card-subtitle>
        <v-card-text style="white-space: pre-wrap">
          {{ resourcedef.description }}<br v-if="resourcedef.description2" />{{
            resourcedef.description2
          }}
        </v-card-text>
      </v-card>
    </v-col>

    <v-col>
      <v-card elevation="0" v-if="resourcedef.operations.includes('R')">
        <!-- Resource Controls -->
        <resource-control
          :resourcedef="resourcedef"
          :endpoint="endpoint"
          :path="path"
        />

        <!-- Value(s) -->
        <v-card-text
          v-if="resourcedef.instancetype == 'single'"
          class="font-weight-bold"
          :class="{ 'text--primary': resource && !resource.supposed }"
        >
          {{ resourceValueAsString }}
        </v-card-text>
        <multi-instances-resource-view
          v-else
          :resourcedef="resourcedef"
          :endpoint="endpoint"
          :path="path"
          :resource="resource"
        />
      </v-card>
    </v-col>
  </v-row>
</template>
<script>
import { resourceToString } from "../../../js/valueutils.js";
import ResourceControl from "../../ResourceControl.vue";
import MultiInstancesResourceView from "./MultiInstancesResourceView.vue";

export default {
  components: {
    ResourceControl,
    MultiInstancesResourceView,
  },
  props: {
    endpoint: String,
    resourcedef: Object,
    resource: Object,
    path: String,
  },

  computed: {
    resourceValueAsString: function() {
      if (!this.resource) {
        return "";
      } else {
        return resourceToString(this.resource, this.resourcedef.type);
      }
    },
  },
};
</script>

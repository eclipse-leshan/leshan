<template>
  <v-simple-table dense v-if="resource">
    <template v-slot:default>
      <thead>
        <tr>
          <th class="text-left " style="width:1%">
            Id
          </th>
          <th class="text-left">
            Value
          </th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr
          v-for="(value, name) in resource.vals"
          :key="name"
          style="pointer-events: none"
        >
          <td class="text--disabled">{{ name }}</td>
          <td>{{ valueAsString(value.val) }}</td>
        </tr>
      </tbody>
    </template>
  </v-simple-table>
</template>
<script>
import { valueToString } from "../../../js/valueutils.js";

/**
 * Display the state of a "Multi Instance" Resource. 
 * 
 * List all instances with its value and controls to execute operations on it. 
 */
export default {
  props: {
    endpoint: String, // the endpoint of the client 
    resourcedef: Object, // the model of the resource
    resource: Object, // the resource data as defined in store.js
    path: String, // the path of the resource (e.g. /3/0/1)
  },
  methods: {
    valueAsString(value) {
      return valueToString(value, this.resourcedef.type);
    },
  },
};
</script>

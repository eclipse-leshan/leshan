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

export default {
  props: {
    endpoint: String,
    resourcedef: Object,
    resource: Object,
    path: String,
  },
  methods: {
    valueAsString(value) {
      return valueToString(value, this.resourcedef.type);
    },
  },
};
</script>

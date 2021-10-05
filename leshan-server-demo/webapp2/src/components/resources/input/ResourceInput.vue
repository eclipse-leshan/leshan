<template>
  <multi-instance-resource-input
    v-if="resourcedef.instancetype != 'single'"
    :resourcedef="resourcedef"
    :hint="hint"
    :value="value"
    @input="$emit('input', $event)"
  />
  <single-value-input
    v-else
    :resourcedef="resourcedef"
    :hint="hint"
    :value="value"
    @input="$emit('input', $event)"
  />
</template>
<script>
import SingleValueInput from "../../values/input/SingleValueInput.vue";
import MultiInstanceResourceInput from "./MultiInstanceResourceInput.vue";

/**
 * An input for "Single Instance" and "Mult Instance" resource.
 */
export default {
  components: { SingleValueInput, MultiInstanceResourceInput },
  props: {
    value: null, // the input value for this resource (v-model)
    resourcedef: Object, // the model of the resource
    hint: { type: String, default: null }, // hint displayed on `?` tooltip. If `null`, the "?" icon is not displayed"
  },
};
</script>

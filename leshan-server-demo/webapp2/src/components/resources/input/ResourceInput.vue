<template>
  <v-text-field
    v-if="resourcedef.instancetype != 'single'"
    label="multi instance resource not yet supported in UI demo"
    disabled
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

/**
 * An input for "Single Instance" and "Mult Instance" resource.
 */
export default {
  components: { SingleValueInput },
  props: {
    value: null, // the input value for this resource (v-model) 
    resourcedef: Object, // the model of the resource
    hint: { type: String, default: null }, // hint displayed on `?` tooltip. If `null`, the "?" icon is not displayed"
  },
};
</script>

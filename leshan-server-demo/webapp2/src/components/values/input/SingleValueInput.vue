<template>
  <boolean-value-input
    v-if="resourcedef.type == 'boolean'"
    :resourcedef="resourcedef"
    :value="value"
    @input="$emit('input', convertValue($event))"
  />
  <opaque-value-input
    v-else-if="resourcedef.type == 'opaque'"
    :resourcedef="resourcedef"
    :value="value"
    @input="$emit('input', convertValue($event))"
  />
  <v-text-field
    v-else
    :label="resourcedef.type"
    :hint="hint"
    :suffix="resourcedef.units"
    :value="value"
    @input="$emit('input', convertValue($event))"
  />
</template>
<script>
import BooleanValueInput from "./BooleanValueInput.vue";
import OpaqueValueInput from "./OpaqueValueInput.vue";

/**
 * An input for single value LWM2M node ("Single Instance Resource" or "Resource Instance")
 */
export default {
  components: { BooleanValueInput, OpaqueValueInput },
  props: {
    value: null, // the input value for this LWM2M Node (v-model) 
    resourcedef: Object, // the model of the resource 
    hint: { type: String, default: null }, // hint displayed on `?` tooltip. If `null`, the "?" icon is not displayed"
  },
  methods: {
    convertValue(strValue) {
      var val = strValue;
      if (this.resourcedef.type != undefined) {
        switch (this.resourcedef.type) {
          case "integer":
            val = parseInt(strValue);
            val = isNaN(val) ? strValue : val;
            break;
          case "float":
            val = parseFloat(strValue);
            val = isNaN(val) ? strValue : val;
            break;
          default:
            val = strValue;
        }
      }
      return val;
    },
  },
};
</script>

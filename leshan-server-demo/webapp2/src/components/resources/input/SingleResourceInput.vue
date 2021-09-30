<template>
  <boolean-resource-input
    v-if="resourcedef.type == 'boolean'"
    :resourcedef="resourcedef"
    :value="value"
    @input="$emit('input', convertValue($event))"
  />
  <opaque-resource-input
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
import BooleanResourceInput from "./BooleanResourceInput.vue";
import OpaqueResourceInput from "./OpaqueResourceInput.vue";

export default {
  components: { BooleanResourceInput, OpaqueResourceInput },
  props: {
    value: null,
    resourcedef: Object,
    hint: { type: String, default: null },
  },
  methods: {
    convertValue(strValue) {
      // TODO this should probably done in dedicated ResourceInputComponent
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

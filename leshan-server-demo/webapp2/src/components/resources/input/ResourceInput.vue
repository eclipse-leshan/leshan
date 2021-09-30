<template>
  <v-row dense>
    <v-col cols="12" md="2">
      <v-subheader v-if="resourcedef.mandatory"
        ><strong>{{ resourcedef.name }} *</strong></v-subheader
      >
      <v-subheader v-else>{{ resourcedef.name }} </v-subheader>
    </v-col>
    <v-col cols="11" md="9">
      <v-text-field
        v-if="resourcedef.instancetype != 'single'"
        label="multi instance resource not yet supported in UI demo"
        disabled
      />
      <single-resource-input v-else :resourcedef="resourcedef" :hint="hint(resourcedef)" :value="value" @input="$emit('input',$event)"/>
    </v-col>
    <v-col cols="1" md="1">
      <v-tooltip left>
        <template v-slot:activator="{ on }">
          <v-icon v-on="on">
            mdi-help-circle-outline
          </v-icon>
        </template>
        <p style="white-space: pre-wrap">{{ resourcedef.description }}</p>
      </v-tooltip>
    </v-col>
  </v-row>
</template>
<script>
import SingleResourceInput from './SingleResourceInput.vue';
export default {
  components: { SingleResourceInput },
  props: { value: null /*resource value */, resourcedef: Object },
  methods: {
    hint(resourcedef) {
      let hint = "";
      if (resourcedef.mandatory) hint += " mandatory";
      if (resourcedef.range) hint += " (Range: " + resourcedef.range + ")";
      return hint;
    },
  },
};
</script>

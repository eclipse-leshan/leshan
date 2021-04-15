<template>
  <div>
    <!-- identity field -->
    <v-textarea
      filled
      label="Identity"
      v-model="psk.identity"
      :rules="[(v) => !!v || 'Identity is required']"
      @input="$emit('input', psk)"
      spellcheck="false"
    ></v-textarea>

    <!-- key field -->
    <v-textarea
      filled
      label="Key"
      v-model="psk.key"
      hint="Hexadecimal format"
      :rules="[
        (v) => !!v || 'Key is required',
        (v) => /^[0-9a-fA-F]+$/.test(v) || 'Hexadecimal format is expected',
      ]"
      @input="$emit('input', psk)"
      spellcheck="false"
    ></v-textarea>
  </div>
</template>
<script>
export default {
  props: { value: Object },
  data() {
    return {
      psk: this.value,
    };
  },

  watch: {
    value(v) {
      this.psk = v;
    },
  },
};
</script>

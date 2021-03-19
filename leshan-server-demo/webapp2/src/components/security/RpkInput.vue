<template>
  <div>
    <!-- Public Key field -->
    <v-textarea
      filled
      label="Client Public Key"
      v-model="rpk.key"
      :rules="[
        (v) => !!v || 'Public is required',
        (v) => /^[0-9a-fA-F]+$/.test(v) || 'Hexadecimal format is expected',
      ]"
      hint="SubjectPublicKeyInfo der encoded in Hexadecimal"
      @input="$emit('input', rpk)"
      spellcheck="false"
    ></v-textarea>
  </div>
</template>
<script>
export default {
  props: { value: Object },
  data() {
    return {
      rpk: this.value,
    };
  },

  watch: {
    value(v) {
      // on init create local copy
      if (v) {
        this.rpk = this.value;
      }
    },
  },
};
</script>

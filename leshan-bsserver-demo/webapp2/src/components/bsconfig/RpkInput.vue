<template>
  <div>
    <!--Client Public Key field -->
    <v-textarea
      filled
      label="Client Public Key"
      v-model="rpk.client_pub_key"
      :rules="[
        (v) => !!v || 'Client Public Key is required',
        (v) => /^[0-9a-fA-F]+$/.test(v) || 'Hexadecimal format is expected',
      ]"
      hint="SubjectPublicKeyInfo der encoded in Hexadecimal"
      @input="$emit('input', rpk)"
      spellcheck="false"
    ></v-textarea>
    <!--Client Private Key field -->
    <v-textarea
      filled
      label="Client Private Key"
      v-model="rpk.client_pri_key"
      :rules="[
        (v) => !!v || 'Client Private Key is required',
        (v) => /^[0-9a-fA-F]+$/.test(v) || 'Hexadecimal format is expected',
      ]"
      hint="SubjectPublicKeyInfo der encoded in Hexadecimal"
      @input="$emit('input', rpk)"
      spellcheck="false"
    ></v-textarea>
    <!--Server Public Key field -->
    <v-textarea
      filled
      label="Server Public Key"
      v-model="rpk.server_pri_key"
      :rules="[
        (v) => !!v || 'Server Public Key is required',
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
      rpk: {},
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

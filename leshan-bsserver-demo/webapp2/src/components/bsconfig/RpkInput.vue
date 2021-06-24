<template>
  <div>
    <!--Client Public Key field -->
    <v-textarea
      filled
      label="Client Public Key"
      v-model="rpk.client_pub_key"
      :rules="[
        (v) =>
          !!defaultvalue.client_pub_key || !!v || 'Client Public Key is required',
        (v) => !v || /^[0-9a-fA-F]*$/.test(v) || 'Hexadecimal format is expected',
      ]"
      hint="SubjectPublicKeyInfo der encoded in Hexadecimal"
      @input="$emit('input', rpk)"
      spellcheck="false"
      :placeholder="defaultvalue.client_pub_key"
      persistent-placeholder
      rows="2"
    ></v-textarea>
    <!--Client Private Key field -->
    <v-textarea
      filled
      label="Client Private Key"
      v-model="rpk.client_pri_key"
      :rules="[
        (v) =>
          !!defaultvalue.client_pri_key || !!v || 'Client Private Key is required',
        (v) => !v || /^[0-9a-fA-F]*$/.test(v) || 'Hexadecimal format is expected',
      ]"
      hint="PKCS8 format der encoded in Hexadecimal"
      @input="$emit('input', rpk)"
      spellcheck="false"
      :placeholder="defaultvalue.client_pri_key"
      persistent-placeholder
      rows="2"
    ></v-textarea>
    <!--Server Public Key field -->
    <v-textarea
      filled
      label="Server Public Key"
      v-model="rpk.server_pub_key"
      :rules="[
        (v) =>
          !!defaultvalue.server_pub_key || !!v || 'Server Public Key is required',
        (v) => !v || /^[0-9a-fA-F]*$/.test(v) || 'Hexadecimal format is expected',
      ]"
      hint="SubjectPublicKeyInfo der encoded in Hexadecimal"
      @input="$emit('input', rpk)"
      spellcheck="false"
      rows="2"
      :placeholder="defaultvalue.server_pub_key"
      persistent-placeholder
    ></v-textarea>
  </div>
</template>
<script>
export default {
  props: { value: Object, defaultvalue:Object},
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

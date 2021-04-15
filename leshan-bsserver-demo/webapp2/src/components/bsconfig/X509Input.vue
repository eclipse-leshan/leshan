<template>
  <div>
    <!--Client Certificate Key field -->
    <v-textarea
      filled
      label="Client Certificate Key"
      v-model="x509.client_certificate"
      :rules="[
        (v) => !!v || 'Client Certificate Key is required',
        (v) => /^[0-9a-fA-F]+$/.test(v) || 'Hexadecimal format is expected',
      ]"
      hint="SubjectPublicKeyInfo der encoded in Hexadecimal"
      @input="$emit('input', x509)"
      spellcheck="false"
    ></v-textarea>
    <!--Client Private Key field -->
    <v-textarea
      filled
      label="Client Private Key"
      v-model="x509.client_pri_key"
      :rules="[
        (v) => !!v || 'Client Private Key is required',
        (v) => /^[0-9a-fA-F]+$/.test(v) || 'Hexadecimal format is expected',
      ]"
      hint="SubjectPublicKeyInfo der encoded in Hexadecimal"
      @input="$emit('input', x509)"
      spellcheck="false"
    ></v-textarea>
    <!--Server Certificate Key field -->
    <v-textarea
      filled
      label="Server Certificate Key"
      v-model="x509.server_certificate"
      :rules="[
        (v) => !!v || 'Server Certificate Key is required',
        (v) => /^[0-9a-fA-F]+$/.test(v) || 'Hexadecimal format is expected',
      ]"
      hint="SubjectPublicKeyInfo der encoded in Hexadecimal"
      @input="$emit('input', x509)"
      spellcheck="false"
    ></v-textarea>
    <!--Certificate Usagefield -->
    <v-select
      :items="usages"
      item-text="label"
      item-value="id"
      label="Certificate Usage"
      v-model="x509.certificate_usage"
      @input="$emit('input', x509)"
    ></v-select>
  </div>
</template>
<script>
export default {
  props: { value: Object },
  data() {
    return {
      x509: { certificate_usage: 3 },
      usages: [
        { id: 0, label: "CA constraint" },
        { id: 1, label: "service certificate constraint" },
        { id: 2, label: "trust anchor assertion" },
        { id: 3, label: "domain-issued certificate" },
      ],
    };
  },

  watch: {
    value(v) {
      // on init create local copy
      if (v) {
        this.x509 = this.value;
      }
    },
  },
};
</script>

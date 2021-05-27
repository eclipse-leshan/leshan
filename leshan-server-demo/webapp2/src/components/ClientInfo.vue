<template>
  <span>
    <!-- info icon -->
    <v-tooltip :left="tooltipleft" :bottom="tooltipbottom">
      <template v-slot:activator="{ on }">
        <v-icon v-on="on" class="pr-2" :small="small">
          mdi-information
        </v-icon>
      </template>
      Lifetime : {{ registration.lifetime }}s
      <br />
      Binding mode : {{ registration.bindingMode }} 
      <span v-if="registration.queuemode">
      <br /> 
      Using QueueMode 
      </span>
      <br />
      Protocole version : {{ registration.lwM2mVersion }}
      <br />
      Address : {{ registration.address }}
      <span
        v-for="(key, val) in registration.additionalRegistrationAttributes"
        :key="key"
      >
        <br />
        {{ key }} {{ val }}
      </span>
    </v-tooltip>
    <!-- secure icon -->
    <v-tooltip :left="tooltipleft" :bottom="tooltipbottom">
      <template v-slot:activator="{ on }">
        <v-icon v-on="on" class="pr-2" :small="small" v-visible="registration.secure">
          mdi-lock
        </v-icon>
      </template>
      Communication over DTLS
    </v-tooltip>
    <!-- secure icon -->
    <v-tooltip :left="tooltipleft" :bottom="tooltipbottom">
      <template v-slot:activator="{ on }">
        <v-icon v-on="on" class="pr-2" :small="small" v-visible="registration.sleeping">
          mdi-sleep
        </v-icon>
      </template>
      Device using Queue mode is absent
    </v-tooltip>
  </span>
</template>
<script>
export default {
  props: {
    registration: Object,
    small: Boolean,
    tooltipleft: Boolean,
    tooltipbottom: Boolean,
  },
};
</script>

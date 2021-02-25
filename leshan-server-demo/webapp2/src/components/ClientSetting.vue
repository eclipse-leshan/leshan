<template>
  <div class="pa-0">
    <div title="Timeout used to send request">
      <v-select
        single-line
        dense
        hide-details
        prefix="Timeout :"
        :items="timeoutList"
        item-value="val"
        item-text="txt"
        v-model="timeout"
        append-outer-icon="mdi-help-circle-outline"
        @click:append-outer="openTimeoutHelps"
      ></v-select>
    </div>
    <div title="Content Format used to send request handling single value">
      <v-select
        single-line
        dense
        prefix="Format :"
        hide-details
        :items="singleFormatList"
        v-model="singleformat"
      ></v-select>
    </div>
    <!-- TODO uncomment when we will support multi node write or read -->
    <!--v-select
      single-line
      dense
      hide-details
      prefix="Multi :"
      :items="multiFormatList"
      v-model="multiformat"
    ></v-select-->
  </div>
</template>
<script>
import { preference } from "vue-preferences";

export default {
  data() {
    return {
      timeoutList: [
        { txt: "5s", val: 5 },
        { txt: "5min", val: 5 * 60 },
        { txt: "15min", val: 15 * 60 },
        { txt: "30min", val: 30 * 60 },
        { txt: "None", val: null },
      ],
      singleFormatList: [
        "TLV",
        "CBOR",
        "TEXT",
        "OPAQUE",
        "SENML_JSON",
        "SENML_CBOR",
        "JSON",
      ],
      // TODO uncomment when we will support multi node write or read
      //multiFormatList: ["TLV", "SENML_JSON", "SENML_CBOR", "JSON"],
    };
  },
  computed: {
    timeout: preference("timeout", { defaultValue: 5 }),
    singleformat: preference("singleformat", { defaultValue: "TLV" }),
    // TODO uncomment when we will support multi node write or read
    //multiformat: preference("multiformat", { defaultValue: "TLV" }),
  },
  methods: {
    openTimeoutHelps() {
      window.open(
        "https://github.com/eclipse/leshan/wiki/Request-Timeout",
        "_blank"
      );
    },
  },
};
</script>

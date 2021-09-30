<template>
  <div :title="resourceValueAsString">
    {{ resourceValueAsString }}
  </div>
</template>
<script>
import { resourceToString } from "../../../js/valueutils.js";

/**
 *  Display "Single Instance" or "Multi Instance" resource as a single line.
 */
export default {
  props: {
    resourcedef: Object, // model of the resource
    resource: Object, // the resource data as defined in store.js
  },

  computed: {
    resourceValueAsString: function() {
      if (!this.resource) {
        return "";
      } else {
        return resourceToString(this.resource, this.resourcedef.type);
      }
    },
  },
};
</script>

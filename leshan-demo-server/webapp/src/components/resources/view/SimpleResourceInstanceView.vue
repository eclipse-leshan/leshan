<!-----------------------------------------------------------------------------
 * Copyright (c) 2021 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
  ----------------------------------------------------------------------------->
<template>
  <div :title="resourceInstanceValueAsString">
    {{ resourceInstanceValueAsString }}
  </div>
</template>
<script>
import { valueToString } from "../../../js/valueutils.js";

/**
 *  Display "Resource Instance" value as a single line.
 */
export default {
  props: {
    resourcedef: Object, // model of the resource
    resourceInstance: Object, // the resource instance data as defined in store.js
  },

  computed: {
    resourceInstanceValueAsString: function() {
      if (!this.resourceInstance) {
        return "";
      } else {
        return valueToString(this.resourceInstance.val, this.resourcedef.type);
      }
    },
  },
};
</script>

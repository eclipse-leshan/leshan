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

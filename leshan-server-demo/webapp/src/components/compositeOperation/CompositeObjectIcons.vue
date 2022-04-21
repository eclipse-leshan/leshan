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
  <span>
    <object-icon
      v-for="(id, index) in objectIds"
      :key="index"
      :objectId="id"
      :invalidObjectIcon="$icons.mdiSelect"
    />
  </span>
</template>
<script>
import { LwM2mPath } from "../../js/lwm2mpath";
import ObjectIcon from "../object/ObjectIcon.vue";
export default {
  components: { ObjectIcon },
  props: {
    compositeObject: Object, // the composite object for which we need to display icon
  },
  computed: {
    objectIds() {
      // create a set containing all object id refers by this compositeObject
      let objectIds = new Set([]);
      if (this.compositeObject) {
        let paths = this.compositeObject.paths;
        paths.forEach((p) => {
          let lp = new LwM2mPath(p);
          if (lp.objectid != null) objectIds.add(lp.objectid);
        });
      }
      // return only the first 3 one.
      objectIds = [...objectIds].slice(0, 3);

      // fill missing one with
      for (let i = objectIds.length; i < 3; i++) {
        objectIds.push(-1);
      }
      return objectIds;
    },
  },
};
</script>

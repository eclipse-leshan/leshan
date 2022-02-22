/*******************************************************************************
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
 *******************************************************************************/

import Vue from "vue";
import {
  mdiAccessPointNetwork,
  mdiAlertCircle,
  mdiCheckboxBlankOutline,
  mdiCertificate,
  mdiCloseBoxOutline,
  mdiCogOutline,
  mdiDelete,
  mdiDevices,
  mdiDownload,
  mdiExitRun,
  mdiEyeOutline,
  mdiEyeRemoveOutline,
  mdiFormatListCheckbox,
  mdiHelpBox,
  mdiHelpCircleOutline,
  mdiHelpRhombusOutline,
  mdiInformation,
  mdiInformationOutline,
  mdiKey,
  mdiKeyChange,
  mdiKeyPlus,
  mdiLock,
  mdiLockOpenRemove,
  mdiLockOutline,
  mdiMagnify,
  mdiPencil,
  mdiSelect,
  mdiServerSecurity,
  mdiShieldKey,
  mdiSleep,
  mdiTune,
} from "@mdi/js";

/**
 * We create a Icons plugin to load plugin using @mdi/js.
 * This will allow to use wepback treeshaking, and so only embedded used icons instead of the whole mdi font
 * See :
 *   https://stackoverflow.com/questions/57552261/vuetifyjs-adding-only-used-icons-to-build
 *   https://github.com/vuetifyjs/vuetify/issues/8265
 */

// create plugin which make data accessible on all vues
const _icons = {
  mdiAccessPointNetwork,
  mdiAlertCircle,
  mdiCheckboxBlankOutline,
  mdiCertificate,
  mdiCloseBoxOutline,
  mdiCogOutline,
  mdiDelete,
  mdiDevices,
  mdiDownload,
  mdiExitRun,
  mdiEyeOutline,
  mdiEyeRemoveOutline,
  mdiFormatListCheckbox,
  mdiHelpBox,
  mdiHelpCircleOutline,
  mdiHelpRhombusOutline,
  mdiInformation,
  mdiInformationOutline,
  mdiKey,
  mdiKeyChange,
  mdiKeyPlus,
  mdiLock,
  mdiLockOpenRemove,
  mdiLockOutline,
  mdiMagnify,
  mdiPencil,
  mdiSelect,
  mdiServerSecurity,
  mdiShieldKey,
  mdiSleep,
  mdiTune,
};

let IconsPlugin = {};
IconsPlugin.install = function (Vue) {
  Object.defineProperties(Vue.prototype, {
    $icons: {
      get() {
        return _icons;
      },
    },
  });
};

Vue.use(IconsPlugin);

export default IconsPlugin;

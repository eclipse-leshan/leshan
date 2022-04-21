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
import {
  mdiCertificate,
  mdiLock,
  mdiKeyChange,
  mdiHelpRhombusOutline,
} from "@mdi/js";

function adaptToUI(sec) {
  // TODO this is a bit tricky, probably better to adapt the REST API
  // But do not want to change it while we have 2 demo UI (old & new)
  let s = {};
  s.endpoint = sec.endpoint;
  s.mode = getMode(sec);
  if (s.mode != "unsupported" && s.mode != "x509") s.details = sec[s.mode];
  return s;
}

function getMode(sec) {
  if (sec.x509) return "x509";
  else if (sec.psk) return "psk";
  else if (sec.rpk) return "rpk";
  else return "unsupported";
}

function getModeIcon(mode) {
  switch (mode) {
    case "x509":
      return mdiCertificate;
    case "psk":
      return mdiLock;
    case "rpk":
      return mdiKeyChange;
    default:
      return mdiHelpRhombusOutline;
  }
}

function adaptToAPI(sec, endpoint) {
  // TODO this is a bit tricky, probably better to adapt the REST API
  // But do not want to change it while we have 2 demo UI (old & new)
  let s = {};
  s.endpoint = endpoint;
  if (sec.mode == "x509") {
    s[sec.mode] = true;
  } else if (sec.mode != "unsupported") {
    s[sec.mode] = sec.details;
  }
  return s;
}

export { adaptToUI, getMode, getModeIcon, adaptToAPI };

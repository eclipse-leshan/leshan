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

function toHex(byteArray) {
  let hex = [];
  for (let i in byteArray) {
    hex[i] = byteArray[i].toString(16).toUpperCase();
    if (hex[i].length === 1) {
      hex[i] = "0" + hex[i];
    }
  }
  return hex.join("");
}

function toAscii(byteArray) {
  let ascii = [];
  for (let i in byteArray) {
    ascii[i] = String.fromCharCode(byteArray[i]);
  }
  return ascii.join("");
}

function base64ToBytes(base64) {
  let byteKey = atob(base64);
  let byteKeyLength = byteKey.length;
  let array = new Uint8Array(new ArrayBuffer(byteKeyLength));
  for (let i = 0; i < byteKeyLength; i++) {
    array[i] = byteKey.charCodeAt(i);
  }
  return array;
}

function fromAscii(ascii) {
  var bytearray = [];
  for (var i in ascii) {
    bytearray[i] = ascii.charCodeAt(i);
  }
  return bytearray;
}

function fromHex(hex) {
  var bytes = [];
  for (var i = 0; i < hex.length - 1; i += 2) {
    bytes.push(parseInt(hex.substr(i, 2), 16));
  }
  return bytes;
}

export { toHex, base64ToBytes, toAscii, fromAscii, fromHex };

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

/**
 * @param {*} value the value of a resource or resource instance
 * @param {*} type the type of the resource
 * @returns a string for this value
 */
function valueToString(value, type) {
  if (type == "objlnk") {
    return value.objectId + ":" + value.objectInstanceId;
  } else if (type == "time") {
    let date = new Date(value);
    return date.toDateString()+ " - " +  date.toLocaleTimeString() + " (" + date.getTime()/1000 + "s)";
  } else {
    return String(value);
  }
}

/**
 * @param {*} value the value of a resource or resource instance
 * @param {*} type the type of the resource
 * @returns a string for this value
 */
function resourceToString(resource, type) {
  if (resource.isSingle) {
    // single resource
    return valueToString(resource.val, type);
  } else {
    // multi resource
    let entries = Object.entries(resource.vals);
    let size = entries.length;
    if (size == 0) {
      return "0 instance";
    } else {
      let nbInstances = size + " instance" + (size > 1 ? "s" : "");
      let values = entries
        .map(([id, v]) => id + ":" + valueToString(v.val, type))
        .join(", ");
      return nbInstances + " [" + values + "]";
    }
  }
}

export { valueToString, resourceToString };

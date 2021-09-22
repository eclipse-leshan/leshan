/**
 * @param {*} value the value of a resource or resource instance
 * @param {*} type the type of the resource
 * @returns a string for this value
 */
function valueToString(value, type) {
  if (type == "objlnk") {
    return value.objectId + ":" + value.objectInstanceId;
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

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
 * a class to handle LWM2M path
 */
class LwM2mPath {
  /**
   * @param {String} path as a string
   */
  constructor(path) {
    this.path = path;

    if (path.indexOf("/") != 0) {
      this.type = "invalid";
      return;
    }
    let pathPart = path.split("/");
    try {
      if (pathPart.length == 2) {
        if (path.length == 1) {
          this.type = "root";
        } else {
          this.type = "object";
          this.objectid = parseInt(pathPart[1]);
        }
      } else if (pathPart.length == 3) {
        this.type = "objectinstance";
        this.objectid = parseInt(pathPart[1]);
        this.objectinstanceid = parseInt(pathPart[2]);
      } else if (pathPart.length == 4) {
        this.type = "resource";
        this.objectid = parseInt(pathPart[1]);
        this.objectinstanceid = parseInt(pathPart[2]);
        this.resourceid = parseInt(pathPart[3]);
      } else if (pathPart.length == 5) {
        this.type = "resourceinstance";
        this.objectid = parseInt(pathPart[1]);
        this.objectinstanceid = parseInt(pathPart[2]);
        this.resourceid = parseInt(pathPart[3]);
        this.resourceinstanceid = parseInt(pathPart[4]);
      } else {
        this.type = "invalid";
      }
    } catch (error) {
      this.type = "invalid";
      console.log("invalid path", path, error);
    }
  }
  toResourcePath() {
    return `/${this.objectid}/${this.objectinstanceid}/${this.resourceid}`;
  }


  toString() {
    return this.path;
  }
}

export { LwM2mPath };

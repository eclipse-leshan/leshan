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

// convert config from rest API format to UI format :
// we regroup security and server data
var configFromRestToUI = function (config) {
  var newConfig = { dm: [], bs: [] };
  for (var i in config.security) {
    var security = config.security[i];
    if (security.bootstrapServer) {
      let bs = { security: security };

      // add oscore object (if any) to bs
      let oscoreObjectInstanceId = security.oscoreSecurityMode;
      let oscore = config.oscore[oscoreObjectInstanceId];
      if (oscore) {
        bs.oscore = oscore;
      }

      newConfig.bs.push(bs);
    } else {
      // search for DM information;
      var server;
      for (var j in config.servers) {
        server = config.servers[j];
        if (server.shortId === security.serverId) {
          newConfig.dm.push(server);
          server.security = security;
        }
      }

      // add oscore object (if any) to dm
      let oscoreObjectInstanceId = security.oscoreSecurityMode;
      let oscore = config.oscore[oscoreObjectInstanceId];
      if (oscore) {
        server.oscore = oscore;
      }
    }
  }
  newConfig.toDelete = config.toDelete;
  newConfig.autoIdForSecurityObject = config.autoIdForSecurityObject;
  return newConfig;
};
var configsFromRestToUI = function (configs) {
  var newConfigs = [];
  for (var endpoint in configs) {
    var config = configFromRestToUI(configs[endpoint]);
    config.endpoint = endpoint;
    newConfigs.push(config);
  }
  return newConfigs;
};

//convert config from UI to rest API format:
var configFromUIToRest = function (c) {
  // do a deep copy
  // we should maybe rather use cloneDeep from lodashz
  let config = JSON.parse(JSON.stringify(c));
  var newConfig = { servers: {}, security: {}, oscore: {} };
  for (var i = 0; i < config.bs.length; i++) {
    var bs = config.bs[i];
    newConfig.security[i] = bs.security;
    if (bs.oscore) {
      newConfig.security[i].oscoreSecurityMode = i;
      newConfig.oscore[i] = bs.oscore;
    }
  }
  if (i == 0) {
    // To be sure that we are not using instance ID 0 for a DM server.
    // The convention is to keep it for Bootstrap server.
    i = 1;
  }
  for (var j = 0; j < config.dm.length; j++) {
    var dm = config.dm[j];
    newConfig.security[i + j] = dm.security;
    delete dm.security;
    if (dm.oscore) {
      newConfig.security[i + j].oscoreSecurityMode = i + j;
      newConfig.oscore[i + j] = dm.oscore;
      delete dm.oscore;
    }
    newConfig.servers[j] = dm;
  }
  newConfig.toDelete = config.toDelete;
  newConfig.autoIdForSecurityObject = config.autoIdForSecurityObject;
  return newConfig;
};

export { configsFromRestToUI, configFromUIToRest };

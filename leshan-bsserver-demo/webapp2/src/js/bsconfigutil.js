// convert config from rest API format to UI format :
// we regroup security and server data
var configFromRestToUI = function(config) {
  var newConfig = { dm: [], bs: [] };
  for (var i in config.security) {
    var security = config.security[i];
    if (security.bootstrapServer) {
      newConfig.bs.push({ security: security });
    } else {
      newConfig.dm = [];
      // search for DM information;
      for (var j in config.servers) {
        var server = config.servers[j];
        if (server.shortId === security.serverId) {
          newConfig.dm.push(server);
          server.security = security;
        }
      }
      if (!newConfig.dm) {
        newConfig.dm.push({ security: security });
      }
    }
  }
  return newConfig;
};
var configsFromRestToUI = function(configs) {
  var newConfigs = [];
  for (var endpoint in configs) {
    var config = configFromRestToUI(configs[endpoint]);
    config.endpoint = endpoint;
    newConfigs.push(config);
  }
  return newConfigs;
};

//convert config from UI to rest API format:
var configFromUIToRest = function(c) {
  // do a deep copy
  // we should maybe rather use cloneDeep from lodashz
  let config = JSON.parse(JSON.stringify(c));
  var newConfig = { servers: {}, security: {} };
  for (var i = 0; i < config.bs.length; i++) {
    var bs = config.bs[i];
    newConfig.security[i] = bs.security;
  }
  for (var j = 0; j < config.dm.length; j++) {
    var dm = config.dm[j];
    newConfig.security[i + j] = dm.security;
    delete dm.security;
    newConfig.servers[j] = dm;
  }
  newConfig.toDelete = ["/0", "/1"];
  return newConfig;
};

export { configsFromRestToUI, configFromUIToRest };

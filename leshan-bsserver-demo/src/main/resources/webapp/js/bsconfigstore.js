// convert config from rest API format to UI format :
// we regroup security and server data
var configFromRestToUI = function(config){
    var newConfig = {dm:[],bs:[]};
    for (var i in config.security){
        var security = config.security[i];
        if (security.bootstrapServer){
            newConfig.bs.push({security:security});

            // add oscore object (if any) to bs
            var oscoreObjectInstanceId = security.oscoreSecurityMode;
            var oscore = config.oscore[oscoreObjectInstanceId];
            if(oscore){
                newConfig.bs.push({oscore:oscore});
            }
        }else{
            newConfig.dm = [];
            // search for DM information;
            for (var j in config.servers){
                var server = config.servers[j];
                if (server.shortId === security.serverId){
                    newConfig.dm.push(server);
                    server.security = security;
                }
            }
            if (!newConfig.dm){
                newConfig.dm.push({security:security});
            }

            // add oscore object (if any) to dm
            var oscoreObjectInstanceId = security.oscoreSecurityMode;
            var oscore = config.oscore[oscoreObjectInstanceId];
            if(oscore){
                newConfig.dm.push({oscore:oscore});
            }
        }
    }
    return newConfig;
};
var configsFromRestToUI = function(configs){
    var newConfigs = {};
    for (var endpoint in configs){
        newConfigs[endpoint] = configFromRestToUI(configs[endpoint]);
    }
    return newConfigs;
};

//convert config from UI to rest API format:
var configFromUIToRest = function(config){
    var newConfig = {servers:{}, security:{}, oscore:{}};
    var writingOscore = false;
    for (var i = 0; i < config.bs.length; i++) {
        var bs = config.bs[i];
        newConfig.security[i] = bs.security;
        newConfig.oscore[i] = bs.oscore;
        writingOscore |= (bs.oscore != null);
    }
    for (var j = 0; j < config.dm.length; j++) {
        var dm = config.dm[j];
        newConfig.security[i+j] = dm.security;
        delete dm.security;
        newConfig.oscore[i+j] = dm.oscore;
        writingOscore |= (dm.oscore != null);
        delete dm.oscore;
        newConfig.servers[j] = dm;
    }
    newConfig.toDelete = ["/0", "/1"]
    if(writingOscore) {
        newConfig.toDelete.push("/21");
    }
    return newConfig;
};

// A bootstrap config store
function BsConfigStore() {
    riot.observable(this); // Riot provides our event emitter.

    var self = this;

    self.bsconfigs = {};

    self.init = function (){
        $.get('api/bootstrap', function(data) {
            self.bsconfigs = configsFromRestToUI(data);
            self.trigger('changed', self.bsconfigs);
        }).fail(function(xhr, status, error){
            var err = "Unable to get the bootstrap info list";
            console.error(err, status, error, xhr.responseText);
            alert(err + ": " + xhr.responseText);
        });
    };

    self.add = function(endpoint,config) {
        var data = configFromUIToRest(config);
        $.ajax({
            type: "POST",
            url: 'api/bootstrap/'+encodeURIComponent(endpoint),
            data: JSON.stringify(data),
            contentType:"application/json; charset=utf-8",
        }).done(function () {
            self.bsconfigs[endpoint] = configFromRestToUI(data);
            self.trigger('changed', self.bsconfigs);
        }).fail(function (xhr, status, error) {
          var err = "Unable to post the bootstrap config";
          console.error(err, endpoint, status, error, xhr.responseText);
          alert(err + ": " + xhr.responseText);
        });
    };

    self.remove = function(endpoint) {
        $.ajax({
            type: "DELETE",
            url: 'api/bootstrap/'+encodeURIComponent(endpoint),
        }).done(function () {
            delete self.bsconfigs[endpoint];
            self.trigger('changed', self.bsconfigs);
        }).fail(function (xhr, status, error) {
          var err = "Unable to delete the bootstrap config";
          console.error(err,endpoint, status, xhr.responseText);
          alert(err + ": " +xhr.responseText);
        });
    };
}

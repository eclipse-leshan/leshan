function server() {
    riot.observable(this); // Riot provides our event emitter.

    var self = this;

    self.security = {};

    self.init = function() {
        $.get('api/server/security', function(data) {
            self.security = data;
            self.trigger('initialized', self.security);
        }).fail(function(xhr, status, error) {
            var err = "Unable to get the server info";
            console.error(err, status, error, xhr.responseText);
            alert(err + ": " + xhr.responseText);
        });
    };
}
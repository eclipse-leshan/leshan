<securityconfig-input>
    <!-- Server URI -->
    <div class="form-group" >
        <label for="uri" class="col-sm-4 control-label">LWM2M Server URL</label>
        <div class="col-sm-8">
            <input class="form-control" id="uri" ref="uri" placeholder={default_uri()} disabled={disable.uri}>
        </div>
    </div>
    
    <!-- Security Mode -->
    <div class="form-group">
        <label for="secMode" class="col-sm-4 control-label">Security mode</label>
        <div class="col-sm-8">
            <select class="form-control" id="secMode" ref="secMode">
                <option value="no_sec" show={secmode.no_sec} >No Security</option>
                <option value="psk"    show={secmode.psk}    >Pre-Shared Key</option>
                <option value="rpk"    show={secmode.rpk}    >Raw Public Key</option>
                <option value="x509"   show={secmode.x509}   >X.509 Certificate</option>
            </select>
        </div>
    </div>

    <!-- PSK -->
    <div if={ refs.secMode.value == "psk" }>
        <psk-input ref="psk" onchange={onchange}></psk-input>
    </div>

     <!-- RPK -->
    <div if={  refs.secMode.value == "rpk" } >
        <rpk-input ref="rpk" onchange={onchange} disable={disable} serverpubkey={serverpubkey}></rpk-input>
    </div>

    <!-- X509 -->
    <div if={  refs.secMode.value == "x509" } >
        <x509-input ref="x509" onchange={onchange} disable={disable} servercertificate={servercertificate}></x509-input>
    </div>

    <script>
        // Tag definition
        var tag = this;
        // Tag Params
        tag.secmode = opts.secmode || {no_sec:true};
        tag.disable = opts.disable || {};
        tag.serverpubkey = opts.serverpubkey || "";
        tag.servercertificate = opts.servercertificate || "";
        tag.onchange = opts.onchange;
        tag.securi = opts.securi || "";
        tag.unsecuri = opts.unsecuri || "";
        // Tag API
        tag.has_error = has_error;
        tag.get_value = get_value;
        // Internal state
        tag.default_uri = default_uri;

        // Tag Functions
        function default_uri() {
            if (!tag.refs.secMode || tag.refs.secMode.value == "no_sec")
                return opts.unsecuri;
            else
                return opts.securi;
        }

        function has_error() {
            return tag.refs.secMode.value === "psk"  && tag.refs.psk.has_error()
                || tag.refs.secMode.value === "rpk"  && tag.refs.rpk.has_error()
                || tag.refs.secMode.value === "x509" && tag.refs.x509.has_error();
        }

        function get_value() {
            var config = {};

            // set URI
            if (!tag.refs.uri.value || tag.refs.uri.value.length == 0) {
                config.uri = tag.default_uri();
            } else {
                config.uri = tag.refs.uri.value;
            }

            // set Secure mode
            config.secmode = tag.refs.secMode.value.toUpperCase();

            // set Credentials
            config.id = [];
            config.key = [];
            config.serverKey = [];
            if (config.secmode === "PSK"){
                var psk = tag.refs.psk.get_value()
                config.id = fromAscii(psk.id);
                config.key = fromHex(psk.key);
            } else if(config.secmode === "RPK"){
                var rpk = tag.refs.rpk.get_value();
                config.id = fromHex(rpk.pubkey);
                config.key = fromHex(rpk.privkey);
                config.serverKey = fromHex(rpk.servpubkey);
            } else if(config.secmode === "X509"){
                var x509 = tag.refs.x509.get_value();
                config.id = fromHex(x509.cert);
                config.key = fromHex(x509.key);
                config.serverKey = fromHex(x509.servCert);
            }

            return config;
        }

        // Utils
        function fromAscii(ascii){
            var bytearray = [];
            for (var i in ascii){
                bytearray[i] = ascii.charCodeAt(i);
            }
            return bytearray;
        };

        function fromHex(hex){
            var bytes = [];
            for(var i=0; i< hex.length-1; i+=2) {
                bytes.push(parseInt(hex.substr(i, 2), 16));
            }
            return bytes;
        };
    </script>
</securityconfig-input>
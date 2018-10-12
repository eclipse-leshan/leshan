<x509-input>
    <!-- X.509 inputs -->
    <div class={ form-group:true, has-error: x509Cert.error }>
        <label for="x509Cert" class="col-sm-4 control-label">Client certificate</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="3" id="x509Cert" ref="x509Cert" oninput={validate_x509Cert} onblur={validate_x509Cert}></textarea>
            <p class="text-right text-muted small" style="margin:0">Hexadecimal format</p>
            <p class="help-block" if={x509Cert.required}>The client certificate is required</p>
            <p class="help-block" if={x509Cert.nothexa}>Hexadecimal format is expected</p>
        </div>
    </div>
    <div class={ form-group:true, has-error: x509PrivateKey.error }>
        <label for="x509PrivateKey" class="col-sm-4 control-label">Client private key</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="3" id="x509PrivateKey" ref="x509PrivateKey" oninput={validate_x509PrivateKey} onblur={validate_x509PrivateKey}></textarea>
            <p class="text-right text-muted small" style="margin:0">Hexadecimal format</p>
            <p class="help-block" if={x509PrivateKey.required}>The client private key is required</p>
            <p class="help-block" if={x509PrivateKey.nothexa}>Hexadecimal format is expected</p>
        </div>
    </div>
    <div class={ form-group:true, has-error: x509ServerCert.error }>
        <label for="x509ServerCert" class="col-sm-4 control-label">Server certificate</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="3" id="x509ServerCert" ref="x509ServerCert" oninput={validate_x509ServerCert} onblur={validate_x509ServerCert} disabled={disable.servercertificate} placeholder={servercertificate}></textarea>
            <p class="text-right text-muted small" style="margin:0">Hexadecimal format</p>
            <p class="help-block" if={x509ServerCert.required}>The server certificate is required</p>
            <p class="help-block" if={x509ServerCert.nothexa}>Hexadecimal format is expected</p>
        </div>
    </div>

    <script>
        // Tag definition
        var tag = this;
        // Tag Params
        tag.onchange = opts.onchange;
        tag.disable = opts.disable || {};
        tag.servercertificate = opts.servercertificate || "";
        // Tag API
        tag.has_error = has_error;
        tag.get_value = get_value
        // Tag intenal state
        tag.x509Cert = {};
        tag.x509PrivateKey = {};
        tag.x509ServerCert = {};
        tag.validate_x509Cert = validate_x509Cert;
        tag.validate_x509PrivateKey = validate_x509PrivateKey;
        tag.validate_x509ServerCert = validate_x509ServerCert;

        // Tag functions
        function validate_x509Cert(){
            var str = tag.refs.x509Cert.value;
            tag.x509Cert.error = false;
            tag.x509Cert.required = false;
            tag.x509Cert.nothexa = false;
            if (!str || 0 === str.length){
                tag.x509Cert.error = true;
                tag.x509Cert.required = true;
            }else if (! /^[0-9a-fA-F]+$/i.test(str)){
                tag.x509Cert.error = true;
                tag.x509Cert.nothexa = true;
            }
            tag.onchange();
        }

        function validate_x509PrivateKey(){
            var str = tag.refs.x509PrivateKey.value;
            tag.x509PrivateKey.error = false;
            tag.x509PrivateKey.required = false;
            tag.x509PrivateKey.nothexa = false;
            if (!str || 0 === str.length){
                tag.x509PrivateKey.error = true;
                tag.x509PrivateKey.required = true;
            }else if (! /^[0-9a-fA-F]+$/i.test(str)){
                tag.x509PrivateKey.error = true;
                tag.x509PrivateKey.nothexa = true;
            }
            tag.onchange();
        }

        function validate_x509ServerCert(){
            var str = tag.refs.x509ServerCert.value || tag.servercertificate;
            tag.x509ServerCert.error = false;
            tag.x509ServerCert.required = false;
            tag.x509ServerCert.nothexa = false;
            if (!str || 0 === str.length){
                  tag.x509ServerCert.error = true;
                  tag.x509ServerCert.required = true;
              }else if (! /^[0-9a-fA-F]+$/i.test(str)){
                  tag.x509ServerCert.error = true;
                  tag.x509ServerCert.nothexa = true;
            }
            tag.onchange();
        }

        function has_error(){
            console.log()
            return typeof tag.x509Cert.error === "undefined" || tag.x509Cert.error ||
                   typeof tag.x509PrivateKey.error === "undefined" || tag.x509PrivateKey.error || 
                   (tag.servercertificate === "" && (typeof tag.x509ServerCert.error === "undefined" || tag.x509ServerCert.error));
        }

        function get_value(){
            return { cert:tag.refs.x509Cert.value, key:tag.refs.x509PrivateKey.value, servCert:tag.refs.x509ServerCert.value || tag.servercertificate };
        }
    </script>
</x509-input>
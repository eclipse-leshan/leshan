<rpk-input>
    <!-- RPK inputs -->
    <div class={ form-group:true, has-error: pubkey.error }>
        <label for="pubkey" class="col-sm-4 control-label">Client public key</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="3" id="pubkey" ref="pubkey" oninput={validate_pubkey} onblur={validate_pubkey}></textarea>
            <p class="text-right text-muted small" style="margin:0">SubjectPublicKeyInfo der encoded in Hexadecimal</p>
            <p class="help-block" if={pubkey.required}>The client public key is required</p>
            <p class="help-block" if={pubkey.nothexa}>Hexadecimal format is expected</p>
        </div>
    </div>
    <div class={ form-group:true, has-error: privkey.error }>
        <label for="privkey" class="col-sm-4 control-label">Client private key</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="3" id="privkey" ref="privkey" oninput={validate_privkey} onblur={validate_privkey}></textarea>
            <p class="text-right text-muted small" style="margin:0">PKCS8 format der encoded in Hexadecimal</p>
            <p class="help-block" if={privkey.required}>The client private key is required</p>
            <p class="help-block" if={privkey.nothexa}>Hexadecimal format is expected</p>
        </div>
    </div>
    <div class={ form-group:true, has-error: servpubkey.error }>
        <label for="servpubkey" class="col-sm-4 control-label">Server public key</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="3" id="servpubkey" ref="servpubkey" oninput={validate_servpubkey} onblur={validate_servpubkey} disabled={disable.serverpubkey} placeholder={serverpubkey}></textarea>
            <p class="text-right text-muted small" style="margin:0">SubjectPublicKeyInfo der encoded in Hexadecimal</p>
            <p class="help-block" if={servpubkey.required}>The server public key is required</p>
            <p class="help-block" if={servpubkey.nothexa}>Hexadecimal format is expected</p>
        </div>
    </div>

    <script>
        // Tag definition
        var tag = this;
        // Tag Params
        tag.onchange = opts.onchange;
        tag.disable = opts.disable || {};
        tag.serverpubkey = opts.serverpubkey || "";
        // Tag API
        tag.has_error = has_error;
        tag.get_value = get_value
        // Tag intenal state
        tag.pubkey = {};
        tag.privkey = {};
        tag.servpubkey = {};
        tag.validate_pubkey = validate_pubkey;
        tag.validate_privkey = validate_privkey;
        tag.validate_servpubkey = validate_servpubkey;

        // Tag functions
        function validate_pubkey(){
            var str = tag.refs.pubkey.value;
            tag.pubkey.error = false;
            tag.pubkey.required = false;
            tag.pubkey.nothexa = false;
            if (!str || 0 === str.length){
                tag.pubkey.error = true;
                tag.pubkey.required = true;
            }else if (! /^[0-9a-fA-F]+$/i.test(str)){
                tag.pubkey.error = true;
                tag.pubkey.nothexa = true;
            }
            tag.onchange();
        }

        function validate_privkey(){
            var str = tag.refs.privkey.value;
            tag.privkey.error = false;
            tag.privkey.required = false;
            tag.privkey.nothexa = false;
            if (!str || 0 === str.length){
                tag.privkey.error = true;
                tag.privkey.required = true;
            }else if (! /^[0-9a-fA-F]+$/i.test(str)){
                tag.privkey.error = true;
                tag.privkey.nothexa = true;
            }
            tag.onchange();
        }

        function validate_servpubkey(){
            var str = tag.refs.servpubkey.value || tag.serverpubkey ;
            console.log(str);
            tag.servpubkey.error = false;
            tag.servpubkey.required = false;
            tag.servpubkey.nothexa = false;
            if (!str || 0 === str.length){
                  tag.servpubkey.error = true;
                  tag.servpubkey.required = true;
              }else if (! /^[0-9a-fA-F]+$/i.test(str)){
                  tag.servpubkey.error = true;
                  tag.servpubkey.nothexa = true;
            }
            tag.onchange();
        }

        function has_error(){
            return typeof tag.pubkey.error === "undefined" || tag.pubkey.error ||
                   typeof tag.privkey.error === "undefined" || tag.privkey.error ||
                   (tag.serverpubkey === "" && (typeof tag.servpubkey.error === "undefined" || tag.servpubkey.error));
        }

        function get_value(){
            return { pubkey:tag.refs.pubkey.value, privkey:tag.refs.privkey.value, servpubkey:tag.refs.servpubkey.value || tag.serverpubkey };
        }
    </script>
</rpk-input>
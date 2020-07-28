<oscore-input>
    <!-- OSCORE inputs -->
    <div class={ form-group:true, has-error: masterSecret.error }>
        <label for="masterSecret" class="col-sm-4 control-label">Master Secret</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="2" id="masterSecret" ref="masterSecret" oninput={validate_masterSecret} onblur={validate_masterSecret}></textarea>
            <p class="text-right text-muted small" style="margin:0">Hexadecimal format</p>
            <p class="help-block" if={masterSecret.required}>The master secret is required</p>
            <p class="help-block" if={masterSecret.nothexa}>Hexadecimal format is expected</p>
            <p class="help-block" if={masterSecret.toolong}>The master secret is too long</p>
        </div>
    </div>
    
    <div class={ form-group:true, has-error: masterSalt.error }>
        <label for="masterSalt" class="col-sm-4 control-label">Master Salt</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="2" id="masterSalt" ref="masterSalt" oninput={validate_masterSalt} onblur={validate_masterSalt}></textarea>
            <p class="text-right text-muted small" style="margin:0">Hexadecimal format</p>
            <p class="help-block" if={masterSalt.nothexa}>Hexadecimal format is expected</p>
            <p class="help-block" if={masterSalt.toolong}>The master salt is too long</p>
        </div>
    </div>

    <div class={ form-group:true, has-error: senderId.error }>
        <label for="senderId" class="col-sm-4 control-label">Sender ID</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="1" id="senderId" ref="senderId" oninput={validate_senderId} onblur={validate_senderId}></textarea>
            <p class="text-right text-muted small" style="margin:0">Hexadecimal format</p>
            <p class="help-block" if={senderId.nothexa}>Hexadecimal format is expected</p>
            <p class="help-block" if={senderId.toolong}>The sender ID is too long</p>
        </div>
    </div>

    <div class={ form-group:true, has-error: recipientId.error }>
        <label for="recipientId" class="col-sm-4 control-label">Recipient ID</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="1" id="recipientId" ref="recipientId" oninput={validate_recipientId} onblur={validate_recipientId}></textarea>
            <p class="text-right text-muted small" style="margin:0">Hexadecimal format</p>
            <p class="help-block" if={recipientId.nothexa}>Hexadecimal format is expected</p>
            <p class="help-block" if={recipientId.toolong}>The recipient ID is too long</p>
        </div>
    </div>
    
    <div class={ form-group:true, has-error: aeadAlgorithm.error }>
        <label for="aeadAlgorithm" class="col-sm-4 control-label">AEAD Algorithm</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="1" id="aeadAlgorithm" ref="aeadAlgorithm" oninput={validate_aeadAlgorithm} onblur={validate_aeadAlgorithm} placeholder={defaultAeadAlgorithm}></textarea>
            <p class="help-block" if={aeadAlgorithm.toolong}>The AEAD algorithm is too long</p>
        </div>
    </div>
    
    <div class={ form-group:true, has-error: hkdfAlgorithm.error }>
        <label for="hkdfAlgorithm" class="col-sm-4 control-label">HKDF Algorithm</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="1" id="hkdfAlgorithm" ref="hkdfAlgorithm" oninput={validate_hkdfAlgorithm} onblur={validate_hkdfAlgorithm} placeholder={defaultHkdfAlgorithm}></textarea>
            <p class="help-block" if={hkdfAlgorithm.toolong}>The HKDF algorithm is too long</p>
        </div>
    </div>

    <script>
        // Tag definition
        var tag = this;
        // Tag Params
        tag.onchange = opts.onchange;
        // Tag API
        tag.has_error = has_error;
        tag.get_value = get_value
        // Tag internal state
        tag.masterSecret={};
        tag.masterSalt={};
        tag.senderId={};
        tag.recipientId={};
        tag.aeadAlgorithm={};
        tag.defaultAeadAlgorithm = "AES_CCM_16_64_128";
        tag.hkdfAlgorithm={};
        tag.defaultHkdfAlgorithm = "HKDF_HMAC_SHA_256";
        tag.validate_masterSecret = validate_masterSecret;
        tag.validate_masterSalt = validate_masterSalt;
        tag.validate_senderId = validate_senderId;
        tag.validate_recipientId = validate_recipientId;
        tag.validate_aeadAlgorithm = validate_aeadAlgorithm;
        tag.validate_hkdfAlgorithm = validate_hkdfAlgorithm;

        // Tag functions
        function validate_masterSecret(e){
            var str = tag.refs.masterSecret.value;
            tag.masterSecret.error = false;
            tag.masterSecret.required = false;
            tag.masterSecret.toolong = false;
            tag.masterSecret.nothexa = false;
            if (!str || 0 === str.length){
                tag.masterSecret.error = true;
                tag.masterSecret.required = true;
            }else if (str.length > 64){
                tag.masterSecret.error = true;
                tag.masterSecret.toolong = true;
            }else if (! /^[0-9a-fA-F]+$/i.test(str)){
                tag.masterSecret.error = true;
                tag.masterSecret.nothexa = true;
            }
            tag.onchange();
        }
        
        function validate_masterSalt(e){
            var str = tag.refs.masterSalt.value;
            tag.masterSalt.error = false;
            tag.masterSalt.toolong = false;
            tag.masterSalt.nothexa = false;
            var isEmpty = !str || 0 === str.length;
            if (str.length > 64){
                tag.masterSalt.error = true;
                tag.masterSalt.toolong = true;
            }else if (!isEmpty && ! /^[0-9a-fA-F]+$/i.test(str)){
                tag.masterSalt.error = true;
                tag.masterSalt.nothexa = true;
            }
            tag.onchange();
        }
        
        function validate_senderId(e){
            var str = tag.refs.senderId.value;
            tag.senderId.error = false;
            tag.senderId.toolong = false;
            tag.senderId.nothexa = false;
            var isEmpty = !str || 0 === str.length;
            if (str.length > 16){
                tag.senderId.error = true;
                tag.senderId.toolong = true;
            }else if (!isEmpty && ! /^[0-9a-fA-F]+$/i.test(str)){
                tag.senderId.error = true;
                tag.senderId.nothexa = true;
            }
            tag.onchange();
        }
        
        function validate_recipientId(e){
            var str = tag.refs.recipientId.value;
            tag.recipientId.error = false;
            tag.recipientId.toolong = false;
            tag.recipientId.nothexa = false;
            var isEmpty = !str || 0 === str.length;
            if (str.length > 16){
                tag.recipientId.error = true;
                tag.recipientId.toolong = true;
            }else if (!isEmpty && ! /^[0-9a-fA-F]+$/i.test(str)){
                tag.recipientId.error = true;
                tag.recipientId.nothexa = true;
            }
            tag.onchange();
        }
        
        function validate_aeadAlgorithm(e){
            var str = tag.refs.aeadAlgorithm.value;
            tag.aeadAlgorithm.error = false;
            tag.aeadAlgorithm.toolong = false;
            if (str.length > 32){
                tag.aeadAlgorithm.error = true;
                tag.aeadAlgorithm.toolong = true;
            }
            tag.onchange();
        }
        
        function validate_hkdfAlgorithm(e){
            var str = tag.refs.hkdfAlgorithm.value;
            tag.hkdfAlgorithm.error = false;
            tag.hkdfAlgorithm.toolong = false;
            if (str.length > 32){
                tag.hkdfAlgorithm.error = true;
                tag.hkdfAlgorithm.toolong = true;
            }
            tag.onchange();
        }

        function has_error(){
            return  typeof tag.masterSecret.error === "undefined" || tag.masterSecret.error
            || tag.masterSalt.error
            || tag.senderId.error
            || tag.recipientId.error
            || tag.aeadAlgorithm.error
            || tag.hkdfAlgorithm.error;
        }

        // Allows entering the AEAD algorithm as a string, and sets default if empty
        function parse_aeadAlgorithm(alg){

            if (!alg || 0 === alg.length){
                alg = tag.defaultAeadAlgorithm;
            }

            switch(alg) {
                case 'AES_CCM_16_64_128':
                    return 10;
                case 'AES_CCM_64_64_128':
                    return 12;
                case 'AES_CCM_16_128_128':
                    return 30;
                case 'AES_CCM_64_128_128':
                    return 32;
                default:
                    return alg;
            }
        }

        // Allows entering the HKDF algorithm as a string, and sets default if empty
        function parse_hkdfAlgorithm(alg){

            if (!alg || 0 === alg.length){
                alg = tag.defaultHkdfAlgorithm;
            }

            switch(alg) {
                case 'HKDF_HMAC_SHA_256':
                    return -10;
                default:
                    return alg;
            }
        }

        function get_value(){
            return { masterSecret:tag.refs.masterSecret.value,
                masterSalt:tag.refs.masterSalt.value,
                senderId:tag.refs.senderId.value,
                recipientId:tag.refs.recipientId.value,
                aeadAlgorithm:parse_aeadAlgorithm(tag.refs.aeadAlgorithm.value),
                hkdfAlgorithm:parse_hkdfAlgorithm(tag.refs.hkdfAlgorithm.value) };
        }
    </script>
</oscore-input>


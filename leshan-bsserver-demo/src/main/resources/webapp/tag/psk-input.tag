<psk-input>
    <!-- PSK inputs -->
    <div class={ form-group:true, has-error: pskId.error }>
        <label for="pskId" class="col-sm-4 control-label">Identity</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="3" id="pskId" ref="pskId" oninput={validate_pskId} onblur={validate_pskId}></textarea>
            <p class="help-block" if={pskId.required} >The PSK identity is required</p>
            <p class="help-block" if={pskId.toolong} >The PSK identity is too long</p>
        </div>
    </div>

    <div class={ form-group:true, has-error: pskVal.error }>
        <label for="pskVal" class="col-sm-4 control-label">Key</label>
        <div class="col-sm-8">
            <textarea class="form-control" style="resize:none" rows="3" id="pskVal" ref="pskVal" oninput={validate_pskVal} onblur={validate_pskVal}></textarea>
            <p class="text-right text-muted small" style="margin:0">Hexadecimal format</p>
            <p class="help-block" if={pskVal.required}>The pre-shared key is required</p>
            <p class="help-block" if={pskVal.nothexa}>Hexadecimal format is expected</p>
            <p class="help-block" if={pskVal.toolong}>The pre-shared key is too long</p>
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
        // Tag intenal state
        tag.pskId={};
        tag.pskVal={};
        tag.validate_pskId = validate_pskId;
        tag.validate_pskVal = validate_pskVal;

        // Tag functions
        function validate_pskId(e){
            var str = tag.refs.pskId.value; 
            tag.pskId.error = false;
            tag.pskId.required = false;
            tag.pskId.toolong = false;
            if (!str || 0 === str.length){
                tag.pskId.error = true;
                tag.pskId.required = true;
            }else if (str.length > 128){
                tag.pskId.error = true;
                tag.pskId.toolong = true;
            }
            tag.onchange();
        }

        function validate_pskVal(e){
            var str = tag.refs.pskVal.value;
            tag.pskVal.error = false;
            tag.pskVal.required = false;
            tag.pskVal.toolong = false;
            tag.pskVal.nothexa = false;
            if (!str || 0 === str.length){
                tag.pskVal.error = true;
                tag.pskVal.required = true;
            }else if (str.length > 128){
                tag.pskVal.error = true;
                tag.pskVal.toolong = true;
            }else if (! /^[0-9a-fA-F]+$/i.test(str)){
                tag.pskVal.error = true;
                tag.pskVal.nothexa = true;
            }
            tag.onchange();
        }

        function has_error(){
            return  typeof tag.pskId.error === "undefined" || tag.pskId.error || typeof tag.pskVal.error === "undefined" || tag.pskVal.error;
        }

        function get_value(){
            return { id:tag.refs.pskId.value, key:tag.refs.pskVal.value };
        }
    </script>
</psk-input>


<bootstrap-modal>
    <div class="modal bs-example-modal-sm" id="bootstrap-modal" tabindex="-1" role="dialog" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                    <h4 class="modal-title">New Bootstrap Configuration</h4>
                </div>
                <div class="modal-body">
                    <form class="form form-horizontal" name="form">
                        <div class={ form-group:true, has-error: endpoint.error } >
                            <label for="endpoint" class="col-sm-4 control-label">Client endpoint</label>
                            <div class="col-sm-8">
                                <input class="form-control" id="endpoint" oninput={validate_endpoint} onblur={validate_endpoint} >
                                <p class="help-block">The endpoint is required</p>
                            </div>
                        </div>

                        <div class="form-group" >
                            <label for="dmUrl" class="col-sm-4 control-label">LWM2M Server URL</label>
                            <div class="col-sm-8">
                                <input class="form-control" id="dmUrl" placeholder={default_uri()}>
                            </div>
                        </div>

                        <div class="form-group">
                            <label for="secMode" class="col-sm-4 control-label">Security mode</label>
                            <div class="col-sm-8">
                                <select class="form-control" id="secMode" onchange={update}>
                                    <option value="no_sec">No Security</option>
                                    <option value="psk">Pre-Shared Key</option>
                                    <!-- option value="rpk">Raw Public Key (Elliptic Curves)</option-->
                                </select>
                            </div>
                        </div>

                        <!-- PSK inputs -->
                        <div class={ form-group:true, has-error: pskId.error } if={ secMode.value == "psk"}>
                            <label for="pskId" class="col-sm-4 control-label">Identity</label>
                            <div class="col-sm-8">
                                <textarea class="form-control" style="resize:none" rows="3" id="pskId" oninput={validate_pskId} onblur={validate_pskId}></textarea>
                                <p class="help-block" if={pskId.required} >The PSK identity is required</p>
                                <p class="help-block" if={pskId.toolong} >The PSK identity is too long</p>
                            </div>
                        </div>

                        <div class={ form-group:true, has-error: pskVal.error } if={ secMode.value == "psk"}>
                            <label for="pskVal" class="col-sm-4 control-label">Key</label>
                            <div class="col-sm-8">
                                <textarea class="form-control" style="resize:none" rows="3" id="pskVal" oninput={validate_pskVal} onblur={validate_pskVal}></textarea>
                                <p class="text-right text-muted small" style="margin:0">Hexadecimal format</p>
                                <p class="help-block" if={pskVal.required}>The pre-shared key is required</p>
                                <p class="help-block" if={pskVal.nothexa}>Hexadecimal format is expected</p>
                                <p class="help-block" if={pskVal.toolong}>The pre-shared key is too long</p>
                            </div>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
                    <button type="button" class="btn btn-primary" disabled={has_error()} onclick={submit}>Create</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        var self = this;
        this.on('mount', function() {
            $('#bootstrap-modal').modal('show');
            this.update();
        });

        default_uri(){
            if (secMode.value === "no_sec")
                return "coap://leshan.eclipse.org:5683";
            else
                return "coaps://leshan.eclipse.org:5684";
        }

        has_error(){
            var endpoint_has_error = (endpoint.error === undefined || endpoint.error);
            var secMode_has_error = secMode.value === "psk" && (typeof pskId.error === "undefined" || pskId.error || typeof pskVal.error === "undefined" || pskVal.error);
            return endpoint_has_error || secMode_has_error;
        }

        validate_endpoint(){
            var str = endpoint.value;
            if (!str || 0 === str.length){
                endpoint.error = true;
                has_error = true;
            }else{
                endpoint.error = false;
                has_error = false;
            }
        }

        validate_pskId(){
            var str = pskId.value;
            pskId.error = false;
            pskId.required = false;
            pskId.toolong = false;
            if (secMode.value === "psk"){
                if (!str || 0 === str.length){
                    pskId.error = true;
                    pskId.required = true;
                }else if (str.length > 128){
                    pskId.error = true;
                    pskId.toolong = true;
                }
            }
        }

        validate_pskVal(){
            var str = pskVal.value;
            pskVal.error = false;
            pskVal.required = false;
            pskVal.toolong = false;
            pskVal.nothexa = false;
            if (secMode.value === "psk"){
                if (!str || 0 === str.length){
                    pskVal.error = true;
                    pskVal.required = true;
                }else if (str.length > 128){
                    pskVal.error = true;
                    pskVal.toolong = true;
                }else if (! /^[0-9a-fA-F]+$/i.test(str)){
                    pskVal.error = true;
                    pskVal.nothexa = true;
                }
            }
        }

        fromAscii(ascii){
            var bytearray = [];
            for (var i in ascii){
                bytearray[i] = ascii.charCodeAt(i);
            }
            return bytearray;
        };

        fromHex(hex){
            var bytes = [];
            for(var i=0; i< hex.length-1; i+=2) {
                bytes.push(parseInt(hex.substr(i, 2), 16));
            }
            return bytes;
        };

        submit(){
            var id = [];
            var key = [];
            if (secMode.value === "psk"){
                id = self.fromAscii(pskId.value);
                key = self.fromHex(pskVal.value);
                console.log(key)
            }
            var uri
            if (!dmUrl.value || dmUrl.value.length == 0){
                uri = self.default_uri();
            }else{
                uri = dmUrl.value;
            }
            bsConfigStore.add(endpoint.value, {
                 dm:[{
                    binding : "U",
                    defaultMinPeriod : 1,
                    lifetime : 20,
                    notifIfDisabled : true,
                    shortId : 123,
                    security : {
                        bootstrapServer : false,
                        clientOldOffTime : 1,
                        publicKeyOrId : id,
                        secretKey : key,
                        securityMode : secMode.value.toUpperCase(),
                        serverId : 123,
                        serverPublicKeyOrId : [  ],
                        serverSmsNumber : "",
                        smsBindingKeyParam : [  ],
                        smsBindingKeySecret : [  ],
                        smsSecurityMode : "NO_SEC",
                        uri : uri
                      }
                }],
                 bs:[{
                    security : {
                        bootstrapServer : true,
                        clientOldOffTime : 1,
                        publicKeyOrId : [],
                        secretKey : [],
                        securityMode : "NO_SEC",
                        serverId : 111,
                        serverPublicKeyOrId : [  ],
                        serverSmsNumber : "",
                        smsBindingKeyParam : [  ],
                        smsBindingKeySecret : [  ],
                        smsSecurityMode : "NO_SEC",
                        uri : "coap://"+location.hostname+":5683",
                      }
                }]
            });
            $('#bootstrap-modal').modal('hide');
            return false;
        }
    </script>

    <style>
        .form-group .help-block {
            display: none;
        }

        .form-group.has-error .help-block {
            display: block;
        }
    </style>
</bootstrap-modal>

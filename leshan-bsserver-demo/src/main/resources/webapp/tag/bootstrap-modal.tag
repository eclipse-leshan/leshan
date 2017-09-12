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
                                    <option value="x509">X.509 Certificate</option>
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

                        <!-- X.509 inputs -->
                        <div class={ form-group:true, has-error: x509Cert.error } if={ secMode.value == "x509"}>
                            <label for="x509Cert" class="col-sm-4 control-label">Client certificate</label>
                            <div class="col-sm-8">
                                <textarea class="form-control" style="resize:none" rows="3" id="x509Cert" oninput={validate_x509Cert} onblur={validate_x509Cert}></textarea>
                                <p class="text-right text-muted small" style="margin:0">Hexadecimal format</p>
                                <p class="help-block" if={x509Cert.required}>The client certificate is required</p>
                                <p class="help-block" if={x509Cert.nothexa}>Hexadecimal format is expected</p>
                            </div>
                        </div>
                        <div class={ form-group:true, has-error: x509PrivateKey.error } if={ secMode.value == "x509"}>
                            <label for="x509PrivateKey" class="col-sm-4 control-label">Client private key</label>
                            <div class="col-sm-8">
                                <textarea class="form-control" style="resize:none" rows="3" id="x509PrivateKey" oninput={validate_x509PrivateKey} onblur={validate_x509PrivateKey}></textarea>
                                <p class="text-right text-muted small" style="margin:0">Hexadecimal format</p>
                                <p class="help-block" if={x509PrivateKey.required}>The client private key is required</p>
                                <p class="help-block" if={x509PrivateKey.nothexa}>Hexadecimal format is expected</p>
                            </div>
                        </div>
                        <div class={ form-group:true, has-error: x509ServerCert.error } if={ secMode.value == "x509"}>
                            <label for="x509ServerCert" class="col-sm-4 control-label">Server certificate</label>
                            <div class="col-sm-8">
                                <textarea class="form-control" style="resize:none" rows="3" id="x509ServerCert" oninput={validate_x509ServerCert} onblur={validate_x509ServerCert}></textarea>
                                <p class="text-right text-muted small" style="margin:0">Hexadecimal format</p>
                                <p class="help-block" if={x509ServerCert.required}>The server certificate is required</p>
                                <p class="help-block" if={x509ServerCert.nothexa}>Hexadecimal format is expected</p>
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
	        $.get('api/server', function(data) {
	            self.server = data;
	            $('#bootstrap-modal').modal('show');
            	this.update();
	        }).fail(function(xhr, status, error){
	            var err = "Unable to get the server info";
	            console.error(err, status, error, xhr.responseText);
	            alert(err + ": " + xhr.responseText);
	        });
        });

        default_uri(){
            if (secMode.value === "no_sec")
                return "coap://"+location.hostname+":5683";
            else
                return "coaps://"+location.hostname+":5684";
        }

        has_error(){
            var endpoint_has_error = (endpoint.error === undefined || endpoint.error);
            var psk_has_error = secMode.value === "psk" && (typeof pskId.error === "undefined" || pskId.error || typeof pskVal.error === "undefined" || pskVal.error);
            var x509_has_error = secMode.value === "x509" && (typeof x509Cert.error === "undefined" || x509Cert.error || typeof x509PrivateKey.error === "undefined" || x509PrivateKey.error || typeof x509ServerCert.error === "undefined" || x509ServerCert.error);
            return endpoint_has_error || psk_has_error || x509_has_error;
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

        validate_x509Cert(){
            var str = x509Cert.value;
            x509Cert.error = false;
            x509Cert.required = false;
            x509Cert.nothexa = false;
            if (secMode.value === "x509"){
                if (!str || 0 === str.length){
                    x509Cert.error = true;
                    x509Cert.required = true;
                }else if (! /^[0-9a-fA-F]+$/i.test(str)){
                    x509Cert.error = true;
                    x509Cert.nothexa = true;
                }
            }
        }

        validate_x509PrivateKey(){
            var str = x509PrivateKey.value;
            x509PrivateKey.error = false;
            x509PrivateKey.required = false;
            x509PrivateKey.nothexa = false;
            if (secMode.value === "x509"){
                if (!str || 0 === str.length){
                    x509PrivateKey.error = true;
                    x509PrivateKey.required = true;
                }else if (! /^[0-9a-fA-F]+$/i.test(str)){
                    x509PrivateKey.error = true;
                    x509PrivateKey.nothexa = true;
                }
            }
        }

        validate_x509ServerCert(){
            var str = x509ServerCert.value;
            x509ServerCert.error = false;
            x509ServerCert.required = false;
            x509ServerCert.nothexa = false;
            if (secMode.value === "x509"){
                if (!str || 0 === str.length){
                    x509ServerCert.error = true;
                    x509ServerCert.required = true;
                }else if (! /^[0-9a-fA-F]+$/i.test(str)){
                    x509ServerCert.error = true;
                    x509ServerCert.nothexa = true;
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
            var serverKey = [];
            if (secMode.value === "psk"){
                id = self.fromAscii(pskId.value);
                key = self.fromHex(pskVal.value);
            }else if(secMode.value === "x509"){
                id = self.fromHex(x509Cert.value);
                key = self.fromHex(x509PrivateKey.value);
                serverKey = self.fromHex(x509ServerCert.value);
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
                        serverPublicKey : serverKey,
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
                        serverPublicKey : [  ],
                        serverSmsNumber : "",
                        smsBindingKeyParam : [  ],
                        smsBindingKeySecret : [  ],
                        smsSecurityMode : "NO_SEC",
                        uri : "coap://"+location.hostname+":"+self.server.unsecuredEndpointPort,
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

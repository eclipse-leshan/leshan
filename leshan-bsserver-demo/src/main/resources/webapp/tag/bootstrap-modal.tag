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
                                <input class="form-control" id="endpoint" ref="endpoint" oninput={validate_endpoint} onblur={validate_endpoint} >
                                <p class="help-block">The endpoint is required</p>
                            </div>
                        </div>

                        <ul class="nav nav-tabs nav-justified">
                            <li role="presentation" class={ active:activetab.lwserver }><a href="javascript:void(0);" onclick={activetab.activelwserver}>LWM2M Server</a></li>
                            <li role="presentation" class={ active:activetab.bsserver }><a href="javascript:void(0);" onclick={activetab.activebsserver}>LWM2M Bootstrap Server</a></li>
                        </ul>
                        </br>

                        <div>
                            <securityconfig-input   ref="lwserver" onchange={update} show={activetab.lwserver}
                                                    securi={ "coaps://" + location.hostname + ":5684" }
                                                    unsecuri= { "coap://" + location.hostname + ":5683" }
                                                    secmode = { {no_sec:true, psk:true, rpk:true, x509:true, oscore:true} }
                                                    ></securityconfig-input>
                        </div>
                        <div>
                             <securityconfig-input ref="bsserver" onchange={update} show={activetab.bsserver}
                                                    securi={ "coaps://" + location.hostname + ":" + serverdata.securedEndpointPort }
                                                    unsecuri= { "coap://" + location.hostname + ":" + serverdata.unsecuredEndpointPort }
                                                    serverpubkey= {serversecurity.rpk.hexDer}
                                                    servercertificate= {serversecurity.certificate.hexDer}
                                                    disable = { {uri:true, serverpubkey:true, servercertificate:true}}
                                                    secmode = { {no_sec:true, psk:true,rpk:true, x509:true, oscore:true}}
                                                    ></securityconfig-input>
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
        // Tag definition
        var tag = this;
        // Tag Params
        tag.serverdata = opts.server || {unsecuredEndpointPort:5683, securedEndpointPort:5684};
        tag.serversecurity = opts.security || {rpk:{},certificate:{}};
        // Tag Internal state
        tag.endpoint = {};
        tag.has_error = has_error;
        tag.validate_endpoint = validate_endpoint;
        tag.submit = submit;

        // Tabs management
        tag.activetab = {
                lwserver:true,
                bsserver:false,
                activelwserver:function(){ tag.activetab.lwserver=true; tag.activetab.bsserver = false; },
                activebsserver:function(){ tag.activetab.lwserver=false; tag.activetab.bsserver = true; }
        };

        // Initialization
        tag.on('mount', function() {
                $('#bootstrap-modal').modal('show');
        });

        // Tag functions
        function has_error(){
            var endpoint_has_error = (tag.endpoint.error === undefined || tag.endpoint.error);
            return endpoint_has_error || tag.refs.lwserver.has_error() || tag.refs.bsserver.has_error();
        }

        function validate_endpoint(){
            var str = tag.refs.endpoint.value;
            if (!str || 0 === str.length){
                tag.endpoint.error = true;
            }else{
                tag.endpoint.error = false;
            }
        }

        function submit(){
            var lwserver = tag.refs.lwserver.get_value()
            var bsserver = tag.refs.bsserver.get_value()
            
            if(bsserver.secmode === "OSCORE") {
                var bsserverOscore = bsserver.oscore;
                var bsOscore =
                {
                    oscoreMasterSecret : bsserverOscore.masterSecret,
                    oscoreSenderId : bsserverOscore.senderId,
                    oscoreRecipientId : bsserverOscore.recipientId,
                    oscoreAeadAlgorithm : bsserverOscore.aeadAlgorithm,
                    oscoreHmacAlgorithm : bsserverOscore.hkdfAlgorithm,
                    oscoreMasterSalt : bsserverOscore.masterSalt,
                }
                var bsOscoreSecurityMode = 0; // link to bs oscore object
                bsserver.secmode = "NO_SEC"; // act as no_sec from here
            }

            if(lwserver.secmode === "OSCORE") {
                var lwserverOscore = lwserver.oscore;
                var dmOscore =
                {
                    oscoreMasterSecret : lwserverOscore.masterSecret,
                    oscoreSenderId : lwserverOscore.senderId,
                    oscoreRecipientId : lwserverOscore.recipientId,
                    oscoreAeadAlgorithm : lwserverOscore.aeadAlgorithm,
                    oscoreHmacAlgorithm : lwserverOscore.hkdfAlgorithm,
                    oscoreMasterSalt : lwserverOscore.masterSalt,
                }
                var dmOscoreSecurityMode = 1; // link to dm oscore object
                lwserver.secmode = "NO_SEC"; // act as no_sec from here
            }

            // add config to the store
            bsConfigStore.add(endpoint.value, {
                 dm:[{
                    binding : "U",
                    defaultMinPeriod : 1,
                    lifetime : 300,
                    notifIfDisabled : true,
                    shortId : 123,
                    security : {
                        bootstrapServer : false,
                        certificateUsage: lwserver.certificateUsage,
                        clientOldOffTime : 1,
                        publicKeyOrId : lwserver.id,
                        secretKey : lwserver.key,
                        securityMode : lwserver.secmode,
                        serverId : 123,
                        serverPublicKey : lwserver.serverKey,
                        serverSmsNumber : "",
                        smsBindingKeyParam : [  ],
                        smsBindingKeySecret : [  ],
                        smsSecurityMode : "NO_SEC",
                        uri : lwserver.uri,
                        oscoreSecurityMode : dmOscoreSecurityMode
                      },
                      oscore : dmOscore
                }],
                 bs:[{
                    security : {
                        bootstrapServer : true,
                        certificateUsage: bsserver.certificateUsage,
                        clientOldOffTime : 1,
                        publicKeyOrId : bsserver.id,
                        secretKey : bsserver.key,
                        securityMode : bsserver.secmode,
                        serverPublicKey : bsserver.serverKey,
                        serverSmsNumber : "",
                        smsBindingKeyParam : [  ],
                        smsBindingKeySecret : [  ],
                        smsSecurityMode : "NO_SEC",
                        uri : bsserver.uri,
                        oscoreSecurityMode : bsOscoreSecurityMode
                      },
                      oscore : bsOscore
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

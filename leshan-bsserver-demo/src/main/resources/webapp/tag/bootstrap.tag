<bootstrap>
   <div class="well well-sm col-md-12"  if={certificate}>
        <h4>The Leshan Bootstrap Certificate  <small>(x509v3 der encoded)</small>
        <button type="button" title ="Download bootstrap server certificate(.der)" class="btn btn-default btn-xs" onclick={()=>saveFile('bsServerCertificate.der',certificate.bytesDer)}>
            <span class="glyphicon glyphicon-download-alt" aria-hidden="true">
        </button>
        </h4>
        <p>
           <div class="col-md-7">
           <u>Hex : </u> <pre>{certificate.hexDer}</pre>
           </div>
           <div class="col-md-5">
           <u>Base64 : </u><pre>{certificate.b64Der}</pre>
           </div>
           <small>Clients generally need it for X509 authentication.</small><br/>
        </p>
    </div>
    <div class="well well-sm col-md-12"  if={pubkey}>
        <h4>The Leshan Bootstrap Public Key <small>(SubjectPublicKeyInfo der encoded)</small>
        <button type="button" title ="Download server public key(.der)" class="btn btn-default btn-xs" onclick={()=>saveFile('bsServerPubKey.der',pubkey.bytesDer)}>
            <span class="glyphicon glyphicon-download-alt" aria-hidden="true">
        </button>
        </h4>
        <p>
           <u>Elliptic Curve parameters :</u> <code>{pubkey.params}</code><br/>
           <u>Public x coord :</u> <code>{pubkey.x}</code><br/>
           <u>Public y coord :</u> <code>{pubkey.y}</code><br/>
           <div class="col-md-7">
           <u>Hex : </u> <pre>{pubkey.hexDer}</pre>
           </div>
           <div class="col-md-5">
           <u>Base64 : </u><pre>{pubkey.b64Der}</pre>
           </div>
           <small>Clients generally need it for RPK authentication.</small>
        </p>
    </div>
    <div>
        <button class="btn btn-default center-block" onclick={showModal}>
            Add new client bootstrap configuration
        </button>
    </div>

    <div class="table-responsive">
        <table class="table table-striped bootstrap-table">
        <thead>
            <tr>
                <th>Client Endpoint</th>
                <th>LWM2M Bootstrap Server</th>
                <th>LWM2M Server</th>
                <th></th>
            </tr>
        </thead>
        <tbody>
            <tr each={ config, endpoint in configs }>
                <td>{ endpoint }</td>
                <td>
                    <div each={ config.bs }>
                        <p>
                            <strong>{ security.uri }</strong><br/>
                            security mode : {security.securityMode}<br/>
                            <span if={security.securityMode === 'PSK'}>
                                Identity : <code code style=display:block;white-space:pre-wrap>{wrap(toAscii(security.publicKeyOrId))}</code>
                                Key : <code code style=display:block;white-space:pre-wrap>{wrap(toHex(security.secretKey))}</code>
                            </span>
                        </p>
                    </div>
                </td>
                <td>
                    <div each={ config.dm }>
                        <p>
                            <strong>{security.uri}</strong><br/>
                            security mode : {security.securityMode}<br/>
                            <span if={security.securityMode === 'PSK'}>
                                Identity : <code code style=display:block;white-space:pre-wrap>{wrap(toAscii(security.publicKeyOrId))}</code>
                                key : <code code style=display:block;white-space:pre-wrap>{wrap(toHex(security.secretKey))}</code>
                            </span>
                        </p>
                    </div>
                </td>
                <td><button type="button" class="btn btn-default btn-xs" onclick={parent.remove}>
                        <span class="glyphicon glyphicon-remove"></button></td>
            </tr>
        <tbody>
        </table>
    </div>

    <div id='modal'></div>

    <script>
        // Tag definition
        var tag = this;
        // internal state;
        tag.remove = remove;
        tag.showModal = showModal;
        tag.toAscii = toAscii;
        tag.toHex = toHex;
        tag.wrap = wrap;
        tag.saveFile = saveFile;
        tag.pubkey = null // .b64Der .hexDer .bytesDer fields
        tag.certificate = null // .b64Der .hexDer .bytesDer fields

        // Tag initilialization
        tag.on('mount', function(){
            server.init();
            bsConfigStore.init();
        });

        bsConfigStore.on("changed", function(configs){
            tag.configs = configs;
            tag.update();
        });

        server.on("initialized", function(securityInfo){
            if (securityInfo.certificate){
                tag.certificate = securityInfo.certificate
                tag.certificate.bytesDer = base64ToBytes(tag.certificate.b64Der);
                tag.certificate.hexDer = toHex(tag.certificate.bytesDer);

                tag.pubkey = securityInfo.certificate.pubkey;
                tag.pubkey.bytesDer = base64ToBytes(tag.pubkey.b64Der);
                tag.pubkey.hexDer = toHex(tag.pubkey.bytesDer);
                tag.update();
            } else if (securityInfo.pubkey) {
                tag.pubkey = securityInfo.pubkey;
                tag.pubkey.bytesDer = base64ToBytes(tag.pubkey.b64Der);
                tag.pubkey.hexDer = toHex(tag.pubkey.bytesDer);
                tag.update();
            }
        });

        // Tag functions
        function showModal(){
            $.get('api/server/endpoint', function(data) {
                
                riot.mount('div#modal', 'bootstrap-modal', {server:data, security:{rpk:tag.pubkey, certificate:tag.certificate}});
            }).fail(function(xhr, status, error){
                var err = "Unable to get the server info";
                console.error(err, status, error, xhr.responseText);
                alert(err + ": " + xhr.responseText);
            });
        };

        function saveFile(filename, bytes) {
            var blob = new Blob([bytes], {type: "application/octet-stream"});
            saveAs(blob, filename);
        };

        function remove(e){
            bsConfigStore.remove(e.item.endpoint);
        }

        function toAscii(byteArray){
            var ascii = [];
            for (var i in byteArray){
                ascii[i] = String.fromCharCode(byteArray[i]);
            }
            return ascii.join('');
        };

        function toHex(byteArray){
            var hex = [];
            for (var i in byteArray){
                hex[i] = byteArray[i].toString(16).toUpperCase();
                if (hex[i].length === 1){
                    hex[i] = '0' + hex[i];
                }
            }
            return hex.join('');
        };

        function wrap(s) {
            var r = s.replace(/([^\n]{1,32})/g, '$1\n');
            return r;
        };

        // Utils
        function base64ToBytes(base64){
            var byteKey = atob(base64);
            var byteKeyLength = byteKey.length;
            var array = new Uint8Array(new ArrayBuffer(byteKeyLength));
            for(i = 0; i < byteKeyLength; i++) {
              array[i] = byteKey.charCodeAt(i);
            }
            return array;
        }
    </script>
    <style>
        pre {
            word-wrap: break-word;
            word-break: break-all;
            white-space: pre-wrap;
        }
    </style>
</bootstrap>

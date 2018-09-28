<bootstrap>
    <div class="well well-sm col-md-12"  if={serverSecurityInfo.rpk}>
        <h4>The Leshan bootstrap public key <small>(SubjectPublicKeyInfo der encoded)</small>
        <button type="button" title ="Download server public key(.der)" class="btn btn-default btn-xs" onclick={saveServerPubKey}>
            <span class="glyphicon glyphicon-download-alt" aria-hidden="true">
        </button>
        </h4>
        <p>
           <u>Elliptic Curve parameters :</u> <code>{serverSecurityInfo.rpk.params}</code><br/>
           <u>Public x coord :</u> <code>{serverSecurityInfo.rpk.x}</code><br/>
           <u>Public y coord :</u> <code>{serverSecurityInfo.rpk.y}</code><br/>
           <div class="col-md-7">
           <u>Hex : </u> <pre>{pkcs8pubkey.hex}</pre>
           </div>
           <div class="col-md-5">
           <u>Base64 : </u><pre>{pkcs8pubkey.base64}</pre>
           </div>
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
                                Id : <code> {toAscii(security.publicKeyOrId)} </code> </br>
                                secret : <code> {toHex(security.secretKey)} </code> </br>
                            </span>
                            <span if={security.securityMode === 'X509'}>
                                Client certificate : <code> {toHex(security.publicKeyOrId)} </code> </br>
                                Client private key : <code> {toHex(security.secretKey)} </code> </br>
                                Server certificate : <code> {toHex(security.serverPublicKey)} </code> </br>
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
                                Id : <code> {toAscii(security.publicKeyOrId)} </code> </br>
                                secret : <code> {toHex(security.secretKey)} </code> </br>
                            </span>
                            <span if={security.securityMode === 'X509'}>
                                Client certificate : <code> {toHex(security.publicKeyOrId)} </code> </br>
                                Client private key : <code> {toHex(security.secretKey)} </code> </br>
                                Server certificate : <code> {toHex(security.serverPublicKey)} </code> </br>
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
        tag.saveServerPubKey = saveServerPubKey;
        tag.serverSecurityInfo = {};
        tag.pkcs8pubkey = {} // .base64 .hex .bytes fields

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
            tag.serverSecurityInfo = securityInfo;
            tag.pkcs8pubkey.base64 = securityInfo.rpk.pkcs8;
            tag.pkcs8pubkey.bytes = base64ToBytes(tag.pkcs8pubkey.base64);
            tag.pkcs8pubkey.hex = toHex(tag.pkcs8pubkey.bytes);
            tag.update();
        });

        // Tag functions
        function showModal(){
            $.get('api/server/endpoint', function(data) {
                
                riot.mount('div#modal', 'bootstrap-modal', {server:data, security:{rpk:tag.pkcs8pubkey}});
            }).fail(function(xhr, status, error){
                var err = "Unable to get the server info";
                console.error(err, status, error, xhr.responseText);
                alert(err + ": " + xhr.responseText);
            });
        };

        function saveServerPubKey(){
            var blob = new Blob([tag.pkcs8pubkey.bytes], {type: "application/octet-stream"});
            var fileName = "bsServerPubKey.der";
            saveAs(blob, fileName);
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

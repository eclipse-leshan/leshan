<bootstrap>
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

        // Tag initilialization
        tag.on('mount',function(){
            bsConfigStore.init();
        });

        bsConfigStore.on("changed", function(configs){
            tag.configs = configs;
            tag.update();
        });

        // Tag functions
        function showModal(){
            $.get('api/server/endpoint', function(data) {
                riot.mount('div#modal', 'bootstrap-modal', {server:data});
            }).fail(function(xhr, status, error){
                var err = "Unable to get the server info";
                console.error(err, status, error, xhr.responseText);
                alert(err + ": " + xhr.responseText);
            });
            
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
    </script>
</bootstrap>

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
            <tr each={ endpoint, config in configs }>
                <td>{ endpoint }</td>
                <td>
                    <div each={ config.bs }>
                        <p>
                            <strong>{ security.uri }</strong><br/>
                            security mode : {security.securityMode}<br/>
                            <span if={security.securityMode !== 'NO_SEC'}>
                                Id : <code> {toAscii(security.publicKeyOrId)} </code> </br>
                                secret : <code> {toHex(security.secretKey)} </code> </br>
                            </span>
                        </p>
                    </div>
                </td>
                <td>
                    <div each={ config.dm }>
                        <p>
                            <strong>{security.uri}</strong><br/>
                            security mode : {security.securityMode}<br/>
                            <span if={security.securityMode !== 'NO_SEC'}>
                                Id : <code> {toAscii(security.publicKeyOrId)} </code> </br>
                                secret : <code> {toHex(security.secretKey)} </code> </br>
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
        var self = this;

        this.on('mount',function(){
            bsConfigStore.init();
        });

        bsConfigStore.on("changed", function(configs){
            self.configs = configs;
            self.update();
        });

        showModal(){
            riot.mount('div#modal', 'bootstrap-modal');
        };

        remove(e){
            bsConfigStore.remove(e.item.endpoint);
        }

        // utils
        toAscii(byteArray){
            var ascii = [];
            for (var i in byteArray){
                ascii[i] = String.fromCharCode(byteArray[i]);
            }
            return ascii.join('');
        };

        toHex(byteArray){
            var hex = [];
            for (var i in byteArray){
                hex[i] = byteArray[i].toString(16).toUpperCase();
            }
            return hex.join('');
        };
    </script>
</bootstrap>

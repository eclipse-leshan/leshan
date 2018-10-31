package org.eclipse.leshan.server.bootstrap.demo;

import org.eclipse.leshan.core.request.Identity;
import org.eclipse.leshan.server.bootstrap.BootstrapConfig;

public class WildCardBootstrapStoreImpl extends BootstrapStoreImpl {


    public WildCardBootstrapStoreImpl() {
        this(DEFAULT_FILE);
    }

    /**
     * @param filename the file path to persist the registry
     */
    public WildCardBootstrapStoreImpl(String filename) {
        super(filename);
    }


    @Override
    public BootstrapConfig getBootstrap(String endpoint, Identity deviceIdentity) {
        BootstrapConfig config = bootstrapByEndpoint.get(endpoint);
        if (config == null) {
            config = bootstrapByEndpoint.get("*");
        }
        return config;
    }


}

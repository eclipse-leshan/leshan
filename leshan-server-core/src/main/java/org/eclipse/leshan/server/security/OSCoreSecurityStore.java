package org.eclipse.leshan.server.security;

import java.util.Collection;

import org.eclipse.californium.cose.AlgorithmID;
import org.eclipse.californium.cose.CoseException;
import org.eclipse.californium.oscore.HashMapCtxDB;
import org.eclipse.californium.oscore.OSCoreCtx;
import org.eclipse.californium.oscore.OSException;
import org.eclipse.leshan.core.OscoreObject;
import org.eclipse.leshan.server.OscoreHandler;

import com.upokecenter.cbor.CBORObject;

public class OSCoreSecurityStore implements EditableSecurityStore {

    private final EditableSecurityStore editableSecurityStore;
    private final HashMapCtxDB ctxDB;

    public OSCoreSecurityStore(EditableSecurityStore editableSecurityStore) {
        this.editableSecurityStore = editableSecurityStore;
        ctxDB = OscoreHandler.getContextDB();
    }

    @Override
    public Collection<SecurityInfo> getAll() {
        return editableSecurityStore.getAll();
    }

    @Override
    public SecurityInfo add(SecurityInfo info) throws NonUniqueSecurityInfoException {
        SecurityInfo securityInfo = editableSecurityStore.add(info);

        addOscoreContextToDb(info);

        return securityInfo;
    }

    @Override
    public SecurityInfo remove(String endpoint, boolean infosAreCompromised) {
        SecurityInfo securityInfo = editableSecurityStore.remove(endpoint, infosAreCompromised);

        try {
            if (securityInfo != null) {
                OSCoreCtx osCoreCtx = getOsCoreCtx(securityInfo);
                ctxDB.removeContext(osCoreCtx);
            }
        } catch (CoseException | OSException e) {
            e.printStackTrace();
        }

        return securityInfo;
    }

    @Override
    public void setListener(SecurityStoreListener listener) {
        editableSecurityStore.setListener(listener);
    }

    @Override
    public SecurityInfo getByEndpoint(String endpoint) {
        return editableSecurityStore.getByEndpoint(endpoint);
    }

    @Override
    public SecurityInfo getByIdentity(String pskIdentity) {
        return editableSecurityStore.getByIdentity(pskIdentity);
    }

    private void addOscoreContextToDb(SecurityInfo info) {
        if (info.useOSCORE()) {
            try {
                ctxDB.addContext(getOsCoreCtx(info));
            } catch (CoseException | OSException e) {
                throw new IllegalArgumentException("Error while creating OSCore Context", e);
            }
        }
    }

    private OSCoreCtx getOsCoreCtx(SecurityInfo info) throws CoseException, OSException {
        byte[] idContext = null;
        OscoreObject oscoreObject = info.getOscoreObject();

        return new OSCoreCtx(oscoreObject.getMasterSecret(), false,
                AlgorithmID.FromCBOR(CBORObject.FromObject(oscoreObject.getAeadAlgorithm())),
                oscoreObject.getSenderId(), oscoreObject.getRecipientId(),
                AlgorithmID.FromCBOR(CBORObject.FromObject(oscoreObject.getHmacAlgorithm())),
                32, oscoreObject.getMasterSalt(), idContext);
    }
}

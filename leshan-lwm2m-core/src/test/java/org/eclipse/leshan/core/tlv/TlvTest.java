package org.eclipse.leshan.core.tlv;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class TlvTest {

    @Test
    public void assertEqualsHashcode() {
        Tlv prefab1 = new Tlv(Tlv.TlvType.OBJECT_INSTANCE, new Tlv[0], null, 1);
        Tlv prefab2 = new Tlv(Tlv.TlvType.OBJECT_INSTANCE, new Tlv[0], null, 2);

        EqualsVerifier.forClass(Tlv.class).withPrefabValues(Tlv.class, prefab1, prefab2).verify();
    }
}
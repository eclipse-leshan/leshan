package org.eclipse.leshan.core.oscore;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class OscoreSettingTest {
    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(OscoreSetting.class).verify();
    }
}
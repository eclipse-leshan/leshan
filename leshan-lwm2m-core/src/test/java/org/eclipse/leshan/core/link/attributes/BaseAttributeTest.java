package org.eclipse.leshan.core.link.attributes;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class BaseAttributeTest {

    private class ExtendedBaseAttribute extends BaseAttribute {
        public ExtendedBaseAttribute(String name, Object value) {
            super(name, value, false);
        }

        @Override
        public boolean canEqual(Object obj) {
            return obj instanceof ExtendedBaseAttribute;
        }

        @Override
        public String getCoreLinkValue() {
            Object val = getValue();
            return val != null ? val.toString() : "";
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(BaseAttribute.class).withRedefinedSubclass(ExtendedBaseAttribute.class).verify();
    }
}
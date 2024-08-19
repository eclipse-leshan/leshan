package org.eclipse.leshan.core.link;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.eclipse.leshan.core.link.attributes.Attribute;
import org.eclipse.leshan.core.link.attributes.AttributeSet;
import org.junit.jupiter.api.Test;

import java.util.Collection;

class LinkTest {

    private class ExtendedLink extends Link {
        public ExtendedLink(String uriReference, Collection<Attribute> attributes) {
            super(uriReference, new AttributeSet(attributes));
        }
        @Override
        public boolean canEqual(Object obj) {
            return (obj instanceof ExtendedLink);
        }
    }

    @Test
    public void assertEqualsHashcode() {
        EqualsVerifier.forClass(Link.class).withRedefinedSubclass(ExtendedLink.class).verify();
    }

}
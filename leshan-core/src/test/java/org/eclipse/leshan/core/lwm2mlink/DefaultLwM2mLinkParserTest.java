package org.eclipse.leshan.core.lwm2mlink;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashSet;

import org.eclipse.leshan.core.attributes.AttributeSet;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.junit.Test;

public class DefaultLwM2mLinkParserTest {

    @Test
    public void parse_link_with_integer() {
        Link[] links = new Link[] {
                new Link("/0/0/0", "dim", "1")
        };

        DefaultLwM2mLinkParser defaultLwM2mLinkParser = new DefaultLwM2mLinkParser();
        LwM2mLinks lwM2mLinks = defaultLwM2mLinkParser.parse(links);

        assertNotNull(lwM2mLinks);
        assertEquals(1, lwM2mLinks.size());

        HashSet<LwM2mPath> expectedPaths = new HashSet<>(Collections.singletonList(new LwM2mPath("/0/0/0")));
        assertEquals(expectedPaths, lwM2mLinks.getPaths());

        AttributeSet expectedAttributes = AttributeSet.parse("dim=1");
        assertEquals(expectedAttributes, lwM2mLinks.getAttributesForPath("/0/0/0"));
    }

    @Test
    public void parse_link_with_float() {
        Link[] links = new Link[] {
                new Link("/0/0/0", "gt", "0.1")
        };

        DefaultLwM2mLinkParser defaultLwM2mLinkParser = new DefaultLwM2mLinkParser();
        LwM2mLinks lwM2mLinks = defaultLwM2mLinkParser.parse(links);

        assertNotNull(lwM2mLinks);
        assertEquals(1, lwM2mLinks.size());

        HashSet<LwM2mPath> expectedPaths = new HashSet<>(Collections.singletonList(new LwM2mPath("/0/0/0")));
        assertEquals(expectedPaths, lwM2mLinks.getPaths());

        AttributeSet expectedAttributes = AttributeSet.parse("gt=0.1");
        assertEquals(expectedAttributes, lwM2mLinks.getAttributesForPath("/0/0/0"));
    }

    @Test
    public void parse_link_with_string() {
        Link[] links = new Link[] {
                new Link("/0/0/0", "ver", "1.1")
        };

        DefaultLwM2mLinkParser defaultLwM2mLinkParser = new DefaultLwM2mLinkParser();
        LwM2mLinks lwM2mLinks = defaultLwM2mLinkParser.parse(links);

        assertNotNull(lwM2mLinks);
        assertEquals(1, lwM2mLinks.size());

        HashSet<LwM2mPath> expectedPaths = new HashSet<>(Collections.singletonList(new LwM2mPath("/0/0/0")));
        assertEquals(expectedPaths, lwM2mLinks.getPaths());

        AttributeSet expectedAttributes = AttributeSet.parse("ver=1.1");
        assertEquals(expectedAttributes, lwM2mLinks.getAttributesForPath("/0/0/0"));
    }
}
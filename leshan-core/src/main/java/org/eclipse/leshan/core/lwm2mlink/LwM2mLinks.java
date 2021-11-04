package org.eclipse.leshan.core.lwm2mlink;

import java.util.Map;
import java.util.Set;

import org.eclipse.leshan.core.attributes.AttributeSet;
import org.eclipse.leshan.core.node.LwM2mPath;

public class LwM2mLinks {

    Map<LwM2mPath, AttributeSet> links;

    public LwM2mLinks(Map<LwM2mPath, AttributeSet> links) {
        this.links = links;
    }

    public Map<LwM2mPath, AttributeSet> getLinks() {
        return links;
    }

    public AttributeSet getAttributesForPath(LwM2mPath path) {
        return links.get(path);
    }

    public AttributeSet getAttributesForPath(String path) {
        return getAttributesForPath(new LwM2mPath(path));
    }

    public Set<LwM2mPath> getPaths() {
        return links.keySet();
    }

    public int size() {
        return getPaths().size();
    }
}

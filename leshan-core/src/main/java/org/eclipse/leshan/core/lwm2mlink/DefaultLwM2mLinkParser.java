package org.eclipse.leshan.core.lwm2mlink;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.leshan.core.attributes.Attribute;
import org.eclipse.leshan.core.attributes.AttributeModel;
import org.eclipse.leshan.core.attributes.AttributeSet;
import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.LinkParamValue;
import org.eclipse.leshan.core.node.LwM2mPath;

public class DefaultLwM2mLinkParser implements LwM2mLinkParser {

    @Override
    public LwM2mLinks parse(Link[] links) {
        Map<LwM2mPath, AttributeSet> result = new HashMap<>();

        for (Link link: links) {
            AttributeSet attributes = linkParamsToAttributeSet(link.getLinkParams());

            result.put(new LwM2mPath(link.getUriReference()), attributes);
        }
        return new LwM2mLinks(result);
    }

    private AttributeSet linkParamsToAttributeSet(Map<String, LinkParamValue> linkParams) {
        Collection<Attribute> attributes = new HashSet<>();
        for (String CoRELinkParam : linkParams.keySet()) {
            LinkParamValue value = linkParams.get(CoRELinkParam);

            AttributeModel model = AttributeModel.get(CoRELinkParam);

            Class<?> expectedClass = model.getValueClass();
            if (expectedClass.equals(Long.class)) {
                attributes.add(new Attribute(CoRELinkParam, Long.parseLong(value.toString())));
            } else if (expectedClass.equals(Double.class)) {
                attributes.add(new Attribute(CoRELinkParam, Double.parseDouble (value.toString())));
            } else if (expectedClass.equals(String.class)) {
                attributes.add(new Attribute(CoRELinkParam, value.toString()));
            }



        }
        return new AttributeSet(attributes);
    }
}

package org.eclipse.leshan.core.link;

import java.util.Objects;

public class LinkParamValue {

    private final String linkParamValue;

    public LinkParamValue(String linkParamValue) {
        if (linkParamValue == null) {
            throw new IllegalArgumentException("Link-param value can't be null");
        }
        this.linkParamValue = linkParamValue;
    }

    @Override
    public String toString() {
        return linkParamValue;
    }

    /**
     * remove quote from string, only if it begins and ends by a quote.
     *
     * @return unquoted string or the original string if there no quote to remove.
     */
    public String getUnquoted() {
        if (linkParamValue.length() >= 2 && linkParamValue.charAt(0) == '"'
                && linkParamValue.charAt(linkParamValue.length() - 1) == '"') {
            return linkParamValue.substring(1, linkParamValue.length() - 1);
        }
        return linkParamValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof LinkParamValue))
            return false;
        LinkParamValue linkParamValue1 = (LinkParamValue) o;
        return Objects.equals(linkParamValue, linkParamValue1.linkParamValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(linkParamValue);
    }
}

/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.link.lwm2m;

import java.util.Collection;

import org.eclipse.leshan.core.link.Link;
import org.eclipse.leshan.core.link.attributes.Attribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.LwM2mAttribute;
import org.eclipse.leshan.core.link.lwm2m.attributes.MixedLwM2mAttributeSet;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.util.Validate;

/**
 * a LWM2M Link which can contain {@link LwM2mAttribute} but also tolerate not LWM2M {@link Attribute}.
 * <p>
 * It can also have a rootpath.
 */
public class MixedLwM2mLink extends Link {

    private LwM2mPath path;
    private String rootPath;

    public MixedLwM2mLink(String rootPath, LwM2mPath path, Attribute... attributes) {
        this(rootPath, path, new MixedLwM2mAttributeSet(attributes));
    }

    public MixedLwM2mLink(String rootPath, LwM2mPath path, Collection<? extends Attribute> attributes) {
        this(rootPath, path, new MixedLwM2mAttributeSet(attributes));
    }

    public MixedLwM2mLink(String rootPath, LwM2mPath path, MixedLwM2mAttributeSet attributes) {
        super(getUriReference(rootPath, path), attributes);

        Validate.notNull(attributes);

        this.path = path;
        this.rootPath = rootPath == null ? "/" : rootPath;
    }

    private static String getUriReference(String rootPath, LwM2mPath path) {
        Validate.notNull(path);

        if (rootPath == null || rootPath.equals("/")) {
            return path.toString();
        } else if (path.isRoot()) {
            // rootpath can not be null because we check this before
            return rootPath;
        } else {
            return rootPath + path.toString();
        }
    }

    public LwM2mPath getPath() {
        return path;
    }

    public String getRootPath() {
        return rootPath;
    }

    @Override
    public MixedLwM2mAttributeSet getAttributes() {
        return (MixedLwM2mAttributeSet) super.getAttributes();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((rootPath == null) ? 0 : rootPath.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        MixedLwM2mLink other = (MixedLwM2mLink) obj;
        if (path == null) {
            if (other.path != null)
                return false;
        } else if (!path.equals(other.path))
            return false;
        if (rootPath == null) {
            if (other.rootPath != null)
                return false;
        } else if (!rootPath.equals(other.rootPath))
            return false;
        return true;
    }
}

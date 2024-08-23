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
import java.util.Objects;

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

    private final LwM2mPath path;
    private final String rootPath;

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
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof MixedLwM2mLink))
            return false;
        if (!super.equals(o))
            return false;
        MixedLwM2mLink that = (MixedLwM2mLink) o;
        return that.canEqual(this) && Objects.equals(path, that.path) && Objects.equals(rootPath, that.rootPath);
    }

    public boolean canEqual(Object o) {
        return (o instanceof MixedLwM2mLink);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), path, rootPath);
    }
}

/*******************************************************************************
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
 *     Magdalena Kundera
 *     Orange Polska S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.demo.cli.converters;

import org.eclipse.leshan.core.node.InvalidLwM2mPathException;
import org.eclipse.leshan.core.node.LwM2mNodeUtil;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.util.Validate;

import picocli.CommandLine.ITypeConverter;

public class ResourceConverter implements ITypeConverter<LwM2mPath> {

    @Override
    public LwM2mPath convert(String path) throws Exception {
        Validate.notNull(path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String[] p = path.split("/");
        if (0 > p.length || p.length > 4)
            throw new InvalidLwM2mPathException("Invalid length for path: ", path);

        Integer resourceId = (p.length >= 3) ? Integer.valueOf(p[2]) : null;
        Integer resourceInstanceId = (p.length == 4) ? Integer.valueOf(p[3]) : null;

        if (resourceId != null)
            LwM2mNodeUtil.validateResourceId(resourceId);
        if (resourceInstanceId != null)
            LwM2mNodeUtil.validateResourceInstanceId(resourceInstanceId);

        return new LwM2mPath(path);
    }
}

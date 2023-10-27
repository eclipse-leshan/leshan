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

import org.eclipse.leshan.core.node.LwM2mNodeException;
import org.eclipse.leshan.core.node.LwM2mPath;

public class ResourceConverter extends LwM2mPathConverter {

    @Override
    public LwM2mPath convert(String path) {
        LwM2mPath lwM2mPath = super.convert(path);
        if (!lwM2mPath.isResource())
            throw new LwM2mNodeException(String.format("Invalid resource path : %s is not a resource path", lwM2mPath));
        return lwM2mPath;
    }
}

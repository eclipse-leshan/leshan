/*******************************************************************************
 * Copyright (c) 2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.client.resource;

import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ValueResponse;

public interface LwM2mInstanceEnabler {

    void setObjectModel(ObjectModel objectModel);

    void addResourceChangedListener(ResourceChangedListener listener);

    void removeResourceChangedListener(ResourceChangedListener listener);

    ValueResponse read(int resourceid);

    LwM2mResponse write(int resourceid, LwM2mResource value);

    LwM2mResponse execute(int resourceid, byte[] params);

}

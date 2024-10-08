/*******************************************************************************
 * Copyright (c) 2024 Sierra Wireless and others.
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
package org.eclipse.leshan.core.endpoint;

public interface EndPointUriParser {

    EndpointUri parse(String Uri) throws InvalidEndpointUriException;

    void validateScheme(String scheme) throws InvalidEndpointUriException;

    void validateHost(String host) throws InvalidEndpointUriException;

    void validatePort(String port) throws InvalidEndpointUriException;

    void validatePort(Integer port) throws InvalidEndpointUriException;
}

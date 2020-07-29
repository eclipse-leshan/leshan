/*******************************************************************************
 * Copyright (c) 2016 Sierra Wireless and others.
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
package org.eclipse.leshan.core.request;

/**
 * An abstract adapter class for visiting down link request. The methods in this class are empty. This class exists as
 * convenience for creating message observer objects.
 */
public class DownLinkRequestVisitorAdapter implements DownlinkRequestVisitor {

    @Override
    public void visit(ReadRequest request) {
    }

    @Override
    public void visit(DiscoverRequest request) {
    }

    @Override
    public void visit(WriteRequest request) {
    }

    @Override
    public void visit(WriteAttributesRequest request) {
    }

    @Override
    public void visit(ExecuteRequest request) {
    }

    @Override
    public void visit(CreateRequest request) {
    }

    @Override
    public void visit(DeleteRequest request) {
    }

    @Override
    public void visit(ObserveRequest request) {
    }

    @Override
    public void visit(CancelObservationRequest request) {
    }

    @Override
    public void visit(BootstrapDiscoverRequest request) {
    }

    @Override
    public void visit(BootstrapWriteRequest request) {
    }

    @Override
    public void visit(BootstrapDeleteRequest request) {
    }

    @Override
    public void visit(BootstrapFinishRequest request) {
    }
}

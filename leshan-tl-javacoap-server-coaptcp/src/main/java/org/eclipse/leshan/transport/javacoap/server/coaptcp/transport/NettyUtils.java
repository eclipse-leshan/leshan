/*******************************************************************************
 * Copyright (c) 2023 Sierra Wireless and others.
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
package org.eclipse.leshan.transport.javacoap.server.coaptcp.transport;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletableFuture;

import io.netty.util.concurrent.Promise;

public class NettyUtils {

    public static <T> CompletableFuture<T> toCompletableFuture(Promise<T> nettyPromise) {

        if (nettyPromise.isSuccess()) {
            return completedFuture(nettyPromise.getNow());
        }

        CompletableFuture<T> promise = new CompletableFuture<>();
        nettyPromise.addListener(future -> {
            if (future.cause() != null) {
                promise.completeExceptionally(future.cause());
            } else {
                promise.complete(nettyPromise.getNow());
            }
        });

        return promise;
    }
}

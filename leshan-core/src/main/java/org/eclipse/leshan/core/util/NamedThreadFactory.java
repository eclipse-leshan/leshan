/*******************************************************************************
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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
 *     Alexander Ellwein, Daniel Maier (Bosch Software Innovations GmbH)
 *                                - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of {@link ThreadFactory} that sets thread names according to given name format. All threads are
 * created by {@link Executors#defaultThreadFactory() #newThread(Runnable)}.
 */
public final class NamedThreadFactory implements ThreadFactory {
    private final String nameFormat;
    private final AtomicLong counter = new AtomicLong();

    /**
     * Creates a new {@link NamedThreadFactory}.
     * 
     * @param nameFormat result of {@link String#format(String, Object...)} with this format and unique counter will be
     *        used for thread name. Example: format of {@code xyz-%d} will result in thread names {@code xyz-1},
     *        {@code xyz-2} etc.
     */
    public NamedThreadFactory(final String nameFormat) {
        this.nameFormat = nameFormat;
    }

    @Override
    public Thread newThread(final Runnable r) {
        final Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setName(String.format(nameFormat, counter.getAndIncrement()));
        return thread;
    }
}
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
package org.eclipse.leshan.client.demo.cli.interactive;

import org.jline.reader.LineReader;

import ch.qos.logback.core.ConsoleAppender;

public class TerminalAppender<E> extends ConsoleAppender<E> {

    private LineReader reader;

    public void setReader(LineReader reader) {
        this.reader = reader;
    }

    @Override
    protected void append(E eventObject) {
        if (reader == null) {
            super.append(eventObject);
        } else {
            reader.printAbove(new String(getEncoder().encode(eventObject)));
        }
    }
}

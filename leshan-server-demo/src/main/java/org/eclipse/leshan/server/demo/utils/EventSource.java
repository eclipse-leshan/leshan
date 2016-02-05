/*
 * Copyright (c) 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.leshan.server.demo.utils;

import java.io.IOException;

/**
 * <p>
 * {@link EventSource} is the passive half of an event source connection, as defined by the <a
 * href="http://www.w3.org/TR/eventsource/">EventSource Specification</a>.
 * </p>
 * <p>
 * {@link EventSource.Emitter} is the active half of the connection and allows to operate on the connection.
 * </p>
 * <p>
 * {@link EventSource} allows applications to be notified of events happening on the connection; two events are being
 * notified: the opening of the event source connection, where method {@link EventSource#onOpen(Emitter)} is invoked,
 * and the closing of the event source connection, where method {@link EventSource#onClose()} is invoked.
 * </p>
 *
 * @see EventSourceServlet
 */
public interface EventSource {
    /**
     * <p>
     * Callback method invoked when an event source connection is opened.
     * </p>
     *
     * @param emitter the {@link Emitter} instance that allows to operate on the connection
     * @throws IOException if the implementation of the method throws such exception
     */
    public void onOpen(Emitter emitter) throws IOException;

    /**
     * <p>
     * Callback method invoked when an event source connection is closed.
     * </p>
     */
    public void onClose();

    /**
     * <p>
     * {@link Emitter} is the active half of an event source connection, and allows applications to operate on the
     * connection by sending events, data or comments, or by closing the connection.
     * </p>
     * <p>
     * An {@link Emitter} instance will be created for each new event source connection.
     * </p>
     * <p>
     * {@link Emitter} instances are fully thread safe and can be used from multiple threads.
     * </p>
     */
    public interface Emitter {
        /**
         * <p>
         * Sends a named event with data to the client.
         * </p>
         * <p>
         * When invoked as: <code>event("foo", "bar")</code>, the client will receive the lines:
         * </p>
         * 
         * <pre>
         * event: foo
         * data: bar
         * </pre>
         *
         * @param name the event name
         * @param data the data to be sent
         * @throws IOException if an I/O failure occurred
         * @see #data(String)
         */
        public void event(String name, String data) throws IOException;

        /**
         * <p>
         * Sends a default event with data to the client.
         * </p>
         * <p>
         * When invoked as: <code>data("baz")</code>, the client will receive the line:
         * </p>
         * 
         * <pre>
         * data: baz
         * </pre>
         * <p>
         * When invoked as: <code>data("foo\r\nbar\rbaz\nbax")</code>, the client will receive the lines:
         * </p>
         * 
         * <pre>
         * data: foo
         * data: bar
         * data: baz
         * data: bax
         * </pre>
         *
         * @param data the data to be sent
         * @throws IOException if an I/O failure occurred
         */
        public void data(String data) throws IOException;

        /**
         * <p>
         * Sends a comment to the client.
         * </p>
         * <p>
         * When invoked as: <code>comment("foo")</code>, the client will receive the line:
         * </p>
         * 
         * <pre>
         * : foo
         * </pre>
         *
         * @param comment the comment to send
         * @throws IOException if an I/O failure occurred
         */
        public void comment(String comment) throws IOException;

        /**
         * <p>
         * Closes this event source connection.
         * </p>
         */
        public void close();
    }
}

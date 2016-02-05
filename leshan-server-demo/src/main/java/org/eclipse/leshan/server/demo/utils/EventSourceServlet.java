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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;

/**
 * <p>
 * A servlet that implements the <a href="http://www.w3.org/TR/eventsource/">event source protocol</a>, also known as
 * "server sent events".
 * </p>
 * <p>
 * This servlet must be subclassed to implement abstract method {@link #newEventSource(HttpServletRequest)} to return an
 * instance of {@link EventSource} that allows application to listen for event source events and to emit event source
 * events.
 * </p>
 * <p>
 * This servlet supports the following configuration parameters:
 * </p>
 * <ul>
 * <li><code>heartBeatPeriod</code>, that specifies the heartbeat period, in seconds, used to check whether the
 * connection has been closed by the client; defaults to 10 seconds.</li>
 * </ul>
 *
 * <p>
 * NOTE: there is currently no support for <code>last-event-id</code>.
 * </p>
 */
@SuppressWarnings("serial")
public abstract class EventSourceServlet extends HttpServlet {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final byte[] CRLF = new byte[] { '\r', '\n' };
    private static final byte[] EVENT_FIELD;
    private static final byte[] DATA_FIELD;
    private static final byte[] COMMENT_FIELD;
    static {
        try {
            EVENT_FIELD = "event: ".getBytes(UTF_8.name());
            DATA_FIELD = "data: ".getBytes(UTF_8.name());
            COMMENT_FIELD = ": ".getBytes(UTF_8.name());
        } catch (UnsupportedEncodingException x) {
            throw new RuntimeException(x);
        }
    }

    private ScheduledExecutorService scheduler;
    private int heartBeatPeriod = 10;

    @Override
    public void init() throws ServletException {
        String heartBeatPeriodParam = getServletConfig().getInitParameter("heartBeatPeriod");
        if (heartBeatPeriodParam != null)
            heartBeatPeriod = Integer.parseInt(heartBeatPeriodParam);
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void destroy() {
        if (scheduler != null)
            scheduler.shutdown();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Enumeration<String> acceptValues = request.getHeaders("Accept");
        while (acceptValues.hasMoreElements()) {
            String accept = acceptValues.nextElement();
            if (accept.equals("text/event-stream")) {
                EventSource eventSource = newEventSource(request);
                if (eventSource == null) {
                    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                } else {
                    respond(request, response);
                    Continuation continuation = ContinuationSupport.getContinuation(request);
                    // Infinite timeout because the continuation is never resumed,
                    // but only completed on close
                    continuation.setTimeout(0L);
                    continuation.suspend(response);
                    EventSourceEmitter emitter = new EventSourceEmitter(eventSource, continuation);
                    emitter.scheduleHeartBeat();
                    open(eventSource, emitter);
                }
                return;
            }
        }
        super.doGet(request, response);
    }

    protected abstract EventSource newEventSource(HttpServletRequest request);

    protected void respond(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(UTF_8.name());
        response.setContentType("text/event-stream");
        // By adding this header, and not closing the connection,
        // we disable HTTP chunking, and we can use write()+flush()
        // to send data in the text/event-stream protocol
        response.addHeader("Connection", "close");
        response.flushBuffer();
    }

    protected void open(EventSource eventSource, EventSource.Emitter emitter) throws IOException {
        eventSource.onOpen(emitter);
    }

    protected class EventSourceEmitter implements EventSource.Emitter, Runnable {
        private final EventSource eventSource;
        private final Continuation continuation;
        private final ServletOutputStream output;
        private Future<?> heartBeat;
        private boolean closed;

        public EventSourceEmitter(EventSource eventSource, Continuation continuation) throws IOException {
            this.eventSource = eventSource;
            this.continuation = continuation;
            this.output = continuation.getServletResponse().getOutputStream();
        }

        public void event(String name, String data) throws IOException {
            synchronized (this) {
                output.write(EVENT_FIELD);
                output.write(name.getBytes(UTF_8.name()));
                output.write(CRLF);
                data(data);
            }
        }

        public void data(String data) throws IOException {
            synchronized (this) {
                BufferedReader reader = new BufferedReader(new StringReader(data));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.write(DATA_FIELD);
                    output.write(line.getBytes(UTF_8.name()));
                    output.write(CRLF);
                }
                output.write(CRLF);
                flush();
            }
        }

        public void comment(String comment) throws IOException {
            synchronized (this) {
                output.write(COMMENT_FIELD);
                output.write(comment.getBytes(UTF_8.name()));
                output.write(CRLF);
                output.write(CRLF);
                flush();
            }
        }

        public void run() {
            // If the other peer closes the connection, the first
            // flush() should generate a TCP reset that is detected
            // on the second flush()
            try {
                synchronized (this) {
                    output.write('\r');
                    flush();
                    output.write('\n');
                    flush();
                }
                // We could write, reschedule heartbeat
                scheduleHeartBeat();
            } catch (IOException x) {
                // The other peer closed the connection
                close();
                eventSource.onClose();
            }
        }

        protected void flush() throws IOException {
            continuation.getServletResponse().flushBuffer();
        }

        public void close() {
            synchronized (this) {
                closed = true;
                heartBeat.cancel(false);
            }
            continuation.complete();
        }

        private void scheduleHeartBeat() {
            synchronized (this) {
                if (!closed)
                    heartBeat = scheduler.schedule(this, heartBeatPeriod, TimeUnit.SECONDS);
            }
        }
    }
}

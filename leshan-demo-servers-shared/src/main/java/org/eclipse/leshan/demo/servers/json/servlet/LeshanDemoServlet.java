/*******************************************************************************
 * Copyright (c) 2025 Sierra Wireless and others.
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
package org.eclipse.leshan.demo.servers.json.servlet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class LeshanDemoServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(LeshanDemoServlet.class);

    public interface UnsafeRunnable {
        void run() throws IOException, ServletException, InterruptedException;
    }

    protected void executeSafely(HttpServletRequest req, HttpServletResponse resp, UnsafeRunnable runable) {
        try {
            runable.run();
        } catch (IOException | ServletException e) {
            safelySendError(req, resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    String.format("Unexpected error handling %s : %s ", reqToPrettyString(req), errToPrettyString(e)),
                    e);
        } catch (InterruptedException e) {
            safelySendError(req, resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    String.format("Thread Interrupted handling %s", reqToPrettyString(req)));
            Thread.currentThread().interrupt();
        }
    }

    protected String reqToPrettyString(HttpServletRequest req) {
        return String.format("%s - %s", req.getMethod(), req.getRequestURI());
    }

    protected String errToPrettyString(Throwable err) {
        StringBuilder b = new StringBuilder();
        b.append(err.getClass().getCanonicalName());
        if (err.getMessage() != null && !err.getMessage().isEmpty()) {
            b.append(" - ");
            b.append(err.getMessage());
        }
        if (err.getCause() != null) {
            b.append(" caused by ");
            b.append(err.getClass().getCanonicalName());
            if (err.getMessage() != null && !err.getMessage().isEmpty()) {
                b.append(" - ");
                b.append(err.getMessage());
            }
        }
        return b.toString();
    }

    protected void safelySendError(HttpServletRequest req, HttpServletResponse resp, int errorCode) {
        safelySendError(req, resp, errorCode, null, null);
    }

    protected void safelySendError(HttpServletRequest req, HttpServletResponse resp, int errorCode, String message) {
        safelySendError(req, resp, errorCode, message, null);
    }

    protected void safelySendError(HttpServletRequest req, HttpServletResponse resp, int errorCode, String message,
            Throwable cause) {
        LOG.error(message, cause);
        resp.setStatus(errorCode);
        resp.setContentType("text/plain; charset=UTF-8");
        try {
            if (message != null)
                resp.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOG.error("Unable to send error response {} - {} to request {}", errorCode, message,
                    reqToPrettyString(req));
        }
    }
}

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
package org.eclipse.leshan.integration.tests.util;

import org.eclipse.leshan.bsserver.BootstrapSession;
import org.eclipse.leshan.bsserver.BootstrapSessionAdapter;
import org.eclipse.leshan.core.request.BootstrapWriteRequest;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.DownlinkBootstrapRequest;
import org.eclipse.leshan.core.response.LwM2mResponse;

public class BootstrapRequestChecker extends BootstrapSessionAdapter {

    public interface RequestValidator {
        boolean validate(DownlinkBootstrapRequest<? extends LwM2mResponse> request);
    }

    private final RequestValidator validator;
    private boolean valid = true;

    public BootstrapRequestChecker(RequestValidator validator) {
        this.validator = validator;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public void sendRequest(BootstrapSession session, DownlinkBootstrapRequest<? extends LwM2mResponse> request) {
        if (!validator.validate(request)) {
            valid = false;
        }

    }

    public static BootstrapRequestChecker contentFormatChecker(final ContentFormat expectedFormat) {
        return new BootstrapRequestChecker(new RequestValidator() {
            @Override
            public boolean validate(DownlinkBootstrapRequest<? extends LwM2mResponse> request) {
                if (request instanceof BootstrapWriteRequest) {
                    return ((BootstrapWriteRequest) request).getContentFormat() == expectedFormat;
                }
                return true;
            }
        });
    }
}

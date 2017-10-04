/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan;

import org.eclipse.leshan.util.Validate;

/**
 * Response codes defined for LWM2M enabler
 */
public class ResponseCode {

    /** Common name for unknown response code. That means "not explicitly" defined in the LWM2M specification */
    public final static String UNKNOWN = "UNKNOWN";

    /** Resource correctly created */
    public final static int CREATED_CODE = 201;
    /** Resource correctly deleted */
    public final static int DELETED_CODE = 202;
    /** Resource correctly changed */
    public final static int CHANGED_CODE = 204;
    /** Content correctly delivered */
    public final static int CONTENT_CODE = 205;
    /** Access Right Permission Denied */
    public final static int UNAUTHORIZED_CODE = 401;
    /** Bad request format (missing parameters, bad encoding ...) */
    public final static int BAD_REQUEST_CODE = 400;
    /** This method (GET/PUT/POST/DELETE) is not allowed on this resource */
    public final static int METHOD_NOT_ALLOWED_CODE = 405;
    /** The Endpoint Client Name registration in the LWM2M Server is not allowed */
    public final static int FORBIDDEN_CODE = 403;
    /** Resource not found */
    public final static int NOT_FOUND_CODE = 404;
    /** None of the preferred Content-Formats can be returned */
    public final static int NOT_ACCEPTABLE_CODE = 406;
    /** The specified format is not supported */
    public final static int UNSUPPORTED_CONTENT_FORMAT_CODE = 415;
    /** generic response code for unexpected error */
    public final static int INTERNAL_SERVER_ERROR_CODE = 500;

    // LwM2m Response codes
    public final static ResponseCode CREATED = new ResponseCode(CREATED_CODE, "CREATED");
    public final static ResponseCode DELETED = new ResponseCode(DELETED_CODE, "DELETED");
    public final static ResponseCode CHANGED = new ResponseCode(CHANGED_CODE, "CHANGED");
    public final static ResponseCode CONTENT = new ResponseCode(CONTENT_CODE, "CONTENT");
    public final static ResponseCode UNAUTHORIZED = new ResponseCode(UNAUTHORIZED_CODE, "UNAUTHORIZED");
    public final static ResponseCode BAD_REQUEST = new ResponseCode(BAD_REQUEST_CODE, "BAD_REQUEST");
    public final static ResponseCode METHOD_NOT_ALLOWED = new ResponseCode(METHOD_NOT_ALLOWED_CODE,
            "METHOD_NOT_ALLOWED");
    public final static ResponseCode FORBIDDEN = new ResponseCode(FORBIDDEN_CODE, "FORBIDDEN");
    public final static ResponseCode NOT_FOUND = new ResponseCode(NOT_FOUND_CODE, "NOT_FOUND");
    public final static ResponseCode NOT_ACCEPTABLE = new ResponseCode(NOT_ACCEPTABLE_CODE, "NOT_ACCEPTABLE");
    public final static ResponseCode UNSUPPORTED_CONTENT_FORMAT = new ResponseCode(UNSUPPORTED_CONTENT_FORMAT_CODE,
            "UNSUPPORTED_CONTENT_FORMAT");
    public final static ResponseCode INTERNAL_SERVER_ERROR = new ResponseCode(INTERNAL_SERVER_ERROR_CODE, "CREATED");

    private static final ResponseCode knownResponseCode[] = new ResponseCode[] { CREATED, DELETED, CHANGED, CONTENT,
                            UNAUTHORIZED, BAD_REQUEST, METHOD_NOT_ALLOWED, FORBIDDEN, NOT_FOUND, NOT_ACCEPTABLE,
                            UNSUPPORTED_CONTENT_FORMAT, INTERNAL_SERVER_ERROR };

    private int code;
    private String name;

    public ResponseCode(int code, String name) {
        Validate.notNull(name);
        this.code = code;
        this.name = name;
    }

    public ResponseCode(int code) {
        this(code, UNKNOWN);
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isError() {
        return isClientError() || isServerError();
    }

    public boolean isSuccess() {
        return (code / 100) == 2;
    }

    public boolean isClientError() {
        return (code / 100) == 4;
    }

    public boolean isServerError() {
        return (code / 100) == 5;
    }

    public static ResponseCode fromName(String name) {
        for (ResponseCode c : knownResponseCode) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    public static ResponseCode fromCode(int code) {
        for (ResponseCode c : knownResponseCode) {
            if (c.getCode() == code) {
                return c;
            }
        }
        return new ResponseCode(code);
    }

    @Override
    public String toString() {
        if (UNKNOWN.equals(name))
            return String.format("%s(%d)", name, code);
        else
            return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + code;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResponseCode other = (ResponseCode) obj;
        if (code != other.code)
            return false;
        return true;
    }
}

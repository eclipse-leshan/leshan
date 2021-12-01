/*******************************************************************************
 * Copyright (c) 2021 Orange.
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
 *     Orange - Add better support for Arguments of Execute Operation.
 *******************************************************************************/
package org.eclipse.leshan.core.request.argument;

import java.util.regex.Pattern;

/**
 * Single element of {@link Arguments} for Execute Operation.
 */
public class Argument {

    private static final Pattern valuePattern = Pattern.compile("[!\\x23-\\x26\\x28-\\x5B\\x5D-\\x7E]*");

    private final int digit;

    private final String value;

    /**
     * Creates an Argument from digit and optional value.
     *
     * <pre>
     * {@code
     * value =  *CHAR
     * CHAR =   "!" / %x23-26 / %x28-5B / %x5D-7E
     * }
     * </pre>
     *
     * @param digit number from 0 to 9. digit is required.
     * @param value optional value of argument. Can be null.
     *
     * @throws InvalidArgumentException in case invalid digit or value
     */
    public Argument(Integer digit, String value) throws InvalidArgumentException {
        validateDigit(digit);
        validateValue(digit, value);
        this.digit = digit;
        this.value = value;
    }

    /**
     * Creates an Argument consist of digit and value.
     *
     * @param digit number from 0 to 9
     *
     * @throws InvalidArgumentException in case invalid digit.
     */
    public Argument(Integer digit) throws InvalidArgumentException {
        validateDigit(digit);
        this.digit = digit;
        this.value = null;
    }

    private void validateDigit(Integer digit) throws InvalidArgumentException {
        if (digit == null || digit < 0 || digit > 9) {
            throw new InvalidArgumentException("Invalid Argument digit [%s]",
                    digit != null ? Integer.toString(digit) : "null");
        }
    }

    private void validateValue(int digit, String value) throws InvalidArgumentException {
        if (value != null && !valuePattern.matcher(value).matches()) {
            throw new InvalidArgumentException("Invalid Argument value [%s] for digit [%s]", value, digit);
        }
    }

    public int getDigit() {
        return digit;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        if (value == null)
            return String.format("Argument [digit=%d]", digit);
        else
            return String.format("Argument [digit=%d, value=%s]", digit, value);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + digit;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        Argument other = (Argument) obj;
        if (digit != other.digit)
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
}

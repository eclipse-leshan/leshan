/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
package org.eclipse.leshan.core.parser;

import org.eclipse.leshan.core.util.Validate;

/**
 * An String Parser Utility specially helpfull to parse to ABNF grammar defined at IETF.
 */
public abstract class StringParser<T extends Throwable> {

    private String strToParse;
    private int cursor;

    public StringParser(String stringToParse) {
        Validate.notNull(stringToParse);
        strToParse = stringToParse;
        cursor = 0;
    }

    /**
     * @return <code>true</code> if there is more char to consume, <code>false</code> if we reach the end of the String.
     */
    public boolean hasMoreChar() {
        return cursor < strToParse.length();
    }

    /**
     * @return <code>true</code> if there is a next char and it is equals to <code>character</code> argument.
     */
    public boolean nextCharIs(char character) {
        return hasMoreChar() && strToParse.charAt(cursor) == character;
    }

    /**
     * @return <code>true</code> if there is a next char and it is one of the char of the given String.
     */
    public boolean nextCharIsIn(String allowedChar) {
        return hasMoreChar() && allowedChar.indexOf(getNextChar()) != -1;
    }

    /**
     * @return <code>true</code> if there is a next char and its ascii value is between <code>start</code> and
     *         <code>end</code>
     */
    public boolean nextCharIsBetween(int start, int end) {
        if (!hasMoreChar())
            return false;
        int c = getNextChar();
        return c >= start && c <= end;
    }

    /**
     * LOALPHA as defined at https://datatracker.ietf.org/doc/html/rfc6690#section-2
     *
     * <pre>
     * LOALPHA        = %x61-7A   ; a-z
     * </pre>
     *
     * @return<code>true</code> if there is a next char and it is a ALPHA
     */
    public boolean nextCharIsLOALPHA() {
        return nextCharIsBetween('a', 'z');
    }

    /**
     * ALPHA as defined at https://datatracker.ietf.org/doc/html/rfc2234#section-6.1
     *
     * <pre>
     *  ALPHA          =  %x41-5A / %x61-7A   ; A-Z / a-z
     * </pre>
     *
     * @return<code>true</code> if there is a next char and it is a ALPHA
     */
    public boolean nextCharIsALPHA() {
        return nextCharIsBetween('A', 'Z') || nextCharIsBetween('a', 'z');
    }

    /**
     * DIGIT as defined at https://datatracker.ietf.org/doc/html/rfc2234#section-6.1
     *
     * <pre>
     *  DIGIT          =  %x30-39
                               ; 0-9
     * </pre>
     *
     *
     *
     * @return <code>true</code> if there is a next char and it is a DIGIT
     */
    public boolean nextCharIsDIGIT() {
        return nextCharIsBetween('0', '9');
    }

    /**
     * HEXDIG as defined at https://datatracker.ietf.org/doc/html/rfc2234#section-6.1
     *
     * <pre>
     * HEXDIG = DIGIT / "A" / "B" / "C" / "D" / "E" / "F"
     * </pre>
     *
     *
     *
     * @return <code>true</code> if there is a next char and it is an HEXDIG
     */
    public boolean nextCharIsHEXDIG() {
        return nextCharIsDIGIT() || nextCharIsBetween('A', 'F');
    }

    /**
     * consume next char. User must check before if there is more char available with {@link #hasMoreChar()}
     */
    public void consumeNextChar() {
        cursor++;
    }

    /**
     * Consume next char if it is equals to <code>character</code> argument, if not raise an exception.
     */
    public void consumeChar(char character) throws T {
        if (!hasMoreChar()) {
            raiseException("Unable to parse [%s] : unexpected EOF, expected '%s' character after %s", strToParse,
                    character, getAlreadyParsedString());
        }
        if (getNextChar() != character) {
            raiseException("Unable to parse [%s] : unexpected character '%s', expected '%s' after %s", strToParse,
                    getNextChar(), character, getAlreadyParsedString());
        }
        consumeNextChar();
    }

    /**
     * Consume next char if its ascii code is between <code>min</code> and <code>max</code> argument if not raise an
     * exception.
     */
    protected void consumeNextCharBetween(int min, int max) throws T {
        if (!hasMoreChar()) {
            raiseException(
                    "Unable to parse [%s] : unexpected EOF, expected char between '%c' and '%c' character after %s",
                    strToParse, min, max, getAlreadyParsedString());
        }
        if (!nextCharIsBetween(min, max)) {
            raiseException(
                    "Unable to parse [%s] : unexpected character '%s', expected char between '%c' and '%c'  after %s",
                    strToParse, getNextChar(), min, max, getAlreadyParsedString());
        }
        consumeNextChar();
    }

    /**
     * Consume next char if it is an HEXDIG character, if not raise an exception.
     *
     * @see #nextCharIsHEXDIG()
     */
    public void consumeHEXDIG() throws T {
        if (!hasMoreChar()) {
            raiseException("Unable to parse [%s] : unexpected EOF, expected 'HEXDIG' character after %s", strToParse,
                    getAlreadyParsedString());
        }
        if (!nextCharIsHEXDIG()) {
            raiseException("Unable to parse [%s] : unexpected character '%s', expected 'HEXDIG' after %s", strToParse,
                    getNextChar(), getAlreadyParsedString());
        }
        consumeNextChar();
    }

    /**
     * Consume next char if it is an DIGIT character, if not raise an exception.
     *
     * @see #nextCharIsDIGIT()
     */
    public void consumeDIGIT() throws T {
        if (!hasMoreChar()) {
            raiseException("Unable to parse [%s] : unexpected EOF, expected 'DIGIT' character after %s", strToParse,
                    getAlreadyParsedString());
        }
        if (!nextCharIsDIGIT()) {
            raiseException("Unable to parse [%s] : unexpected character '%s', expected 'DIGIT' after %s", strToParse,
                    getNextChar(), getAlreadyParsedString());
        }
        consumeNextChar();
    }

    /**
     * Consume next char if it is LOALPHA argument if not raise an exception.
     *
     * @see #nextCharIsLOALPHA()
     */
    public void consumeLOALPHA() throws T {
        if (!hasMoreChar()) {
            raiseException("Unable to parse [%s] : unexpected EOF, expected 'LOALPHA' character after %s", strToParse,
                    getAlreadyParsedString());
        }
        if (!nextCharIsLOALPHA()) {
            raiseException("Unable to parse [%s] : unexpected character '%s', expected 'LOALPHA' after %s", strToParse,
                    getNextChar(), getAlreadyParsedString());
        }
        consumeNextChar();
    }

    /**
     * Consume cardinal as described at https://datatracker.ietf.org/doc/html/rfc6690#section-2
     *
     * <pre>
     *  cardinal       = "0" / ( %x31-39 *DIGIT )
     * </pre>
     */
    public String consumeCardinal() throws T {
        // "0"
        if (nextCharIs('0')) {
            consumeNextChar();
            return "0";
        }
        // ( %x31-39 *DIGIT )
        else {
            int start = getPosition();
            consumeNextCharBetween('1', '9');
            while (nextCharIsDIGIT()) {
                consumeNextChar();
            }
            int end = getPosition();
            return substring(start, end);
        }
    }

    /**
     * @return the index of the next char to consume.
     */
    public int getPosition() {
        return cursor;
    }

    /**
     * @return next char to consume (without consuming it)
     */
    public char getNextChar() {
        return strToParse.charAt(cursor);
    }

    /**
     * @return subtring of the string to parse from start to end.
     *
     * @see String#substring(int, int)
     */
    public String substring(int start, int end) {
        return strToParse.substring(start, end);
    }

    /**
     * @return the whole string to parse or currently parsing.
     */
    public String getStringToParse() {
        return strToParse;
    }

    /**
     * @return already parsed substring.
     */
    public String getAlreadyParsedString() {
        return strToParse.substring(0, cursor);
    }

    /**
     * @return already parsed substring.
     */
    public String getRemainingStringToParse() {
        return strToParse.substring(cursor, strToParse.length());
    }

    /**
     * @see String#format(String, Object...)
     */
    public void raiseException(String message, Object... args) throws T {
        raiseException(null, message, args);
    };

    /**
     * @see String#format(String, Object...)
     */
    public void raiseException(Exception cause, String message, Object... args) throws T {
        if (args == null || args.length == 0) {
            raiseException(message, cause);
        } else {
            raiseException(String.format(message, args), cause);
        }
    }

    public abstract void raiseException(String message, Exception cause) throws T;
}

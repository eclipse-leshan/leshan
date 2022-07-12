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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Arguments for Execute Operation.
 */
public class Arguments implements Iterable<Argument> {

    private final Map<Integer, Argument> argumentMap;

    /**
     * Arguments should be build using {@link ArgumentsBuilder}.
     */
    private Arguments(Map<Integer, Argument> argumentMap) {
        this.argumentMap = argumentMap;
    }

    @Override
    public String toString() {
        return String.format("Arguments [argumentMap=%s]", argumentMap.toString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((argumentMap == null) ? 0 : argumentMap.hashCode());
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
        Arguments other = (Arguments) obj;
        if (argumentMap == null) {
            if (other.argumentMap != null)
                return false;
        } else if (!argumentMap.equals(other.argumentMap))
            return false;
        return true;
    }

    /**
     * Returns number of arguments.
     */
    public int size() {
        return argumentMap.size();
    }

    /**
     * Check if Arguments has any elements.
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Checks if {@link Arguments} has {@link Argument} with digit.
     */
    public boolean hasDigit(int digit) {
        validateDigit(digit);
        return argumentMap.containsKey(digit);
    }

    /**
     * Gets {@link Argument} by digit.
     *
     * @throws IllegalArgumentException if digit doesn't exists in {@link Arguments}
     */
    public Argument get(int digit) {
        validateDigit(digit);
        return argumentMap.get(digit);
    }

    /**
     * Gets digit keys of arguments.
     */
    public Set<Integer> getDigits() {
        return Collections.unmodifiableSet(argumentMap.keySet());
    }

    /**
     * Gets collection of {@link Argument}.
     */
    public Collection<Argument> getValues() {
        return Collections.unmodifiableCollection(argumentMap.values());
    }

    @Override
    public Iterator<Argument> iterator() {
        return Collections.unmodifiableCollection(argumentMap.values()).iterator();
    }

    private void validateDigit(int digit) throws IllegalArgumentException {
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException(String.format("Invalid digit [%s]", digit));
        }
    }

    /**
     * Parse String into {@link Arguments}
     *
     * <pre>
     * {@code
     * arglist = arg *( "," arg )
     * arg = DIGIT / DIGIT "=" "'" *CHAR "'"
     * DIGIT = "0" / "1" / "2" / "3" / "4" / "5" / "6" / "7" / "8" / "9"
     * CHAR = "!" / %x23-26 / %x28-5B / %x5D-7E
     * }
     * </pre>
     *
     * @param arglist String contains arguments to parse.
     * @return {@link Arguments} represents list of arguments.
     * @throws InvalidArgumentException if text has invalid format.
     */
    public static Arguments parse(String arglist) throws InvalidArgumentException {
        ArgumentsBuilder builder = builder();

        if (arglist != null && !arglist.isEmpty()) {
            int cursor = 0;
            while (true) {
                // consume argument
                cursor = consumeArgument(arglist, cursor, builder);

                // no more argument we finished
                if (cursor == arglist.length()) {
                    break;
                }

                // else consume separator ','
                if (arglist.charAt(cursor) == ',') {
                    cursor++;
                } else {
                    throw new InvalidArgumentException(
                            "Unable to parse Arguments [%s] : [,] separator expected at index %d after [%s]", arglist,
                            cursor, arglist.substring(0, cursor));
                }
            }
        }
        // Use builder capability to validate arguments uniqueness.
        try {
            return builder.build();
        } catch (InvalidArgumentException e) {
            throw new InvalidArgumentException(e, "Unable to parse Arguments [%s] : %s ", arglist, e.getMessage());
        }
    }

    static int consumeArgument(String arglist, final int initialCursor, ArgumentsBuilder builder)
            throws InvalidArgumentException {
        int currentCursor = initialCursor;
        int digit;
        String value = null;

        // consume digit
        if (currentCursor == arglist.length()) {
            throw new InvalidArgumentException(
                    "Unable to parse Arguments [%s] : unexpected EOF, expected argument after %s", arglist,
                    arglist.substring(0, currentCursor));
        }
        String digitChar = arglist.substring(currentCursor, currentCursor + 1);
        try {
            digit = Integer.parseInt(digitChar);
            currentCursor++;
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException(
                    "Unable to parse Arguments [%s] : Invalid digit [%s] (an integer between 0 and 9 is expected)",
                    arglist, digitChar);
        }

        // consume quoted value
        if (currentCursor < arglist.length() && arglist.charAt(currentCursor) == '=') {
            // consume '='
            currentCursor++;

            // consume quote
            if (currentCursor < arglist.length() && arglist.charAt(currentCursor) != '\'') {
                throw new InvalidArgumentException(
                        "Unable to parse Arguments [%s] : opening quote ['] is expected at index %d after [%s]",
                        arglist, currentCursor, arglist.substring(0, currentCursor));
            }
            currentCursor++;

            // consume value
            int indexOf = arglist.indexOf('\'', currentCursor);
            if (indexOf == -1) {
                throw new InvalidArgumentException(
                        "Unable to parse Arguments [%s] : Argument value must end with quote['].", arglist);
            } else {
                value = arglist.substring(currentCursor, indexOf);
            }
            currentCursor = indexOf;
            // move cursor after quote
            currentCursor++;
        }

        builder.addArgument(digit, value);
        return currentCursor;
    }

    /**
     * Serialize {$link Arguments} into text representation. In case of empty arguments returns null.
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();

        for (Argument argument : getValues()) {
            if (sb.length() > 0) {
                sb.append(",");
            }

            sb.append(argument.getDigit());

            if (argument.getValue() != null) {
                sb.append("='");
                sb.append(argument.getValue());
                sb.append("'");
            }
        }

        if (sb.toString().length() == 0) {
            return null;
        }

        return sb.toString();
    }

    /**
     * Create a builder for {$link Arguments}.
     */
    public static ArgumentsBuilder builder() {
        return new ArgumentsBuilder();
    }

    /**
     * Create a builder for {$link Arguments}.
     */
    public static Arguments emptyArguments() {
        return new Arguments(Collections.emptyMap());
    }

    /**
     * Builder for creating {$link Arguments}.
     */
    public static class ArgumentsBuilder {

        private final List<PendingArgument> pendingArguments = new ArrayList<>();

        /**
         * Add an argument with digit and value.
         */
        public ArgumentsBuilder addArgument(int digit, String value) {
            pendingArguments.add(new PendingArgument(digit, value));
            return this;
        }

        /**
         * Add an argument with digit and no value.
         */
        public ArgumentsBuilder addArgument(int digit) {
            pendingArguments.add(new PendingArgument(digit, null));
            return this;
        }

        /**
         * Add an argument.
         */
        public void addArgument(Argument argument) {
            if (argument != null) {
                addArgument(argument.getDigit(), argument.getValue());
            }
        }

        /**
         * Builds {$link Arguments}.
         *
         * @throws InvalidArgumentException In case of any argument is invalid or digit exists more than once.
         */
        public Arguments build() throws InvalidArgumentException {
            Map<Integer, Argument> argumentMap = new LinkedHashMap<>();

            for (PendingArgument argument : pendingArguments) {
                if (argumentMap.containsKey(argument.digit)) {
                    throw new InvalidArgumentException("Unable to build Arguments : digit [%d] exists more than once",
                            argument.digit);
                }
                argumentMap.put(argument.digit, new Argument(argument.digit, argument.value));
            }
            return new Arguments(argumentMap);
        }

        private static class PendingArgument {
            private final Integer digit;
            private final String value;

            public PendingArgument(Integer digit, String value) {
                this.digit = digit;
                this.value = value;
            }
        }
    }
}

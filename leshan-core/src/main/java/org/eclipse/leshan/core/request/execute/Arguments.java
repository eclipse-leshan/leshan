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
package org.eclipse.leshan.core.request.execute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.leshan.core.util.StringUtils;

/**
 * Arguments for Execute Operation.
 */
public class Arguments {

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
     * Checks if  {@link Arguments} has {@link Argument} with digit.
     */
    public boolean hasDigit(int digit) {
        for (Integer mapKey: argumentMap.keySet()) {
            if (mapKey == digit) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets {@link Argument} by digit.
     *
     * @throws IllegalArgumentException if digit doesn't exists in {@link Arguments}
     */
    public Argument get(int digit) {
        if (!hasDigit(digit)) {
            throw new IllegalArgumentException();
        }

        return argumentMap.get(digit);
    }

    /**
     * Gets digit keys of arguments.
     */
    public Set<Integer> getDigits() {
        return argumentMap.keySet();
    }

    /**
     * Gets collection of {@link Argument}.
     */
    public Collection<Argument> getValues() {
        return argumentMap.values();
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
    static Arguments parse(String arglist) throws InvalidArgumentException {
        Map<Integer, Argument> argumentMap = new HashMap<>();

        if (arglist != null && !arglist.isEmpty()) {
            int beginProbe = 0;
            int validBegin = 0;
            while (true) {
                int separatorIndex = arglist.indexOf(',', beginProbe);
                if (separatorIndex == -1) {
                    break;
                }
                String argumentText = arglist.substring(validBegin, separatorIndex);

                ArgumentParser argumentParser = new ArgumentParser(argumentText, arglist);
                if (argumentParser.isValid()) {
                    Argument argument = argumentParser.parse();
                    argumentMap.put(argument.getDigit(), argument);
                    validBegin = separatorIndex + 1;
                }
                beginProbe = separatorIndex + 1;
            }

            String argumentText = arglist.substring(beginProbe);
            ArgumentParser argumentParser = new ArgumentParser(argumentText, arglist);
            Argument argument = argumentParser.parse();
            argumentMap.put(argument.getDigit(), argument);
        }

        return new Arguments(argumentMap);
    }

    private static class ArgumentParser {
        private final String digitPart;
        private final String valueDecorated;
        private final String content;

        public ArgumentParser(String argument, String content) {
            this.content = content;
            String[] keyValue = argument.split("=", 2);
            digitPart = keyValue[0];
            valueDecorated = keyValue.length == 1 ? null : keyValue[1];
        }

        private Argument parse() throws InvalidArgumentException {
            if (!isValid()) {
                throw new InvalidArgumentException("Unable to parse Arguments [%s]", content);
            }
            int digit;
            try {
                digit = Integer.parseInt(digitPart);
            } catch (NumberFormatException e) {
                throw new InvalidArgumentException(e, "Unable to parse Arguments [%s] with digit [%s]", content, digitPart);
            }
            String value = null;
            if (valueDecorated != null) {
                value = StringUtils.removeEnd(StringUtils.removeStart(valueDecorated, "'"), "'");
            }

            return new Argument(digit, value);
        }

        private boolean isValid() {
            if (digitPart.length() != 1) {
                return false;
            }

            if (valueDecorated != null) {
                return valueDecorated.length() >= 2 && valueDecorated.charAt(0) == '\''
                        && valueDecorated.charAt(valueDecorated.length() - 1) == '\'';
            }
            return true;
        }
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
     * Creates a map that represents arguments.
     */
    public Map<Integer, String> toMap() {
        Map<Integer, String> result = new HashMap<>();
        for (Argument argument : getValues()) {
            String value = argument.getValue();
            result.put(argument.getDigit(), value);
        }
        return result;
    }

    /**
     * Create a builder for {$link Arguments}.
     */
    public static ArgumentsBuilder builder() {
        return new ArgumentsBuilder();
    }

    /**
     * Builder for creating {$link Arguments}.
     */
    public static class ArgumentsBuilder {

        private final List<Map<Integer, String>> keyValuePairs = new ArrayList<>();

        /**
         * Add an argument with digit and value.
         */
        public ArgumentsBuilder addArgument(int digit, String value) {
            keyValuePairs.add(Collections.singletonMap(digit, value));
            return this;
        }

        /**
         * Add an argument with digit with no value.
         */
        public ArgumentsBuilder addArgument(int digit) {
            keyValuePairs.add(Collections.singletonMap(digit, (String) null));
            return this;
        }

        /**
         * Builds {$link Arguments}.
         *
         * @throws InvalidArgumentException In case of any argument is invalid or digit exists more than once.
         */
        public Arguments build() throws InvalidArgumentException {
            Map<Integer, Argument> argumentMap = new HashMap<>();

            for (Map<Integer, String> pair : keyValuePairs) {
                Integer digit = pair.keySet().iterator().next();
                String value = pair.get(digit);
                if (argumentMap.containsKey(digit)) {
                    throw new InvalidArgumentException("Unable to build Arguments : %d digit exists more than once", digit);
                }
                argumentMap.put(digit, new Argument(digit, value));
            }
            return new Arguments(argumentMap);
        }
    }
}

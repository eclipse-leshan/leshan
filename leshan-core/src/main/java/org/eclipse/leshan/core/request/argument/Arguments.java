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
        return argumentMap.keySet();
    }

    /**
     * Gets collection of {@link Argument}.
     */
    public Collection<Argument> getValues() {
        return argumentMap.values();
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
    static Arguments parse(String arglist) throws InvalidArgumentException {
        ArgumentsBuilder builder = builder();

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
                ArgumentParser.ValidationError validationError = argumentParser.isValid();

                if (argumentParser.isValid() == ArgumentParser.ValidationError.NO_ERROR) {
                    Argument argument = argumentParser.parse();
                    builder.addArgument(argument);
                    validBegin = separatorIndex + 1;
                } else {
                    if (validationError == ArgumentParser.ValidationError.INVALID_DIGIT_FORMAT
                            || validationError == ArgumentParser.ValidationError.INVALID_DIGIT_RANGE) {
                        throw new InvalidArgumentException(validationError.getValidationMessage(argumentText,
                                argumentParser.digitPart, argumentParser.valueDecorated));
                    }
                }
                beginProbe = separatorIndex + 1;
            }

            String argumentText = arglist.substring(beginProbe);
            ArgumentParser argumentParser = new ArgumentParser(argumentText, arglist);
            Argument argument = argumentParser.parse();
            builder.addArgument(argument);
        }

        // Use builder capability to validate arguments uniqueness.
        return builder.build();
    }

    private static class ArgumentParser {

        private enum ValidationError {
            NO_ERROR, INVALID_DIGIT_FORMAT, INVALID_DIGIT_RANGE, VALUE_SHOULD_HAVE_QUOTES;

            public String getValidationMessage(String content, String digitPart, String valueDecorated) {
                switch (this) {
                case INVALID_DIGIT_FORMAT:
                    return String.format("Unable to parse Argument [%s] : Invalid digit format : [%s]", content,
                            digitPart);
                case INVALID_DIGIT_RANGE:
                    return String.format(
                            "Unable to parse Argument [%s] : Digit should be between 0 and 9 but was : [%s]", content,
                            digitPart);
                case VALUE_SHOULD_HAVE_QUOTES:
                    return String.format(
                            "Unable to parse Argument [%s] : Argument value should be quoted but was: [%s]", content,
                            valueDecorated);
                case NO_ERROR:
                default:
                    return null;
                }
            }
        }

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
            ValidationError validationError = isValid();
            if (validationError != ValidationError.NO_ERROR) {
                throw new InvalidArgumentException(
                        validationError.getValidationMessage(content, digitPart, valueDecorated));
            }

            String value = null;
            if (valueDecorated != null) {
                value = StringUtils.removeEnd(StringUtils.removeStart(valueDecorated, "'"), "'");
            }

            return new Argument(Integer.parseInt(digitPart), value);
        }

        private ValidationError isValid() {
            if (digitPart.length() == 0) {
                return ValidationError.INVALID_DIGIT_FORMAT;
            }

            int digit;

            try {
                digit = Integer.parseInt(digitPart);
            } catch (NumberFormatException e) {
                return ValidationError.INVALID_DIGIT_FORMAT;
            }

            if (digit < 0 || digit > 9) {
                return ValidationError.INVALID_DIGIT_RANGE;
            }

            if (valueDecorated != null) {
                if (valueDecorated.length() < 2 || valueDecorated.charAt(0) != '\''
                        || valueDecorated.charAt(valueDecorated.length() - 1) != '\'') {
                    return ValidationError.VALUE_SHOULD_HAVE_QUOTES;
                }
            }

            return ValidationError.NO_ERROR;
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
            Map<Integer, Argument> argumentMap = new HashMap<>();

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

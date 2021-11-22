package org.eclipse.leshan.core.request.execute;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.util.StringUtils;

public class Arguments implements Iterable<Argument> {

    private final List<Argument> argumentList;

    private Arguments(List<Argument> argumentList) {
        this.argumentList = argumentList;
    }

    @Override
    public String toString() {
        return String.format("Arguments [argumentList=%s]", argumentList.toString());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((argumentList == null) ? 0 : argumentList.hashCode());
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
        if (argumentList == null) {
            if (other.argumentList != null)
                return false;
        } else if (!argumentList.equals(other.argumentList))
            return false;
        return true;
    }

    public int size() {
        return argumentList.size();
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    static Arguments parse(String content) throws InvalidArgumentException {
        List<Argument> argumentList = new ArrayList<>();

        if (content != null && !content.isEmpty()) {
            int beginProbe = 0;
            int validBegin = 0;
            while (true) {
                int separatorIndex = content.indexOf(',', beginProbe);
                if (separatorIndex == -1) {
                    break;
                }
                String argument = content.substring(validBegin, separatorIndex);

                ArgumentParser argumentParser = new ArgumentParser(argument, content);
                if (argumentParser.isValid()) {
                    argumentList.add(argumentParser.parse());
                    validBegin = separatorIndex + 1;
                }
                beginProbe = separatorIndex + 1;
            }

            String argument = content.substring(beginProbe);
            ArgumentParser argumentParser = new ArgumentParser(argument, content);
            argumentList.add(argumentParser.parse());
        }

        return new Arguments(argumentList);
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

        private void validate() throws InvalidArgumentException {
            if (!isValid()) {
                throw new InvalidArgumentException("Unable to parse Arguments [%s]", content);
            }
        }

        public Argument parse() throws InvalidArgumentException {
            validate();
            int digit = Integer.parseInt(digitPart);
            String value = null;
            if (valueDecorated != null) {
                value = StringUtils.removeEnd(StringUtils.removeStart(valueDecorated, "'"), "'");
            }

            return new Argument(digit, value);
        }

        public boolean isValid() {
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

    public byte[] serialize() {
        StringBuilder sb = new StringBuilder();

        for (Argument argument : this) {
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

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Creates a map that represents arguments.
     * Every argument's null value is converted to empty string "".
     */
    public Map<Integer, String> toMap() {
        Map<Integer, String> result = new HashMap<>();
        for (Argument argument: this) {
            String value = argument.getValue();
            result.put(argument.getDigit(), value != null ? value : "");
        }
        return result;
    }

    @Override
    public Iterator<Argument> iterator() {
        return argumentList.iterator();
    }

    public static ArgumentsBuilder builder() {
        return new ArgumentsBuilder();
    }

    public static class ArgumentsBuilder {

        private List<Map<Integer, String>> keyValuePairs = new ArrayList<>();

        public ArgumentsBuilder addArgument(int digit, String value) {
            keyValuePairs.add(Collections.singletonMap(digit, value));
            return this;
        }

        public ArgumentsBuilder addArgument(int digit) {
            keyValuePairs.add(Collections.singletonMap(digit, (String) null));
            return this;
        }

        public Arguments build() throws InvalidArgumentException {
            List<Argument> argumentList = new ArrayList<>();

            for (Map<Integer, String> pair: keyValuePairs) {
                Integer digit = pair.keySet().iterator().next();
                String value = pair.get(digit);
                argumentList.add(new Argument(digit, value));
            }
            return new Arguments(argumentList);
        }
    }
}

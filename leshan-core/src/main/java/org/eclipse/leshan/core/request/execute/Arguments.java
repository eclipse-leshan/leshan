package org.eclipse.leshan.core.request.execute;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Arguments implements Iterable<Argument> {

    private final List<Argument> argumentList;

    public Arguments(List<Argument> argumentList) {
        this.argumentList = argumentList;
    }

    public Arguments(Map<Integer, String> argumentsMap) {
        this(buildArgumentList(argumentsMap));
    }

    private static List<Argument> buildArgumentList(Map<Integer, String> argumentsMap) {
        List<Argument> argumentList = new ArrayList<>(2);
        for (Integer key : argumentsMap.keySet()) {
            argumentList.add(new Argument(key, argumentsMap.get(key)));
        }
        return argumentList;
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

    static Arguments parse(String content) {
        List<Argument> argumentList = new ArrayList<>();

        if (content != null && !content.isEmpty()) {
            String[] arguments = content.split(",");
            for (String argument : arguments) {
                String[] keyValue = argument.split("=");
                String value = keyValue.length == 1 ? null : keyValue[1].substring(1, keyValue[1].length() - 1);
                argumentList.add(new Argument(Integer.parseInt(keyValue[0]), value));
            }
        }

        return new Arguments(argumentList);
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
}

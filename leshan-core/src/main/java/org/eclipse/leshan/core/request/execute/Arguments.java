package org.eclipse.leshan.core.request.execute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Arguments implements Iterable<SingleArgument> {

    private final List<SingleArgument> argumentList;

    public Arguments(List<SingleArgument> argumentList) {
        this.argumentList = argumentList;
    }

    public Arguments(Map<Integer, String> argumentsMap) {
        this(buildSingleArguments(argumentsMap));
    }

    private static List<SingleArgument> buildSingleArguments(Map<Integer, String> argumentsMap) {
        List<SingleArgument> argumentList = new ArrayList<>(2);
        for (Integer key : argumentsMap.keySet()) {
            argumentList.add(new SingleArgument(key, argumentsMap.get(key)));
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

    /**
     * Creates a map that represents arguments.
     * Every argument's null value is converted to empty string "".
     */
    public Map<Integer, String> toMap() {
        Map<Integer, String> result = new HashMap<>();
        for (SingleArgument argument: this) {
            String value = argument.getValue();
            result.put(argument.getDigit(), value != null ? value : "");
        }
        return result;
    }

    static class ArgumentIterator implements Iterator<SingleArgument> {

        private final Iterator<SingleArgument> iterator;

        public ArgumentIterator(List<SingleArgument> argumentList) {
            iterator = argumentList.iterator();
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public SingleArgument next() {
            return iterator.next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }
    }

    @Override
    public Iterator<SingleArgument> iterator() {
        return new ArgumentIterator(argumentList);
    }

}

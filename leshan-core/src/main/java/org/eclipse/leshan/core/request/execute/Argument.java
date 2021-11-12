package org.eclipse.leshan.core.request.execute;

public class Argument {

    private final int digit;

    private final String value;

    public Argument(int digit, String value) {
        this.digit = digit;
        this.value = value;
    }

    public Argument(int digit) {
        this.digit = digit;
        this.value = null;
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

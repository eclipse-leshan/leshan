package org.eclipse.leshan.core.request.execute;

import java.util.regex.Pattern;

public class Argument {

    private static final Pattern valuePattern = Pattern.compile("[!\\x23-\\x26\\x28-\\x5B\\x5D-\\x7E]*");

    private final int digit;

    private final String value;

    public Argument(int digit, String value) throws InvalidArgumentException {
        validateDigit(digit);
        validateValue(digit, value);
        this.digit = digit;
        this.value = value;
    }

    public Argument(int digit) throws InvalidArgumentException {
        validateDigit(digit);
        this.digit = digit;
        this.value = null;
    }

    private void validateDigit(int digit) throws InvalidArgumentException {
        if (digit < 0 || digit > 9) {
            throw new InvalidArgumentException("Invalid Argument digit [%s]", Integer.toString(digit));
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

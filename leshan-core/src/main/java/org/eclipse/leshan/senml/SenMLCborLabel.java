package org.eclipse.leshan.senml;

public class SenMLCborLabel implements SenMLLabel {

    private static final String BASE_NAME = "-2";
    private static final String BASE_TIME = "-3";

    private static final String NAME = "0";
    private static final String TIME = "6";

    private static final String NUMBER_VALUE = "2";
    private static final String STRING_VALUE = "3";
    private static final String BOOLEAN_VALUE = "4";
    private static final String DATA_VALUE = "8";

    @Override
    public String getBaseName() {
        return BASE_NAME;
    }

    @Override
    public String getBaseTime() {
        return BASE_TIME;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getTime() {
        return TIME;
    }

    @Override
    public String getNumberValue() {
        return NUMBER_VALUE;
    }

    @Override
    public String getStringValue() {
        return STRING_VALUE;
    }

    @Override
    public String getBooleanValue() {
        return BOOLEAN_VALUE;
    }

    @Override
    public String getDataValue() {
        return DATA_VALUE;
    }

    @Override
    public boolean isBaseName(String label) {
        return label != null && label.equals(BASE_NAME);
    }

    @Override
    public boolean isBaseTime(String label) {
        return label != null && label.equals(BASE_TIME);
    }

    @Override
    public boolean isName(String label) {
        return label != null && label.equals(NAME);
    }

    @Override
    public boolean isTime(String label) {
        return label != null && label.equals(TIME);
    }

    @Override
    public boolean isNumberValue(String label) {
        return label != null && label.equals(NUMBER_VALUE);
    }

    @Override
    public boolean isStringValue(String label) {
        return label != null && label.equals(STRING_VALUE);
    }

    @Override
    public boolean isBooleanValue(String label) {
        return label != null && label.equals(BOOLEAN_VALUE);
    }

    @Override
    public boolean isDataValue(String label) {
        return label != null && label.equals(DATA_VALUE);
    }
}

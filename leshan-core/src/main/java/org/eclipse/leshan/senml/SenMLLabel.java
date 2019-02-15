package org.eclipse.leshan.senml;

public interface SenMLLabel {
    String getBaseName();

    String getBaseTime();

    String getName();

    String getTime();

    String getNumberValue();

    String getStringValue();

    String getBooleanValue();

    String getDataValue();

    boolean isBaseName(String label);

    boolean isBaseTime(String label);

    boolean isName(String label);

    boolean isTime(String label);

    boolean isNumberValue(String label);

    boolean isStringValue(String label);

    boolean isBooleanValue(String label);

    boolean isDataValue(String label);
}
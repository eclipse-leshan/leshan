/*******************************************************************************
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Cavenaghi9 - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.senml;

public class SenMLJsonLabel implements SenMLLabel {
    private static final String BASE_NAME = "bn";
    private static final String BASE_TIME = "bt";

    private static final String NAME = "n";
    private static final String TIME = "t";

    private static final String NUMBER_VALUE = "v";
    private static final String STRING_VALUE = "vs";
    private static final String BOOLEAN_VALUE = "vb";
    private static final String DATA_VALUE = "vd";

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

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
 *     Boya Zhang - initial API and implementation
 *******************************************************************************/

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
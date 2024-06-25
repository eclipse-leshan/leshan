/*******************************************************************************
 * Copyright (c) 2022 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.util;

public interface TestLwM2mId {
    static final int TEST_OBJECT = 3442;

    /* TEST RESOURCES */
    static final int RESET_VALUES = 0;
    static final int RANDOMIZE_VALUES = 1;
    static final int CLEAR_VALUES = 2;

    static final int STRING_VALUE = 110;
    static final int INTEGER_VALUE = 120;
    static final int UNSIGNED_INTEGER_VALUE = 125;
    static final int FLOAT_VALUE = 130;
    static final int BOOLEAN_VALUE = 140;
    static final int OPAQUE_VALUE = 150;
    static final int TIME_VALUE = 160;
    static final int OBJLINK_VALUE = 170;
    static final int CORELNK_VALUE = 180;

    static final int MULTIPLE_STRING_VALUE = 1110;
    static final int MULTIPLE_INTEGER_VALUE = 1120;
    static final int MULTIPLE_UNSIGNED_INTEGER_VALUE = 1125;
    static final int MULTIPLE_FLOAT_VALUE = 1130;
    static final int MULTIPLE_BOOLEAN_VALUE = 1140;
    static final int MULTIPLE_OPAQUE_VALUE = 1150;
    static final int MULTIPLE_TIME_VALUE = 1160;
    static final int MULTIPLE_OBJLINK_VALUE = 1170;
    static final int MULTIPLE_CORELNK_VALUE = 1180;
}

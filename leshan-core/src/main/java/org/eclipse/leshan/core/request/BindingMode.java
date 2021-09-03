/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
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
package org.eclipse.leshan.core.request;

import java.util.EnumSet;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;

/**
 * Transport binding and Queue Mode
 */
public enum BindingMode {

    /** UDP */
    U,

    /** TCP */
    T,

    /** SMS */
    S,

    /** Non-Ip */
    N,

    /** Queue Mode : removed since LWM2M 1.1 */
    Q;

    /**
     * @param targetVersion the target LWM2M version
     * @return null if the BindingMode value is compatible with the given LWM2M version, else return an error message.
     */
    public String isValidFor(LwM2mVersion targetVersion) {
        switch (this) {
        case T:
        case N:
            if (targetVersion.olderThan(LwM2mVersion.V1_1)) {
                return String.format("%s is supported since LWM2M 1.1", this);
            }
            break;
        case Q:
            if (targetVersion.newerThan(LwM2mVersion.V1_0)) {
                return String.format("%s is not supported since LWM2M 1.1", this);
            }
            break;
        default:
        }
        return null;
    }

    private static BindingMode valueOf(char c) {
        switch (c) {
        case 'U':
            return U;
        case 'T':
            return T;
        case 'S':
            return S;
        case 'N':
            return N;
        case 'Q':
            return Q;
        default:
            throw new IllegalArgumentException("No enum constant " + c + ".");

        }
    }

    /**
     * @param bindings bindings to check
     * @param targetVersion the target LWM2M version
     * @return null if the bindings are compatible with the given LWM2M version, else return an error message.
     */
    public static String isValidFor(EnumSet<BindingMode> bindings, LwM2mVersion targetVersion) {
        for (BindingMode binding : bindings) {
            String err = binding.isValidFor(targetVersion);
            if (err != null)
                return err;
        }
        return null;
    }

    public static String toString(EnumSet<BindingMode> bindings) {
        StringBuilder b = new StringBuilder();
        for (BindingMode binding : bindings) {
            b.append(binding);
        }
        return b.toString();
    }

    public static EnumSet<BindingMode> parse(String bindings) {
        EnumSet<BindingMode> res = EnumSet.noneOf(BindingMode.class);
        for (int i = 0; i < bindings.length(); i++) {
            res.add(BindingMode.valueOf(bindings.charAt(i)));
        }
        return res;
    }
}

/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.core.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

public class LeshanProperties {

    public static final String VERSION = "version";
    public static final String COMMIT_ID = "commitid";
    public static final String TIMESTAMP = "timestamp";

    private Properties prop;

    public void load() throws IOException {
        prop = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/leshan.properties")) {
            prop.load(in);
        }
    }

    public String getVersion() {
        String version = prop.getProperty(VERSION);
        if (!hasRealValue(version)) {
            return "???";
        } else {
            return version;
        }
    }

    public String getCommitId() {
        String commitId = prop.getProperty(COMMIT_ID);
        if (!hasRealValue(commitId)) {
            return "???";
        } else {
            return commitId;
        }
    }

    public Long getTimestamp() {
        String timestamp = prop.getProperty(TIMESTAMP);
        if (!hasRealValue(timestamp)) {
            return null;
        } else {
            return Long.parseLong(timestamp);
        }
    }

    public String getBuildDateAsString() {
        Long timestamp = getTimestamp();
        if (timestamp == null) {
            return "???";
        } else {
            return new Date(timestamp).toString();
        }
    }

    public String getCodeURL() {
        String version = prop.getProperty(VERSION);
        String commitId = prop.getProperty(COMMIT_ID);

        if (hasRealValue(version) && !isSnapShot(version)) {
            return "https://github.com/eclipse/leshan/tree/leshan-" + version;
        } else if (hasRealValue(commitId)) {
            return "https://github.com/eclipse/leshan/tree/" + commitId;
        } else {
            return "https://github.com/eclipse/leshan";
        }
    }

    private static boolean hasRealValue(String name) {
        // check if this is the real value or the template one.
        // (In dev mode this could be the template one else this looks like a bug)
        return name != null && !(name.startsWith("${") && name.endsWith("}"));
    }

    private static boolean isSnapShot(String version) {
        return version.endsWith("SNAPSHOT");
    }

}

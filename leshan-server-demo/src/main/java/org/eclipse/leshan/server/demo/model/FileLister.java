/*******************************************************************************
 * Copyright (c) 2020 Sierra Wireless and others.
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
package org.eclipse.leshan.server.demo.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

import org.eclipse.leshan.server.demo.LeshanServerDemo;

/**
 * An helper to generate {@code private final static String[] modelPaths} in {@link LeshanServerDemo} and
 * LeshanClientDemo.
 */
public class FileLister {
    public static void main(String[] args) throws FileNotFoundException {
        String ddfFilesPath = Ddf2JsonGenerator.DEFAULT_DDF_FILES_PATH;

        File input = new File(ddfFilesPath);

        // check input exists
        if (!input.exists())
            throw new FileNotFoundException(input.toString());

        // get input files.
        File[] files;
        if (input.isDirectory()) {
            files = input.listFiles();
        } else {
            files = new File[] { input };
        }

        StringBuilder builder = new StringBuilder();
        Arrays.sort(files);
        for (File file : files) {
            builder.append("\"").append(file.getName()).append("\"").append(",");
        }

        System.out.println(builder.toString());

    }
}

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
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.leshan.core.model.DDFFileParser;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.server.demo.LeshanServerDemo;

/**
 * An helper to generate {@code private final static String[] modelPaths} in {@link LeshanServerDemo} and
 * LeshanClientDemo.
 */
public class FileLister {
    public static void main(String[] args) throws InvalidDDFFileException, IOException {
        String ddfFilesPath = DdfDownloader.DOWNLOAD_FOLDER_PATH;
        if (args.length >= 1)
            ddfFilesPath = args[0]; // the path to folder to list

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
        SortedMap<String, String> sortedFiles = new TreeMap<>();
        for (File file : files) {
            DDFFileParser ddfFileParser = new DDFFileParser();
            ObjectModel model = ddfFileParser.parse(file).iterator().next();
            sortedFiles.put(String.format("%10d-%s", model.id, model.version), file.getName());
        }
        for (String name : sortedFiles.values()) {
            builder.append("\"").append(name).append("\"").append(",");
        }

        System.out.println(builder.toString());

    }
}

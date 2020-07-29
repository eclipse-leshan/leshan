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
package org.eclipse.leshan.server.demo.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.leshan.core.model.DDFFileParser;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.util.json.JsonException;

public class Ddf2JsonGenerator {

    static final String DEFAULT_DDF_FILES_PATH = "ddffiles";
    static final String DEFAULT_OUTPUT_PATH = "src/main/resources/objectspec.json";

    private void generate(Collection<ObjectModel> objectModels, OutputStream output) throws IOException, JsonException {
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(output)) {
            outputStreamWriter.write(new ObjectModelSerDes().sSerialize(objectModels));
        }
    }

    public void generate(File input, OutputStream output) throws IOException, JsonException, InvalidDDFFileException {
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

        // parse DDF file
        List<ObjectModel> objectModels = new ArrayList<>();
        DDFFileParser ddfParser = new DDFFileParser();
        for (File f : files) {
            if (f.canRead()) {
                objectModels.addAll(ddfParser.parse(f));
            }
        }

        // sort object by id
        Collections.sort(objectModels, new Comparator<ObjectModel>() {
            @Override
            public int compare(ObjectModel o1, ObjectModel o2) {
                return o1.id - o2.id;
            }
        });

        // generate json
        generate(objectModels, output);
    }

    public static void main(String[] args)
            throws FileNotFoundException, IOException, JsonException, InvalidDDFFileException {
        // default value
        String ddfFilesPath = DEFAULT_DDF_FILES_PATH;
        String outputPath = DEFAULT_OUTPUT_PATH;

        // use arguments if they exit
        if (args.length >= 1)
            ddfFilesPath = args[0]; // the path to a DDF file or a folder which contains DDF files.
        if (args.length >= 2)
            outputPath = args[1]; // the path of the output file.

        // generate object spec file
        Ddf2JsonGenerator ddfJsonGenerator = new Ddf2JsonGenerator();
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath)) {
            ddfJsonGenerator.generate(new File(ddfFilesPath), fileOutputStream);
        }
    }
}

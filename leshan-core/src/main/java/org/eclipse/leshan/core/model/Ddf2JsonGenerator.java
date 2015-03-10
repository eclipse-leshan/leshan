/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.model;

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

import org.eclipse.leshan.core.model.json.ObjectModelSerializer;
import org.eclipse.leshan.core.model.json.ResourceModelSerializer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Ddf2JsonGenerator {

    private Gson gson;

    public Ddf2JsonGenerator() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ObjectModel.class, new ObjectModelSerializer());
        gsonBuilder.registerTypeAdapter(ResourceModel.class, new ResourceModelSerializer());
        gsonBuilder.setPrettyPrinting();
        gson = gsonBuilder.create();
    }

    private void generate(Collection<ObjectModel> objectModels, OutputStream output) throws IOException {
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(output)) {
            gson.toJson(objectModels, outputStreamWriter);
        }
    }

    private void generate(File input, OutputStream output) throws IOException {
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
        List<ObjectModel> objectModels = new ArrayList<ObjectModel>();
        DDFFileParser ddfParser = new DDFFileParser();
        for (File f : files) {
            if (f.canRead()) {
                ObjectModel objectModel = ddfParser.parse(f);
                if (objectModel != null) {
                    objectModels.add(objectModel);
                }
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

    public static void main(String[] args) throws FileNotFoundException, IOException {
        // default value
        String ddfFilesPath = "ddffiles";
        String outputPath = "src/main/resources/objectspec.json";

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

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.leshan.core.model.json.ObjectModelDeserializer;
import org.eclipse.leshan.core.model.json.ResourceModelDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ObjectLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectLoader.class);

    private static final Gson GSON;

    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ObjectModel.class, new ObjectModelDeserializer());
        gsonBuilder.registerTypeAdapter(ResourceModel.class, new ResourceModelDeserializer());
        GSON = gsonBuilder.create();
    }

    /**
     * Load the default LWM2M objects
     */
    public static List<ObjectModel> loadDefault() {
        List<ObjectModel> models = new ArrayList<>();

        // standard objects
        LOG.debug("Loading OMA standard object models");
        InputStream input = ObjectLoader.class.getResourceAsStream("/oma-objects-spec.json");
        if (input != null) {
            try (Reader reader = new InputStreamReader(input)) {
                models.addAll(loadJsonStream(input));
            } catch (IOException e) {
                LOG.error("Unable to load object models", e);
            }
        }

        // custom objects (environment variable)
        String modelsFolderEnvVar = System.getenv("MODELS_FOLDER");
        if (modelsFolderEnvVar != null) {
            models.addAll(loadObjectsFromDir(new File(modelsFolderEnvVar)));
        }

        return models;
    }

    /**
     * Load object definitions from DDF or JSON files.
     * 
     * @param modelDir the directory containing the object definition files.
     */
    public static List<ObjectModel> load(File modelDir) {
        return loadObjectsFromDir(modelDir);
    }

    /**
     * Load object definition from DDF file.
     * 
     * @param input An inputStream to a DFF file.
     * @param streamName A name for the stream used for logging only
     */
    public static ObjectModel loadDdfFile(InputStream input, String streamName) {
        DDFFileParser ddfFileParser = new DDFFileParser();
        return ddfFileParser.parse(input, streamName);
    }

    /**
     * Load object definitions from JSON stream.
     * 
     * @param input An inputStream to a JSON stream.
     */
    public static List<ObjectModel> loadJsonStream(InputStream input) {
        List<ObjectModel> models = new ArrayList<>();
        Reader reader = new InputStreamReader(input);
        ObjectModel[] objectModels = GSON.fromJson(reader, ObjectModel[].class);
        for (ObjectModel objectModel : objectModels) {
            models.add(objectModel);
        }
        return models;
    }

    /*
     * Load object definitions from files
     */
    private static List<ObjectModel> loadObjectsFromDir(File modelsDir) {
        List<ObjectModel> models = new ArrayList<>();

        // check if the folder is usable
        if (!modelsDir.isDirectory() || !modelsDir.canRead()) {
            LOG.error(MessageFormat.format(
                    "Models folder {0} is not a directory or you are not allowed to list its content",
                    modelsDir.getPath()));
        } else {
            // get all files
            for (File file : modelsDir.listFiles()) {
                if (!file.canRead())
                    continue;

                if (file.getName().endsWith(".xml")) {
                    // from DDF file
                    LOG.debug("Loading object models from DDF file {}", file.getAbsolutePath());
                    try (FileInputStream input = new FileInputStream(file)) {
                        ObjectModel objectModel = loadDdfFile(input, file.getName());
                        if (objectModel != null) {
                            models.add(objectModel);
                        }
                    } catch (IOException e) {
                        LOG.warn(MessageFormat.format("Unable to load object models for {0}", file.getAbsolutePath()),
                                e);
                    }

                } else if (file.getName().endsWith(".json")) {
                    // from JSON file
                    LOG.debug("Loading object models from JSON file {}", file.getAbsolutePath());
                    try (FileInputStream input = new FileInputStream(file)) {
                        models.addAll(loadJsonStream(input));
                    } catch (IOException e) {
                        LOG.warn(MessageFormat.format("Unable to load object models for {0}", file.getAbsolutePath()),
                                e);
                    }
                }
            }
        }
        return models;
    }
}

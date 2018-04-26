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

import org.eclipse.leshan.core.model.json.ObjectModelSerDes;
import org.eclipse.leshan.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonValue;

public class ObjectLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectLoader.class);

    private static final String[] ddfpaths = new String[] { "LWM2M_Security-v1_0.xml", "LWM2M_Server-v1_0.xml",
                            "LWM2M_Access_Control-v1_0.xml", "LWM2M_Device-v1_0.xml",
                            "LWM2M_Connectivity_Monitoring-v1_0.xml", "LWM2M_Firmware_Update-v1_0.xml",
                            "LWM2M_Location-v1_0.xml", "LWM2M_Connectivity_Statistics-v1_0.xml" };

    /**
     * Load the default LWM2M objects
     */
    public static List<ObjectModel> loadDefault() {
        List<ObjectModel> models = new ArrayList<>();

        // standard objects
        LOG.debug("Loading OMA standard object models");
        models.addAll(loadDdfResources("/models/", ddfpaths));

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
     * @param input An inputStream to a DDF file.
     * @param streamName A name for the stream used for logging only
     */
    public static List<ObjectModel> loadDdfFile(InputStream input, String streamName) {
        DDFFileParser ddfFileParser = new DDFFileParser();
        return ddfFileParser.parse(input, streamName);
    }

    /**
     * Load object definitions from JSON stream.
     * 
     * @param input An inputStream to a JSON stream.
     */
    public static List<ObjectModel> loadJsonStream(InputStream input) {
        try {
            Reader reader = new InputStreamReader(input);
            JsonValue json = Json.parse(reader);
            return new ObjectModelSerDes().deserialize(json.asArray());
        } catch (IOException e) {
            LOG.error("Cannot load json model from inputstream");
        }
        return null;
    }

    /**
     * Load object definition from DDF resources following rules of {@link Class#getResourceAsStream(String)}.
     * 
     * It should be used to load DDF embedded with your application bundle (e.g. jar, war, ...)
     * 
     * @param path directory path to the DDF files
     * @param filenames names of all the DDF files
     */
    public static List<ObjectModel> loadDdfResources(String path, String[] filenames) {
        List<ObjectModel> models = new ArrayList<>();
        for (String filename : filenames) {
            String fullpath = StringUtils.removeEnd(path, "/") + "/" + StringUtils.removeStart(filename, "/");
            InputStream input = ObjectLoader.class.getResourceAsStream(fullpath);
            if (input != null) {
                try (Reader reader = new InputStreamReader(input)) {
                    models.addAll(loadDdfFile(input, fullpath));
                } catch (IOException e) {
                    throw new IllegalStateException(String.format("Unable to load model %s", fullpath), e);
                }
            } else {
                throw new IllegalStateException(String.format("Unable to load model %s", fullpath));
            }
        }
        return models;
    }

    /**
     * Load object definition from DDF resources following rules of {@link Class#getResourceAsStream(String)}.
     * 
     * It should be used to load DDF embedded with your application bundle (e.g. jar, war, ...)
     * 
     * @param paths An array of paths to DDF files.
     */
    public static List<ObjectModel> loadDdfResources(String[] paths) {
        List<ObjectModel> models = new ArrayList<>();
        for (String path : paths) {
            InputStream input = ObjectLoader.class.getResourceAsStream(path);
            if (input != null) {
                try (Reader reader = new InputStreamReader(input)) {
                    models.addAll(loadDdfFile(input, path));
                } catch (IOException e) {
                    throw new IllegalStateException(String.format("Unable to load model %s", path), e);
                }
            } else {
                throw new IllegalStateException(String.format("Unable to load model %s", path));
            }
        }
        return models;
    }

    /*
     * Load object definitions from files
     */
    public static List<ObjectModel> loadObjectsFromDir(File modelsDir) {
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
                        models.addAll(loadDdfFile(input, file.getName()));
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

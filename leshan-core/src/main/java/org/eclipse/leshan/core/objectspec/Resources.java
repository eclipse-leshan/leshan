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
package org.eclipse.leshan.core.objectspec;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.leshan.core.objectspec.json.ObjectSpecDeserializer;
import org.eclipse.leshan.core.objectspec.json.ResourceSpecDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The resource descriptions for registered LWM2M objects
 */
public class Resources {

    private static final Logger LOG = LoggerFactory.getLogger(Resources.class);

    private static final Map<Integer, ObjectSpec> OBJECTS = new HashMap<>(); // objects by ID

    private static final Gson GSON;

    // load objects definitions
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(ObjectSpec.class, new ObjectSpecDeserializer());
        gsonBuilder.registerTypeAdapter(ResourceSpec.class, new ResourceSpecDeserializer());
        GSON = gsonBuilder.create();

        // standard objects
        LOG.debug("Loading OMA standard object definitions");
        InputStream input = Resources.class.getResourceAsStream("/oma-objects-spec.json");
        if (input != null) {
            try (Reader reader = new InputStreamReader(input)) {
                ObjectSpec[] objectSpecs = GSON.fromJson(reader, ObjectSpec[].class);
                for (ObjectSpec objectSpec : objectSpecs) {
                    loadObject(objectSpec);
                }
            } catch (IOException e) {
                LOG.error("Unable to load object specification", e);
            }
        }

        // custom objects (environment variable)
        String modelsFolderEnvVar = System.getenv("MODELS_FOLDER");
        if (modelsFolderEnvVar != null) {
            loadObjectsFromDir(new File(modelsFolderEnvVar));
        }
    }

    /**
     * Load object definitions from DDF or JSON files.
     * 
     * @param modelDir the directory containing the object definition files.
     */
    public static void load(File modelDir) {
        loadObjectsFromDir(modelDir);
    }

    /**
     * Returns the description of a given resource.
     *
     * @param objectId the object identifier
     * @param resourceId the resource identifier
     * @return the resource specification or <code>null</code> if not found
     */
    public static ResourceSpec getResourceSpec(int objectId, int resourceId) {
        ObjectSpec object = OBJECTS.get(objectId);
        if (object != null) {
            return object.resources.get(resourceId);
        }
        return null;
    }

    /**
     * Returns the description of a given object.
     *
     * @param objectId the object identifier
     * @return the object specification or <code>null</code> if not found
     */
    public static ObjectSpec getObjectSpec(int objectId) {
        return OBJECTS.get(objectId);
    }

    /**
     * @return all the objects descriptions known.
     */
    public static Collection<ObjectSpec> getObjectSpecs() {
        return Collections.unmodifiableCollection(OBJECTS.values());
    }

    /*
     * Load object definitions from files
     */
    private static void loadObjectsFromDir(File modelsDir) {
        // check if the folder is usable
        if (!modelsDir.isDirectory() || !modelsDir.canRead()) {
            LOG.error(MessageFormat.format(
                    "Models folder {0} is not a directory or you are not allowed to list its content",
                    modelsDir.getPath()));
            return;
        }

        // get all files
        for (File file : modelsDir.listFiles()) {
            if (!file.canRead())
                continue;

            if (file.getName().endsWith(".xml")) {
                // from DDF file
                LOG.debug("Loading object definitions from DDF file {}", file.getAbsolutePath());
                DDFFileParser ddfFileParser = new DDFFileParser();
                ObjectSpec objectSpec = ddfFileParser.parse(file);
                if (objectSpec != null) {
                    loadObject(objectSpec);
                }
            } else if (file.getName().endsWith(".json")) {
                // from JSON file
                LOG.debug("Loading object definitions from JSON file {}", file.getAbsolutePath());
                try (Reader reader = new InputStreamReader(new FileInputStream(file))) {
                    ObjectSpec[] objectSpecs = GSON.fromJson(reader, ObjectSpec[].class);
                    for (ObjectSpec objectSpec : objectSpecs) {
                        loadObject(objectSpec);
                    }
                } catch (IOException e) {
                    LOG.warn(
                            MessageFormat.format("Unable to load object specification for {0}", file.getAbsolutePath()),
                            e);
                }
            }
        }
    }

    private static void loadObject(ObjectSpec objectSpec) {
        if (OBJECTS.containsKey(objectSpec.id)) {
            LOG.debug(MessageFormat.format("Definition already exists for object {0}. Overriding it.", objectSpec.id));
        }
        OBJECTS.put(objectSpec.id, objectSpec);
        LOG.debug("Object definition loaded: " + objectSpec);
    }
}

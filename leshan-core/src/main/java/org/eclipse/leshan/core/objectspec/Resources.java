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
 * The resource descriptions for registered LWM2M objects (only OMA objects for now).
 */
public class Resources {

    private static final Logger LOG = LoggerFactory.getLogger(Resources.class);

    private static final Map<Integer, ObjectSpec> OBJECTS = new HashMap<>(); // objects by ID

    /**
     * Initializes the list of LWM2M object definitions.
     */
    public static void load() {

        synchronized (OBJECTS) {
            if (OBJECTS.isEmpty()) {

                // load OMA objects definitions from json files
                InputStream input = Resources.class.getResourceAsStream("/objectspec.json");
                if (input == null) {
                    return;
                }

                GsonBuilder gsonBuilder = new GsonBuilder();
                gsonBuilder.registerTypeAdapter(ObjectSpec.class, new ObjectSpecDeserializer());
                gsonBuilder.registerTypeAdapter(ResourceSpec.class, new ResourceSpecDeserializer());
                Gson gson = gsonBuilder.create();

                try (Reader reader = new InputStreamReader(input)) {
                    ObjectSpec[] objectSpecs = gson.fromJson(reader, ObjectSpec[].class);
                    for (ObjectSpec objectSpec : objectSpecs) {
                        OBJECTS.put(objectSpec.id, objectSpec);
                    }
                } catch (IOException e) {
                    LOG.error("Unable to load object specification", e);
                }

                // load custom resources
                // get folder path
                String modelsFolderEnvVar = System.getenv("MODELS_FOLDER");
                String modelsFolderPath = modelsFolderEnvVar != null ? modelsFolderEnvVar : "./models";

                // check if the folder is usable
                File modelsFolder = new File(modelsFolderPath);
                if (!modelsFolder.isDirectory() || !modelsFolder.canRead()) {
                    // log only if env var is configured
                    if (modelsFolderEnvVar != null)
                        LOG.error(MessageFormat.format(
                                "Models folder {0} is not a directory or you have not the right to list its content.",
                                modelsFolderPath));
                    return;
                }

                // get all files
                for (File file : modelsFolder.listFiles()) {
                    if (!file.canRead())
                        continue;

                    if (file.getName().endsWith(".xml")) {
                        // load DDF file
                        DDFFileParser ddfFileParser = new DDFFileParser();
                        ObjectSpec objectSpec = ddfFileParser.parse(file);
                        if (objectSpec != null) {
                            if (OBJECTS.containsKey(objectSpec.id))
                                LOG.warn(MessageFormat.format("There are multiple definitions for the object {0}",
                                        objectSpec.id));
                            OBJECTS.put(objectSpec.id, objectSpec);
                        }
                    } else if (file.getName().endsWith(".json")) {
                        // load object spec json file
                        try (Reader reader = new InputStreamReader(new FileInputStream(file))) {
                            ObjectSpec[] objectSpecs = gson.fromJson(reader, ObjectSpec[].class);
                            for (ObjectSpec objectSpec : objectSpecs) {
                                if (OBJECTS.containsKey(objectSpec.id))
                                    LOG.warn(MessageFormat.format("There are multiple definitions for the object {0}",
                                            objectSpec.id));
                                OBJECTS.put(objectSpec.id, objectSpec);
                            }
                        } catch (IOException e) {
                            LOG.warn(
                                    MessageFormat.format("Unable to load object specification for {0}",
                                            file.getAbsolutePath()), e);
                        }
                    }
                }
            }
        }
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
}

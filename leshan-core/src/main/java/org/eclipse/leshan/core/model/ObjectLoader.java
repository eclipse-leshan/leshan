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
package org.eclipse.leshan.core.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectLoader {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectLoader.class);

    static final String[] ddfpaths = new String[] { "0-1_0.xml", "0-1_1.xml", "1-1_0.xml", "1-1_1.xml", "2-1_0.xml",
            "3-1_0.xml", "3-1_1.xml", "4-1_0.xml", "4-1_1.xml", "4-1_2.xml", "5-1_0.xml", "6.xml", "7.xml",
            "21-1_0.xml", };

    /**
     * Load last embedded version of default LWM2M objects. So the list contain only one model by object.
     */
    public static List<ObjectModel> loadDefault() {
        return loadDefault(LwM2mVersion.V1_1);
    }

    /**
     * Load embedded version of default LWM2M objects for a given version of LWM2M. So the list contain only one model
     * by object.
     */
    public static List<ObjectModel> loadDefault(LwM2mVersion requiredVersion) {
        String errorMsg = Version.validate(requiredVersion.toString());
        if (errorMsg != null)
            throw new IllegalStateException(String.format("Invalid version : %s", errorMsg));

        // standard objects
        LOG.debug("Loading OMA standard object models for LWM2M {}", requiredVersion);
        try {
            Map<Integer, ObjectModel> models = new TreeMap<>();
            for (ObjectModel model : loadDdfResources("/models/", ddfpaths)) {
                // skip model not compatible with the given version
                if (LwM2mVersion.get(model.lwm2mVersion).newerThan(requiredVersion))
                    continue;

                ObjectModel previousModel = models.get(model.id);
                if (previousModel == null || new Version(model.version).newerThan(previousModel.version)) {
                    models.put(model.id, model);
                }
            }
            return new ArrayList<>(models.values());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load models", e);
        }
    }

    /**
     * Load all embedded version of default LWM2M objects. So the list can contains several version of the same object
     * model.
     */
    public static List<ObjectModel> loadAllDefault() {
        List<ObjectModel> models = new ArrayList<>();

        // standard objects
        LOG.debug("Loading OMA standard object models");
        try {
            models.addAll(loadDdfResources("/models/", ddfpaths));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to load models", e);
        }
        return models;
    }

    /**
     * Load object definition from DDF file.
     * <p>
     * Models are not validate if you want to ensure that model are valid use
     * {@link #loadDdfFile(InputStream, String, boolean)} or
     * {@link #loadDdfFile(InputStream, String, DDFFileParser, ObjectModelValidator)}
     * 
     * @param input An inputStream to a DDF file.
     * @param streamName A name for the stream used for logging only
     * 
     * @throws InvalidDDFFileException if DDF file is invalid
     */
    public static List<ObjectModel> loadDdfFile(InputStream input, String streamName)
            throws InvalidDDFFileException, IOException {
        DDFFileParser ddfFileParser = new DDFFileParser();
        return ddfFileParser.parse(input, streamName);
    }

    /**
     * Load object definition from DDF file.
     * 
     * @param input An inputStream to a DDF file.
     * @param streamName A name for the stream used for logging only
     * @param validate true if you want model validation. Validation is not free and it could make sense to not validate
     *        model if you already trust it.
     * 
     * @throws InvalidDDFFileException if DDF file is invalid
     * @throws InvalidModelException if model is invalid
     * 
     * @since 1.1
     */
    public static List<ObjectModel> loadDdfFile(InputStream input, String streamName, boolean validate)
            throws InvalidModelException, InvalidDDFFileException, IOException {
        return loadDdfFile(input, streamName, new DDFFileParser(validate ? new DefaultDDFFileValidatorFactory() : null),
                validate ? new DefaultObjectModelValidator() : null);
    }

    /**
     * Load object definition from DDF file.
     * 
     * @param input An inputStream to a DDF file.
     * @param streamName A name for the stream used for logging and validation
     * @param ddfFileParser a ddfFileParser which could do validation optionally.
     * @param modelValidator an Object model validator to ensure model is valid, see
     *        {@link DefaultObjectModelValidator}. If {@code null} then there will be no validation.
     * 
     * @throws InvalidDDFFileException if DDF file is invalid
     * @throws InvalidModelException if model is invalid
     */
    public static List<ObjectModel> loadDdfFile(InputStream input, String streamName, DDFFileParser ddfFileParser,
            ObjectModelValidator modelValidator) throws InvalidModelException, InvalidDDFFileException, IOException {
        List<ObjectModel> models = ddfFileParser.parse(input, streamName);
        if (modelValidator != null) {
            modelValidator.validate(models, streamName);
        }
        return models;
    }

    /**
     * Load object definition from DDF resources following rules of {@link Class#getResourceAsStream(String)}.
     * <p>
     * It should be used to load DDF embedded with your application bundle (e.g. jar, war, ...)
     * <p>
     * Models are not validate if you want to ensure that model are valid use
     * {@link #loadDdfResources(String, String[], boolean)}
     * {@link #loadDdfResources(String, String[], DDFFileParser, ObjectModelValidator)}
     * 
     * @param path directory path to the DDF files
     * @param filenames names of all the DDF files
     * 
     * @throws InvalidDDFFileException if DDF file is invalid
     * @throws InvalidModelException if model is invalid
     */
    public static List<ObjectModel> loadDdfResources(String path, String[] filenames)
            throws IOException, InvalidModelException, InvalidDDFFileException {
        return loadDdfResources(path, filenames, false);
    }

    /**
     * Load object definition from DDF resources following rules of {@link Class#getResourceAsStream(String)}.
     * <p>
     * It should be used to load DDF embedded with your application bundle (e.g. jar, war, ...)
     * 
     * @param path directory path to the DDF files
     * @param filenames names of all the DDF files
     * @param validate true if you want model validation. Validation is not free and it could make sense to not validate
     *        model if you already trust it.
     * 
     * @throws InvalidDDFFileException if DDF file is invalid
     * @throws InvalidModelException if model is invalid
     * 
     * @since 1.1
     */
    public static List<ObjectModel> loadDdfResources(String path, String[] filenames, boolean validate)
            throws IOException, InvalidModelException, InvalidDDFFileException {
        return loadDdfResources(path, filenames,
                new DDFFileParser(validate ? new DefaultDDFFileValidatorFactory() : null),
                validate ? new DefaultObjectModelValidator() : null);
    }

    /**
     * Load object definition from DDF resources following rules of {@link Class#getResourceAsStream(String)}.
     * <p>
     * It should be used to load DDF embedded with your application bundle (e.g. jar, war, ...)
     * <p>
     * 
     * @param path directory path to the DDF files
     * @param filenames names of all the DDF files
     * @param ddfFileParser a ddfFileParser which could do validation optionally.
     * @param modelValidator an Object model validator to ensure model is valid, see
     *        {@link DefaultObjectModelValidator}. If {@code null} then there will be no validation.
     * 
     * @throws InvalidDDFFileException if DDF file is invalid
     * @throws InvalidModelException if model is invalid
     */
    public static List<ObjectModel> loadDdfResources(String path, String[] filenames, DDFFileParser ddfFileParser,
            ObjectModelValidator modelValidator) throws IOException, InvalidModelException, InvalidDDFFileException {
        List<ObjectModel> models = new ArrayList<>();
        for (String filename : filenames) {
            String fullpath = StringUtils.removeEnd(path, "/") + "/" + StringUtils.removeStart(filename, "/");
            InputStream input = ObjectLoader.class.getResourceAsStream(fullpath);
            if (input != null) {
                models.addAll(loadDdfFile(input, fullpath, ddfFileParser, modelValidator));
            } else {
                throw new FileNotFoundException(String.format("%s not found", fullpath));
            }

        }
        return models;
    }

    /**
     * Load object definition from DDF resources following rules of {@link Class#getResourceAsStream(String)}.
     * <p>
     * It should be used to load DDF embedded with your application bundle (e.g. jar, war, ...)
     * <p>
     * Models are not validate if you want to ensure that model are valid use
     * {@link #loadDdfResources(String[], boolean)} or
     * {@link #loadDdfResources(String[], DDFFileParser, ObjectModelValidator)}
     * 
     * @param paths An array of paths to DDF files.
     * 
     * @throws InvalidDDFFileException if DDF file is invalid
     * @throws InvalidModelException if model is invalid
     */
    public static List<ObjectModel> loadDdfResources(String[] paths)
            throws InvalidDDFFileException, IOException, InvalidModelException {
        return loadDdfResources(paths, false);
    }

    /**
     * Load object definition from DDF resources following rules of {@link Class#getResourceAsStream(String)}.
     * <p>
     * It should be used to load DDF embedded with your application bundle (e.g. jar, war, ...)
     * <p>
     * 
     * @param paths An array of paths to DDF files.
     * @param validate true if you want model validation. Validation is not free and it could make sense to not validate
     *        model if you already trust it.
     * 
     * @throws InvalidDDFFileException if DDF file is invalid
     */
    public static List<ObjectModel> loadDdfResources(String[] paths, boolean validate)
            throws IOException, InvalidModelException, InvalidDDFFileException {
        return loadDdfResources(paths, new DDFFileParser(validate ? new DefaultDDFFileValidatorFactory() : null),
                validate ? new DefaultObjectModelValidator() : null);
    }

    /**
     * Load object definition from DDF resources following rules of {@link Class#getResourceAsStream(String)}.
     * <p>
     * It should be used to load DDF embedded with your application bundle (e.g. jar, war, ...)
     * <p>
     * 
     * @param paths An array of paths to DDF files.
     * @param ddfFileParser a ddfFileParser which could do validation optionally.
     * @param modelValidator an Object model validator to ensure model is valid, see
     *        {@link DefaultObjectModelValidator}. If {@code null} then there will be no validation.
     * 
     * @throws InvalidDDFFileException if DDF file is invalid
     */
    public static List<ObjectModel> loadDdfResources(String[] paths, DDFFileParser ddfFileParser,
            ObjectModelValidator modelValidator) throws IOException, InvalidModelException, InvalidDDFFileException {
        List<ObjectModel> models = new ArrayList<>();
        for (String path : paths) {
            InputStream input = ObjectLoader.class.getResourceAsStream(path);
            if (input != null) {
                models.addAll(loadDdfFile(input, path, ddfFileParser, modelValidator));
            } else {
                throw new FileNotFoundException(String.format("%s not found", path));
            }
        }
        return models;
    }

    /**
     * Load object definitions from directory.
     * <p>
     * Models are not validate if you want to ensure that model are valid use {@link #loadObjectsFromDir(File, boolean)}
     * or {@link #loadObjectsFromDir(File, DDFFileParser, ObjectModelValidator)}
     * 
     * @param modelsDir the directory containing all the ddf file definition.
     */
    public static List<ObjectModel> loadObjectsFromDir(File modelsDir) {
        return loadObjectsFromDir(modelsDir, false);
    }

    /**
     * Load object definitions from directory.
     * <p>
     * Invalid model will be logged and ignored.
     * 
     * @param modelsDir the directory containing all the ddf file definition.
     * @param validate true if you want model validation. Validation is not free and it could make sense to not validate
     *        model if you already trust it.
     */
    public static List<ObjectModel> loadObjectsFromDir(File modelsDir, boolean validate) {
        return loadObjectsFromDir(modelsDir, new DDFFileParser(validate ? new DefaultDDFFileValidatorFactory() : null),
                validate ? new DefaultObjectModelValidator() : null);
    }

    /**
     * Load object definitions from directory.
     * <p>
     * Invalid model will be logged and ignored.
     * 
     * @param modelsDir the directory containing all the ddf file definition.
     * @param ddfFileParser a ddfFileParser which could do validation optionally.
     * @param modelValidator an Object model validator to ensure model is valid, see
     *        {@link DefaultObjectModelValidator}. If {@code null} then there will be no validation.
     * 
     */
    public static List<ObjectModel> loadObjectsFromDir(File modelsDir, DDFFileParser ddfFileParser,
            ObjectModelValidator modelValidator) {
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
                        models.addAll(loadDdfFile(input, file.getName(), ddfFileParser, modelValidator));
                    } catch (IOException | InvalidModelException | InvalidDDFFileException e) {
                        LOG.warn(MessageFormat.format("Unable to load object models for {0}", file.getAbsolutePath()),
                                e);
                    }
                }
            }
        }
        return models;
    }
}

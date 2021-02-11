/*******************************************************************************
 * Copyright (c) 2013-2017 Sierra Wireless and others.
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
 *     Ville Skyttä - initial implementation
 *******************************************************************************/
package org.eclipse.leshan.server.demo.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.eclipse.leshan.core.model.DDFFileParser;
import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.util.json.JsonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DdfDownloader {

    static {
        // Define a default logback.configurationFile
        String property = System.getProperty("logback.configurationFile");
        if (property == null) {
            System.setProperty("logback.configurationFile", "logback-config.xml");
        }
    }

    private static class DdfRef {
        public Integer objectId;
        public String url;

        public DdfRef(Integer objectId, String url) {
            this.objectId = objectId;
            this.url = url;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(DdfDownloader.class);

    public static final String DOWNLOAD_FOLDER_PATH = "ddffiles";
    public static final String CORE_DOWNLOAD_FOLDER_PATH = "core";
    public static final List<Integer> CORE_IDS = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 21);
    public static final String DEMO_DOWNLOAD_FOLDER_PATH = "demo";
    private static final String LWM2M_REGISTRY_FOLDER_URL = "https://raw.githubusercontent.com/OpenMobileAlliance/lwm2m-registry/prod/";
    private static final String LWM2M_REGISTRY_FILENAME = "DDF.xml";

    private final DocumentBuilderFactory factory;

    public DdfDownloader() {
        factory = DocumentBuilderFactory.newInstance();
    }

    private URLConnection openConnection(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        // The default Java User-Agent gets 403 Forbidden from OMA website
        conn.setRequestProperty("User-Agent", "Leshan " + getClass().getSimpleName());
        return conn;
    }

    private void download(String registryFolderUrl, String registryFileName, String downloadFolderPath)
            throws IOException, InvalidDDFFileException {
        String registryUrl = registryFolderUrl + registryFileName;

        LOG.info("Processing LWM2M registry at {} ...", registryUrl);

        List<DdfRef> ddfUrls = new ArrayList<>();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document;
            // Downloading using URLConnection for ability to set User-Agent
            try (InputStream is = openConnection(new URL(registryUrl)).getInputStream()) {
                document = builder.parse(is, registryUrl);
            }

            NodeList items = document.getDocumentElement().getElementsByTagName("Item");
            for (int i = 0; i < items.getLength(); i++) {
                Node item = items.item(i);
                Node id = ((Element) item).getElementsByTagName("ObjectID").item(0);
                Integer objectId;
                if (id == null) {
                    LOG.warn("Item without ObjectID : {}" + item.getTextContent());
                    continue;
                }
                try {
                    objectId = Integer.parseInt(id.getTextContent());
                } catch (NumberFormatException e) {
                    LOG.warn("Item with Invalid ObjectID : {}" + item.getTextContent());
                    continue;
                }

                Node urn = ((Element) item).getElementsByTagName("URN").item(0);
                if (urn == null) {
                    LOG.warn("Unable to handle Object {} : Item has not URN field", id.getTextContent());
                    continue;
                }

                Node ddf = ((Element) item).getElementsByTagName("DDF").item(0);
                if (ddf == null || ddf.getTextContent() == null || ddf.getTextContent().isEmpty()) {
                    LOG.warn("Unable to handle {}: no DDF field", urn.getTextContent());
                    continue;
                }

                ddfUrls.add(new DdfRef(objectId, registryFolderUrl + ddf.getTextContent()));
            }
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(e);
        }

        LOG.info("Downloading DDF files in [{}] folder ...", downloadFolderPath);
        if (!Files.isDirectory(Paths.get(downloadFolderPath))) {
            String absoluteDownloadPath = Paths.get(downloadFolderPath).normalize().toAbsolutePath().toString();
            LOG.warn("Files will be downloaded in [{}] but this is not exist or is not a directory : \n=>  {}",
                    downloadFolderPath, absoluteDownloadPath);
            System.exit(-1);
        }
        Path coreOutPath = Paths.get(downloadFolderPath, CORE_DOWNLOAD_FOLDER_PATH);
        Files.createDirectories(coreOutPath);
        Path demoOutPath = Paths.get(downloadFolderPath, DEMO_DOWNLOAD_FOLDER_PATH);
        Files.createDirectories(demoOutPath);

        DDFFileParser ddfFileParser = new DDFFileParser();
        int nbDownloaded = 0;
        for (DdfRef ddfRef : ddfUrls) {

            URL parsedUrl;
            try {
                parsedUrl = new URL(ddfRef.url);
            } catch (MalformedURLException e) {
                LOG.error("Skipping malformed URL {}", ddfRef.url);
                continue;
            }

            String filename = parsedUrl.getPath();
            filename = filename.substring(filename.lastIndexOf("/"));
            Path outPath;
            if (CORE_IDS.contains(ddfRef.objectId)) {
                outPath = Paths.get(downloadFolderPath, CORE_DOWNLOAD_FOLDER_PATH, filename);
            } else {
                outPath = Paths.get(downloadFolderPath, DEMO_DOWNLOAD_FOLDER_PATH, filename);
            }

            LOG.debug("Downloading DDF file {} to {} (from {})", filename, outPath, parsedUrl);
            // store in memory
            byte[] ddfBytes;
            try (InputStream in = openConnection(parsedUrl).getInputStream()) {
                ddfBytes = IOUtils.toByteArray(in);
            }
            // parse to skip not lwm2m 1.1 or 1.0 models
            boolean skip = false;
            String version = "";
            try (InputStream in = new ByteArrayInputStream(ddfBytes)) {
                List<ObjectModel> models = ddfFileParser.parse(in, filename);
                for (ObjectModel model : models) {
                    if (!model.lwm2mVersion.equals("1.0") && !model.lwm2mVersion.equals("1.1")) {
                        skip = true;
                        version = model.lwm2mVersion;
                        break;
                    }
                }
            }
            // copy file
            if (!skip) {
                try (InputStream in = new ByteArrayInputStream(ddfBytes)) {
                    Files.copy(in, outPath, StandardCopyOption.REPLACE_EXISTING);
                    nbDownloaded++;
                }
            } else {
                LOG.info("Skip models with version {} > 1.1 : {}", version, ddfRef.url);
            }
        }
        LOG.info("Downloaded {} models in {}", nbDownloaded, downloadFolderPath);
    }

    public static void main(String[] args) throws IOException, JsonException, InvalidDDFFileException {
        // default values
        String downloadFolderPath = DOWNLOAD_FOLDER_PATH;
        String registryFolderUrl = LWM2M_REGISTRY_FOLDER_URL;
        String registryFileName = LWM2M_REGISTRY_FILENAME;

        // use arguments if they exist
        if (args.length >= 1)
            downloadFolderPath = args[0]; // the path to folder where files will be downloaded
        if (args.length >= 2)
            registryFolderUrl = args[1]; // folder URL of LWM2M registry
        if (args.length >= 3)
            registryFileName = args[2]; // filename of LWM2M registry

        new DdfDownloader().download(registryFolderUrl, registryFileName, downloadFolderPath);
    }
}

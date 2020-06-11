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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.leshan.core.model.InvalidDDFFileException;
import org.eclipse.leshan.core.util.json.JsonException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DdfList2JsonGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(DdfList2JsonGenerator.class);

    private final DocumentBuilderFactory factory;

    public DdfList2JsonGenerator() {
        factory = DocumentBuilderFactory.newInstance();
    }

    private URLConnection openConnection(URL url) throws IOException {
        URLConnection conn = url.openConnection();
        // The default Java User-Agent gets 403 Forbidden from OMA website
        conn.setRequestProperty("User-Agent", "Leshan " + getClass().getSimpleName());
        return conn;
    }

    private void processDdfList(String url, String ddfFilesPath) throws IOException {

        LOG.debug("Processing DDF list file {}", url);

        List<String> ddfUrls = new ArrayList<>();

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document;
            // Downloading using URLConnection for ability to set User-Agent
            try (InputStream is = openConnection(new URL(url)).getInputStream()) {
                document = builder.parse(is, url);
            }

            NodeList items = document.getDocumentElement().getElementsByTagName("Item");
            for (int i = 0; i < items.getLength(); i++) {
                Node item = items.item(i);
                Node ddf = ((Element) item).getElementsByTagName("DDF").item(0);
                ddfUrls.add(ddf.getTextContent());
            }
        } catch (SAXException | ParserConfigurationException e) {
            throw new IOException(e);
        }

        for (String ddfUrl : ddfUrls) {

            URL parsedUrl;
            try {
                parsedUrl = new URL(ddfUrl);
            } catch (MalformedURLException e) {
                LOG.error("Skipping malformed URL {}", ddfUrl);
                continue;
            }

            String filename = parsedUrl.getPath();
            filename = filename.substring(filename.lastIndexOf("/"));

            Path outPath = Paths.get(ddfFilesPath, filename);

            LOG.debug("Downloading DDF file {} to {}", ddfUrl, outPath);

            try (InputStream in = openConnection(parsedUrl).getInputStream()) {
                Files.copy(in, outPath);
            }
        }
    }

    public static void main(String[] args) throws IOException, JsonException, InvalidDDFFileException {
        // default values
        String ddfFilesPath = Ddf2JsonGenerator.DEFAULT_DDF_FILES_PATH;
        String outputPath = Ddf2JsonGenerator.DEFAULT_OUTPUT_PATH;
        String ddfListUrl = "http://www.openmobilealliance.org/wp/OMNA/LwM2M/DDF.xml";

        // use arguments if they exist
        if (args.length >= 1)
            ddfFilesPath = args[0]; // the path to a DDF file or a folder which contains DDF files.
        if (args.length >= 2)
            outputPath = args[1]; // the path of the output file.
        if (args.length >= 3)
            ddfListUrl = args[2]; // the path of the DDF list file.

        new DdfList2JsonGenerator().processDdfList(ddfListUrl, ddfFilesPath);

        LOG.error("DDF list {} processed to {}, proceeding with JSON generation in {}", ddfListUrl, ddfFilesPath,
                outputPath);

        // generate object spec file
        Ddf2JsonGenerator ddfJsonGenerator = new Ddf2JsonGenerator();
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath)) {
            ddfJsonGenerator.generate(new File(ddfFilesPath), fileOutputStream);
        }
    }
}

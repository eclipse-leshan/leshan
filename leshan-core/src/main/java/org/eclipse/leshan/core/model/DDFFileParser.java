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
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.leshan.core.model.ResourceModel.Operations;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * A parser for Object DDF files.
 */
public class DDFFileParser {

    private static final Logger LOG = LoggerFactory.getLogger(DDFFileParser.class);

    private final DocumentBuilderFactory factory;

    public DDFFileParser() {
        factory = DocumentBuilderFactory.newInstance();
    }

    public ObjectModel parse(File ddfFile) {
        try (InputStream input = new FileInputStream(ddfFile)) {
            return parse(input, ddfFile.getName());
        } catch (IOException e) {
            LOG.error("Could not parse the resource definition file " + ddfFile.getName(), e);
        }
        return null;
    }

    public ObjectModel parse(InputStream inputStream, String streamName) {
        streamName = streamName == null ? "" : streamName;

        LOG.debug("Parsing DDF file {}", streamName);

        ObjectModel result = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            Node object = document.getDocumentElement().getElementsByTagName("Object").item(0);
            result = this.parseObject(object);

        } catch (SAXException | IOException | ParserConfigurationException e) {
            LOG.error("Could not parse the resource definition file " + streamName, e);
        }

        return result;
    }

    private ObjectModel parseObject(Node object) {

        Integer id = null;
        String name = null;
        String description = null;
        boolean multiple = false;
        boolean mandatory = false;
        List<ResourceModel> resources = new ArrayList<>();

        for (int i = 0; i < object.getChildNodes().getLength(); i++) {
            Node field = object.getChildNodes().item(i);
            switch (field.getNodeName()) {
            case "ObjectID":
                id = Integer.valueOf(field.getTextContent());
                break;
            case "Name":
                name = field.getTextContent();
                break;
            case "Description1":
                description = field.getTextContent();
                break;
            case "MultipleInstances":
                multiple = "Multiple".equals(field.getTextContent());
                break;
            case "Mandatory":
                mandatory = "Mandatory".equals(field.getTextContent());
                break;
            case "Resources":
                for (int j = 0; j < field.getChildNodes().getLength(); j++) {
                    Node item = field.getChildNodes().item(j);
                    if (item.getNodeName().equals("Item")) {
                        resources.add(this.parseResource(item));
                    }
                }
                break;
            }
        }

        return new ObjectModel(id, name, description, multiple, mandatory, resources);

    }

    private ResourceModel parseResource(Node item) {

        Integer id = Integer.valueOf(item.getAttributes().getNamedItem("ID").getTextContent());
        String name = null;
        Operations operations = Operations.NONE;
        boolean multiple = false;
        boolean mandatory = false;
        Type type = Type.STRING;
        String rangeEnumeration = null;
        String units = null;
        String description = null;

        for (int i = 0; i < item.getChildNodes().getLength(); i++) {
            Node field = item.getChildNodes().item(i);
            switch (field.getNodeName()) {
            case "Name":
                name = field.getTextContent();
                break;
            case "Operations":
                String strOp = field.getTextContent();
                if (strOp != null && !strOp.isEmpty()) {
                    operations = Operations.valueOf(strOp);
                }
                break;
            case "MultipleInstances":
                multiple = "Multiple".equals(field.getTextContent());
                break;
            case "Mandatory":
                mandatory = "Mandatory".equals(field.getTextContent());
                break;
            case "Type":
                switch (field.getTextContent()) {
                case "String":
                    type = Type.STRING;
                    break;
                case "Integer":
                    type = Type.INTEGER;
                    break;
                case "Float":
                    type = Type.FLOAT;
                    break;
                case "Boolean":
                    type = Type.BOOLEAN;
                    break;
                case "Opaque":
                    type = Type.OPAQUE;
                    break;
                case "Time":
                    type = Type.TIME;
                    break;
                case "Objlnk":
                    type = Type.OBJLNK;
                    break;
                }
                break;
            case "RangeEnumeration":
                rangeEnumeration = field.getTextContent();
                break;
            case "Units":
                units = field.getTextContent();
                break;
            case "Description":
                description = field.getTextContent();
                break;
            }

        }

        return new ResourceModel(id, name, operations, multiple, mandatory, type, rangeEnumeration, units, description);
    }

}

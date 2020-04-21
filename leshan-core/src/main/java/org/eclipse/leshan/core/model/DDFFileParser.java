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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.leshan.core.model.ResourceModel.Operations;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A parser for Object DDF files.
 */
public class DDFFileParser {

    private static final Logger LOG = LoggerFactory.getLogger(DDFFileParser.class);

    private final DocumentBuilderFactory factory;

    public DDFFileParser() {
        factory = DocumentBuilderFactory.newInstance();
    }

    public List<ObjectModel> parse(File ddfFile) {
        try (InputStream input = new FileInputStream(ddfFile)) {
            return parse(input, ddfFile.getName());
        } catch (Exception e) {
            LOG.error("Could not parse the resource definition file " + ddfFile.getName(), e);
        }
        return Collections.emptyList();
    }

    public List<ObjectModel> parse(InputStream inputStream, String streamName) {
        streamName = streamName == null ? "" : streamName;

        LOG.debug("Parsing DDF file {}", streamName);

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            ArrayList<ObjectModel> objects = new ArrayList<>();
            NodeList nodeList = document.getDocumentElement().getElementsByTagName("Object");
            for (int i = 0; i < nodeList.getLength(); i++) {
                objects.add(parseObject(nodeList.item(i), streamName));
            }
            return objects;
        } catch (Exception e) {
            LOG.error("Could not parse the resource definition file " + streamName, e);
        }
        return Collections.emptyList();
    }

    private ObjectModel parseObject(Node object, String streamName) {

        // TODO All the validation here should be replaced by a real Validator

        Node objectType = object.getAttributes().getNamedItem("ObjectType");
        if (objectType == null || !"MODefinition".equals(objectType.getTextContent())) {
            LOG.warn("Object element in {} MUST have a ObjectType attribute equals to 'MODefinition'", streamName);
        }

        Integer id = null;
        String name = null;
        String description = null;
        String version = ObjectModel.DEFAULT_VERSION;
        boolean multiple = false;
        boolean mandatory = false;
        List<ResourceModel> resources = new ArrayList<>();

        for (int i = 0; i < object.getChildNodes().getLength(); i++) {
            Node field = object.getChildNodes().item(i);
            if (field.getNodeType() != Node.ELEMENT_NODE)
                continue;

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
            case "ObjectVersion":
                if (!StringUtils.isEmpty(field.getTextContent()))
                    version = field.getTextContent();
                break;
            case "MultipleInstances":
                if ("Multiple".equals(field.getTextContent())) {
                    multiple = true;
                } else if ("Single".equals(field.getTextContent())) {
                    multiple = false;
                } else {
                    LOG.warn(
                            "Invalid Value for [MultipleInstances] field in {}. It MUST be 'Multiple' or 'Single' but it was {} : We will consider it as a 'Single' value.",
                            field.getTextContent(), streamName);
                    multiple = false;
                }
                break;
            case "Mandatory":
                if ("Mandatory".equals(field.getTextContent())) {
                    mandatory = true;
                } else if ("Optional".equals(field.getTextContent())) {
                    mandatory = false;
                } else {
                    LOG.warn(
                            "Invalid Value for [Mandatory] field in {}. It MUST be 'Mandatory' or 'Optional' but it was {} :s We will consider it as a 'Optional' value.",
                            field.getTextContent(), streamName);
                    mandatory = false;
                }
                break;
            case "Resources":
                for (int j = 0; j < field.getChildNodes().getLength(); j++) {
                    Node item = field.getChildNodes().item(j);
                    if (item.getNodeType() != Node.ELEMENT_NODE)
                        continue;

                    if (item.getNodeName().equals("Item")) {
                        resources.add(this.parseResource(item, streamName));
                    } else {
                        LOG.warn("Unexpected resources element [{}] in {} : it will be ignored.", item.getNodeName(),
                                streamName);
                    }
                }
                break;
            case "ObjectURN":
            case "LWM2MVersion":
            case "Description2":
                // TODO it should be supported in Leshan v1.1 or later.
                break;
            default:
                LOG.warn("Unexpected object element [{}] in {} : it will be ignored.", field.getNodeName(), streamName);
                break;
            }
        }

        return new ObjectModel(id, name, description, version, multiple, mandatory, resources);

    }

    private ResourceModel parseResource(Node item, String streamName) {

        Integer id = Integer.valueOf(item.getAttributes().getNamedItem("ID").getTextContent());
        String name = null;
        Operations operations = Operations.NONE;
        boolean multiple = false;
        boolean mandatory = false;
        Type type = null;
        String rangeEnumeration = null;
        String units = null;
        String description = null;

        for (int i = 0; i < item.getChildNodes().getLength(); i++) {
            Node field = item.getChildNodes().item(i);
            if (field.getNodeType() != Node.ELEMENT_NODE)
                continue;

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
                if ("Multiple".equals(field.getTextContent())) {
                    multiple = true;
                } else if ("Single".equals(field.getTextContent())) {
                    multiple = false;
                } else {
                    LOG.warn(
                            "Invalid Value for [MultipleInstances] field in {}. It MUST be 'Multiple' or 'Single' but it was {} : We will consider it as a 'Single' value.",
                            field.getTextContent(), streamName);
                    multiple = false;
                }
                break;
            case "Mandatory":
                if ("Mandatory".equals(field.getTextContent())) {
                    mandatory = true;
                } else if ("Optional".equals(field.getTextContent())) {
                    mandatory = false;
                } else {
                    LOG.warn(
                            "Invalid Value for [Mandatory] field in {}. It MUST be 'Mandatory' or 'Optional' but it was {} :s We will consider it as a 'Optional' value.",
                            field.getTextContent(), streamName);
                    mandatory = false;
                }
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
                case "":
                    type = null;
                    break;
                default:
                    LOG.warn("Unexpected type value [{}] in {} : no type assigned", field.getTextContent(), streamName);
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
            default:
                LOG.warn("Unexpected resource element [{} {}] in {} : it will be ignored.", field.getNodeName(),
                        field.getNodeType(), streamName);
                break;
            }
        }

        if (operations.isExecutable() && type != null) {
            LOG.warn("Model for Resource {}({}) in {} is invalid : an executable resource MUST NOT have a type({})",
                    name, id, streamName, type);
        } else if (!operations.isExecutable() && type == null) {
            LOG.warn(
                    "Model for Resource {}({}) in {} is invalid : a none executable resource MUST have a type. We will consider it as String.",
                    name, id, streamName, type);
            type = Type.STRING;
        }

        return new ResourceModel(id, name, operations, multiple, mandatory, type, rangeEnumeration, units, description);
    }
}

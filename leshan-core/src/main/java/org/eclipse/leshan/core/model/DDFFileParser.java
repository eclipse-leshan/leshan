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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.model.ResourceModel.Operations;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A parser for Object DDF files.
 */
public class DDFFileParser {

    private static final Logger LOG = LoggerFactory.getLogger(DDFFileParser.class);

    private final DocumentBuilderFactory factory;
    private final DDFFileValidatorFactory ddfValidatorFactory;
    private final DDFFileValidator ddfValidator;

    public DDFFileParser() {
        this(null, null);
    }

    /**
     * Build a DDFFileParser with a given {@link DDFFileValidator}.
     * 
     * @param ddfValidator a {@link DDFFileValidator} or {@code null} if no validation required.
     * @since 1.1
     */
    public DDFFileParser(DDFFileValidator ddfValidator) {
        this(ddfValidator, null);
    }

    /**
     * Build a DDFFileParser with a given {@link DDFFileValidatorFactory}.
     * 
     * @param ddfFileValidatorFactory a {@link DDFFileValidatorFactory} or {@code null} if no validation required.
     */
    public DDFFileParser(DDFFileValidatorFactory ddfFileValidatorFactory) {
        this(null, ddfFileValidatorFactory);
    }

    private DDFFileParser(DDFFileValidator ddfValidator, DDFFileValidatorFactory ddfFileValidatorFactory) {
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        this.ddfValidator = ddfValidator;
        this.ddfValidatorFactory = ddfFileValidatorFactory;
    }

    /**
     * Parse a DDF file.
     * 
     * @throws InvalidDDFFileException if DDF file is not a valid.
     * @throws IOException see {@link FileInputStream#FileInputStream(File)} or
     *         {@link DocumentBuilder#parse(InputStream)}
     */
    public List<ObjectModel> parse(File ddfFile) throws InvalidDDFFileException, IOException {
        try (InputStream input = new FileInputStream(ddfFile)) {
            return parse(input, ddfFile.getName());
        }
    }

    /**
     * Parse a DDF file from an inputstream.
     * 
     * @throws InvalidDDFFileException if DDF file is not a valid.
     * @throws IOException see {@link FileInputStream#FileInputStream(File)} or
     *         {@link DocumentBuilder#parse(InputStream)}
     */
    public List<ObjectModel> parse(InputStream inputStream, String streamName)
            throws InvalidDDFFileException, IOException {
        streamName = streamName == null ? "" : streamName;

        LOG.debug("Parsing DDF file {}", streamName);

        try {
            // Parse XML file
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            // Get DDF file validator
            LwM2mVersion lwm2mVersion;
            DDFFileValidator ddfFileValidator;
            if (ddfValidatorFactory != null) {
                lwm2mVersion = ddfValidatorFactory.extractLWM2MVersion(document, streamName);
                ddfFileValidator = ddfValidatorFactory.create(lwm2mVersion);
            } else {
                lwm2mVersion = null;
                ddfFileValidator = ddfValidator;
            }

            // Validate XML against Schema
            boolean validateDdf = ddfFileValidator != null;
            if (validateDdf) {
                ddfFileValidator.validate(document);
            }

            // Build list of ObjectModel
            ArrayList<ObjectModel> objects = new ArrayList<>();
            NodeList nodeList = document.getDocumentElement().getElementsByTagName("Object");
            for (int i = 0; i < nodeList.getLength(); i++) {
                objects.add(parseObject(nodeList.item(i), streamName, lwm2mVersion, validateDdf));
            }
            return objects;
        } catch (InvalidDDFFileException | SAXException e) {
            throw new InvalidDDFFileException(e, "Invalid DDF file %s", streamName);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unable to create Document Builder", e);
        }
    }

    private ObjectModel parseObject(Node object, String streamName, LwM2mVersion schemaVersion, boolean validate)
            throws InvalidDDFFileException {

        Node objectType = object.getAttributes().getNamedItem("ObjectType");
        if (validate && (objectType == null || !"MODefinition".equals(objectType.getTextContent()))) {
            throw new InvalidDDFFileException(
                    "Object element in %s MUST have a ObjectType attribute equals to 'MODefinition'.", streamName);
        }

        Integer id = null;
        String name = null;
        String description = null;
        String version = ObjectModel.DEFAULT_VERSION;
        Boolean multiple = null;
        Boolean mandatory = null;
        Map<Integer, ResourceModel> resources = new HashMap<>();
        String urn = null;
        String description2 = null;
        String lwm2mVersion = ObjectModel.DEFAULT_VERSION;

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
                if (!StringUtils.isEmpty(field.getTextContent())) {
                    version = field.getTextContent();
                }
                break;
            case "MultipleInstances":
                if ("Multiple".equals(field.getTextContent())) {
                    multiple = true;
                } else if ("Single".equals(field.getTextContent())) {
                    multiple = false;
                }
                break;
            case "Mandatory":
                if ("Mandatory".equals(field.getTextContent())) {
                    mandatory = true;
                } else if ("Optional".equals(field.getTextContent())) {
                    mandatory = false;
                }
                break;
            case "Resources":
                for (int j = 0; j < field.getChildNodes().getLength(); j++) {
                    Node item = field.getChildNodes().item(j);
                    if (item.getNodeType() != Node.ELEMENT_NODE)
                        continue;

                    if (item.getNodeName().equals("Item")) {
                        ResourceModel resource = this.parseResource(item, streamName);
                        if (validate && resources.containsKey(resource.id)) {
                            throw new InvalidDDFFileException(
                                    "Object %s in %s contains at least 2 resources with same id %s.",
                                    id != null ? id : "", streamName, resource.id);
                        } else {
                            resources.put(resource.id, resource);
                        }
                    }
                }
                break;
            case "ObjectURN":
                urn = field.getTextContent();
                break;
            case "LWM2MVersion":
                if (!StringUtils.isEmpty(field.getTextContent())) {
                    lwm2mVersion = field.getTextContent();
                    if (schemaVersion != null && !schemaVersion.toString().equals(lwm2mVersion)) {
                        throw new InvalidDDFFileException(
                                "LWM2MVersion is not consistent with xml shema(xsi:noNamespaceSchemaLocation) in %s : %s  expected but was %s.",
                                streamName, schemaVersion, lwm2mVersion);
                    }
                }
                break;
            case "Description2":
                description2 = field.getTextContent();
                break;
            default:
                break;
            }
        }

        return new ObjectModel(id, name, description, version, multiple, mandatory, resources.values(), urn,
                lwm2mVersion, description2);

    }

    private ResourceModel parseResource(Node item, String streamName) throws DOMException, InvalidDDFFileException {

        Integer id = Integer.valueOf(item.getAttributes().getNamedItem("ID").getTextContent());
        String name = null;
        Operations operations = null;
        Boolean multiple = false;
        Boolean mandatory = false;
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
                } else {
                    operations = Operations.NONE;
                }
                break;
            case "MultipleInstances":
                if ("Multiple".equals(field.getTextContent())) {
                    multiple = true;
                } else if ("Single".equals(field.getTextContent())) {
                    multiple = false;
                }
                break;
            case "Mandatory":
                if ("Mandatory".equals(field.getTextContent())) {
                    mandatory = true;
                } else if ("Optional".equals(field.getTextContent())) {
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
                case "Unsigned Integer":
                    type = Type.UNSIGNED_INTEGER;
                    break;
                case "Corelnk":
                    type = Type.CORELINK;
                    break;
                case "":
                    type = Type.NONE;
                    break;
                default:
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
                break;
            }
        }
        return new ResourceModel(id, name, operations, multiple, mandatory, type, rangeEnumeration, units, description);
    }
}

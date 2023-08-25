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
import java.util.Collections;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

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
    private final DDFFileValidator ddfValidator;

    public DDFFileParser() {
        this(null);
    }

    /**
     * Build a DDFFileParser with a given {@link DDFFileValidator}.
     * 
     * @param ddfValidator a {@link DDFFileValidator} or {@code null} if no validation required.
     * @since 1.1
     */
    public DDFFileParser(DDFFileValidator ddfValidator) {
        factory = createDocumentBuilderFactory();
        this.ddfValidator = ddfValidator;
    }

    protected DocumentBuilderFactory createDocumentBuilderFactory() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            // Create Safe DocumentBuilderFactory (not vulnerable to XXE Attacks)
            // -----------------------------------------------------------------
            // There is several recommendation from different source we try to apply all, even if some are maybe
            // redundant.

            // from :
            // https://semgrep.dev/docs/cheat-sheets/java-xxe/
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

            // from :
            // https://cheatsheetseries.owasp.org/cheatsheets/XML_External_Entity_Prevention_Cheat_Sheet.html#jaxp-documentbuilderfactory-saxparserfactory-and-dom4j
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // Disable DTDs
            factory.setXIncludeAware(false); // Disable XML Inclusions

            // from :
            // https://community.veracode.com/s/article/Java-Remediation-Guidance-for-XXE
            factory.setExpandEntityReferences(false); // disable expand entity reference nodes

        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unable to create DocumentBuilderFactory", e);
        }
        factory.setNamespaceAware(true);
        return factory;
    }

    /**
     * @deprecated use {@link #parseEx(File)}
     */
    @Deprecated
    public List<ObjectModel> parse(File ddfFile) {
        try (InputStream input = new FileInputStream(ddfFile)) {
            return parse(input, ddfFile.getName());
        } catch (Exception e) {
            LOG.error("Could not parse the resource definition file " + ddfFile.getName(), e);
        }
        return Collections.emptyList();
    }

    /**
     * @deprecated use {@link #parseEx(InputStream, String)}
     */
    @Deprecated
    public List<ObjectModel> parse(InputStream inputStream, String streamName) {
        streamName = streamName == null ? "" : streamName;

        LOG.debug("Parsing DDF file {}", streamName);

        try {
            // Parse XML file
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            // Validate XML against Schema
            if (ddfValidator != null) {
                ddfValidator.validate(document);
            }

            // Build list of ObjectModel
            ArrayList<ObjectModel> objects = new ArrayList<>();
            NodeList nodeList = document.getDocumentElement().getElementsByTagName("Object");
            for (int i = 0; i < nodeList.getLength(); i++) {
                objects.add(parseObject(nodeList.item(i), streamName, false));
            }
            return objects;
        } catch (Exception e) {
            LOG.error("Could not parse the resource definition file " + streamName, e);
        }
        return Collections.emptyList();
    }

    /**
     * Parse a DDF file.
     * 
     * @throws InvalidDDFFileException if DDF file is not a valid.
     * @throws IOException see {@link FileInputStream#FileInputStream(File)} or
     *         {@link DocumentBuilder#parse(InputStream)}
     * 
     * @since 1.1
     */
    public List<ObjectModel> parseEx(File ddfFile) throws InvalidDDFFileException, IOException {
        try (InputStream input = new FileInputStream(ddfFile)) {
            return parseEx(input, ddfFile.getName());
        }
    }

    /**
     * Parse a DDF file from an inputstream.
     * 
     * @throws InvalidDDFFileException if DDF file is not a valid.
     * @throws IOException see {@link FileInputStream#FileInputStream(File)} or
     *         {@link DocumentBuilder#parse(InputStream)}
     * 
     * @since 1.1
     */
    public List<ObjectModel> parseEx(InputStream inputStream, String streamName)
            throws InvalidDDFFileException, IOException {
        streamName = streamName == null ? "" : streamName;

        LOG.debug("Parsing DDF file {}", streamName);

        // Parse XML file
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);
            // Validate XML against Schema
            if (ddfValidator != null) {
                ddfValidator.validate(document);
            }

            // Build list of ObjectModel
            ArrayList<ObjectModel> objects = new ArrayList<>();
            NodeList nodeList = document.getDocumentElement().getElementsByTagName("Object");
            for (int i = 0; i < nodeList.getLength(); i++) {
                objects.add(parseObject(nodeList.item(i), streamName, ddfValidator != null));
            }
            return objects;
        } catch (InvalidDDFFileException | SAXException e) {
            throw new InvalidDDFFileException(e, "Invalid DDF file %s", streamName);
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("Unable to create Document Builder", e);
        }
    }

    private ObjectModel parseObject(Node object, String streamName, boolean raiseException)
            throws InvalidDDFFileException {

        Node objectType = object.getAttributes().getNamedItem("ObjectType");
        if (objectType == null || !"MODefinition".equals(objectType.getTextContent())) {
            // TODO change for v2.0 : we keep it for backward compatibility
            // test must be done only if validation is needed
            handleError(raiseException,
                    "Object element in %s MUST have a ObjectType attribute equals to 'MODefinition'.", streamName);
        }

        Integer id = null;
        String name = null;
        String description = null;
        String version = ObjectModel.DEFAULT_VERSION;
        boolean multiple = false;
        boolean mandatory = false;
        List<ResourceModel> resources = new ArrayList<>();
        String urn = null;
        String description2 = null;
        String lwm2mVersion = null;

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
                    // TODO remove for v2.0 : we keep it for backward compatibility
                    // test will be done in DDFFileValidator or ObjectModelValidatorif validation if needed
                    handleError(raiseException,
                            "Invalid Value for [MultipleInstances] field in %s. It MUST be 'Multiple' or 'Single' but it was %s.",
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
                    // TODO remove for v2.0 : we keep it for backward compatibility
                    // test will be done in DDFFileValidator or ObjectModelValidator if validation if needed
                    handleError(raiseException,
                            "Invalid Value for [Mandatory] field in %s. It MUST be 'Mandatory' or 'Optional' but it was %s.",
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
                        resources.add(this.parseResource(item, streamName, raiseException));
                    } else {
                        // TODO remove for v2.0 : we keep it for backward compatibility
                        // test will be done in DDFFileValidator if validation if needed
                        handleError(raiseException, "Unexpected resources element [%s] in %s : it will be ignored.",
                                item.getNodeName(), streamName);
                    }
                }
                break;
            case "ObjectURN":
                urn = field.getTextContent();
                break;
            case "LWM2MVersion":
                lwm2mVersion = field.getTextContent();
                break;
            case "Description2":
                description2 = field.getTextContent();
                break;
            default:
                // TODO remove for v2.0 : we keep it for backward compatibility
                // test will be done in DDFFileValidator if validation if needed
                handleError(raiseException, "Unexpected object element [%s] in %s : it will be ignored.",
                        field.getNodeName(), streamName);
                break;
            }
        }

        return new ObjectModel(id, name, description, version, multiple, mandatory, resources, urn, lwm2mVersion,
                description2);

    }

    private ResourceModel parseResource(Node item, String streamName, boolean raiseException)
            throws DOMException, InvalidDDFFileException {

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
                    // TODO remove for v2.0 : we keep it for backward compatibility
                    // test will be done in DDFFileValidator or ObjectModelValidatorif validation if needed
                    handleError(raiseException,
                            "Invalid Value for [MultipleInstances] field in %s. It MUST be 'Multiple' or 'Single' but it was %s : We will consider it as a 'Single' value.",
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
                    // TODO remove for v2.0 : we keep it for backward compatibility
                    // test will be done in DDFFileValidator or ObjectModelValidatorif validation if needed
                    handleError(raiseException,
                            "Invalid Value for [Mandatory] field in %s. It MUST be 'Mandatory' or 'Optional' but it was %s :s We will consider it as a 'Optional' value.",
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
                    // TODO remove for v2.0 : we keep it for backward compatibility
                    // test will be done in DDFFileValidator validation if needed
                    handleError(raiseException, "Unexpected type value [%s] in %s : no type assigned",
                            field.getTextContent(), streamName);
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
                // TODO remove for v2.0 : we keep it for backward compatibility
                // test will be done in DDFFileValidator validation if needed
                handleError(raiseException, "Unexpected resource element [%s %s] in %s : it will be ignored.",
                        field.getNodeName(), field.getNodeType(), streamName);
                break;
            }
        }

        // TODO remove for v2.0 : we keep it for backward compatibility
        // test will be done in ObjetModelValidator if validation if needed
        if (!raiseException) {
            if (operations.isExecutable() && type != null) {
                LOG.warn("Model for Resource {}({}) in {} is invalid : an executable resource MUST NOT have a type({})",
                        name, id, streamName, type);
            } else if (!operations.isExecutable() && type == null) {
                LOG.warn(
                        "Model for Resource {}({}) in {} is invalid : a none executable resource MUST have a type. We will consider it as String.",
                        name, id, streamName, type);
                type = Type.STRING;
            }
        }

        return new ResourceModel(id, name, operations, multiple, mandatory, type, rangeEnumeration, units, description);
    }

    private void handleError(boolean raiseException, String Message, Object... args) throws InvalidDDFFileException {
        String msg = String.format(Message, args);
        if (raiseException) {
            throw new InvalidDDFFileException(msg);
        } else {
            LOG.warn(msg);
        }
    }
}

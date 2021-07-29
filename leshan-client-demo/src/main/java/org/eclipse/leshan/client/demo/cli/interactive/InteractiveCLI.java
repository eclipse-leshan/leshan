/*******************************************************************************
 * Copyright (c) 2021 Sierra Wireless and others.
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
package org.eclipse.leshan.client.demo.cli.interactive;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.demo.MyLocation;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.model.LwM2mModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InteractiveCLI {

    private static final Logger LOG = LoggerFactory.getLogger(InteractiveCLI.class);

    private LeshanClient client;
    private LwM2mModel model;

    public InteractiveCLI(LeshanClient client, LwM2mModel model) throws IOException {
        this.client = client;
        this.model = model;
    }

    public void showHelp() {
        // Print commands help
        StringBuilder commandsHelp = new StringBuilder("Commands available :");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append(" - create <objectId> : to enable a new object.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append(" - delete <objectId> : to disable a new object.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append(" - update : to trigger a registration update.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append(" - w : to move to North.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append(" - a : to move to East.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append(" - s : to move to South.");
        commandsHelp.append(System.lineSeparator());
        commandsHelp.append(" - d : to move to West.");
        commandsHelp.append(System.lineSeparator());
        LOG.info(commandsHelp.toString());
    }

    public void start() throws IOException {
        // Change the location through the Console
        try (Scanner scanner = new Scanner(System.in)) {
            List<Character> wasdCommands = Arrays.asList('w', 'a', 's', 'd');
            while (scanner.hasNext()) {
                String command = scanner.next();
                if (command.startsWith("create")) {
                    try {
                        int objectId = scanner.nextInt();
                        if (client.getObjectTree().getObjectEnabler(objectId) != null) {
                            LOG.info("Object {} already enabled.", objectId);
                        }
                        if (model.getObjectModel(objectId) == null) {
                            LOG.info("Unable to enable Object {} : there no model for this.", objectId);
                        } else {
                            ObjectsInitializer objectsInitializer = new ObjectsInitializer(model);
                            objectsInitializer.setDummyInstancesForObject(objectId);
                            LwM2mObjectEnabler object = objectsInitializer.create(objectId);
                            client.getObjectTree().addObjectEnabler(object);
                        }
                    } catch (Exception e) {
                        // skip last token
                        scanner.next();
                        LOG.info("Invalid syntax, <objectid> must be an integer : create <objectId>");
                    }
                } else if (command.startsWith("delete")) {
                    try {
                        int objectId = scanner.nextInt();
                        if (objectId == 0 || objectId == 0 || objectId == 3) {
                            LOG.info("Object {} can not be disabled.", objectId);
                        } else if (client.getObjectTree().getObjectEnabler(objectId) == null) {
                            LOG.info("Object {} is not enabled.", objectId);
                        } else {
                            client.getObjectTree().removeObjectEnabler(objectId);
                        }
                    } catch (Exception e) {
                        // skip last token
                        scanner.next();
                        LOG.info("\"Invalid syntax, <objectid> must be an integer : delete <objectId>");
                    }
                } else if (command.startsWith("update")) {
                    client.triggerRegistrationUpdate();
                } else if (command.length() == 1 && wasdCommands.contains(command.charAt(0))) {
                    LwM2mObjectEnabler objectEnabler = client.getObjectTree().getObjectEnabler(LwM2mId.LOCATION);
                    if (objectEnabler != null && objectEnabler instanceof ObjectEnabler) {
                        LwM2mInstanceEnabler instance = ((ObjectEnabler) objectEnabler).getInstance(0);
                        if (instance instanceof MyLocation) {
                            ((MyLocation) instance).moveLocation(command);
                        }
                    }
                } else {
                    LOG.info("Unknown command '{}'", command);
                }
            }
        }
    }
}

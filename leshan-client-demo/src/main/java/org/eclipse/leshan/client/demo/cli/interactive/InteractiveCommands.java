/*******************************************************************************
 * Copyright (c) 2022    Sierra Wireless and others.
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

import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.LeshanClient;
import org.eclipse.leshan.client.demo.MyLocation;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.CollectCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.CreateCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.DeleteCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.ListCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.MoveCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.SendCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.UpdateCommand;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectTree;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.send.ManualDataSender;
import org.eclipse.leshan.client.send.NoDataException;
import org.eclipse.leshan.client.send.SendService;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.demo.cli.converters.ContentFormatConverter;
import org.eclipse.leshan.core.demo.cli.converters.LwM2mPathConverter;
import org.eclipse.leshan.core.demo.cli.converters.StringLwM2mPathConverter;
import org.eclipse.leshan.core.demo.cli.converters.VersionConverter;
import org.eclipse.leshan.core.demo.cli.interactive.JLineInteractiveCommands;
import org.eclipse.leshan.core.model.LwM2mModelRepository;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.ResourceModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

/**
 * Interactive commands for the Leshan Client Demo
 */
@Command(name = "",
         description = "@|bold,underline Leshan Client Demo Interactive Console :|@%n",
         footer = { "%n@|italic Press Ctl-C to exit.|@%n" },
         subcommands = { HelpCommand.class, ListCommand.class, CreateCommand.class, DeleteCommand.class,
                 UpdateCommand.class, SendCommand.class, CollectCommand.class, MoveCommand.class },
         customSynopsis = { "" },
         synopsisHeading = "")
public class InteractiveCommands extends JLineInteractiveCommands implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(InteractiveCommands.class);

    private final LeshanClient client;
    private final LwM2mModelRepository repository;

    public InteractiveCommands(LeshanClient client, LwM2mModelRepository repository) {
        this.client = client;
        this.repository = repository;
    }

    @Override
    public void run() {
        printUsageMessage();
    }

    /**
     * A command to list objects.
     */
    @Command(name = "list",
             description = "List available Objects, Instances and Resources",
             headerHeading = "%n",
             footer = "")
    static class ListCommand implements Runnable {

        @Parameters(description = "Id of the object, if no value is specified all available objects will be listed.",
                    index = "0",
                    arity = "0..1")
        private Integer objectId;

        @ParentCommand
        InteractiveCommands parent;

        @Override
        public void run() {
            LwM2mObjectTree objectTree = parent.client.getObjectTree();
            if (objectTree == null) {
                parent.printf("no object.%n");
                parent.flush();
                return;
            }
            if (objectId != null) {
                // print object with given id
                LwM2mObjectEnabler objectEnabler = objectTree.getObjectEnablers().get(objectId);
                if (objectEnabler == null) {
                    parent.printf("no object available with id %d.%n", objectId);
                    parent.flush();
                    return;
                }
                printObject(objectEnabler);
                parent.flush();
            } else {
                // print all objects
                objectTree.getObjectEnablers().forEach((objectId, objectEnabler) -> {
                    printObject(objectEnabler);
                });
                parent.flush();
            }
        }

        public void printObject(LwM2mObjectEnabler objectEnabler) {
            ObjectModel objectModel = objectEnabler.getObjectModel();
            objectEnabler.getAvailableInstanceIds().forEach(instance -> {
                parent.printfAnsi("@|bold,fg(magenta) /%d/%d : |@ @|bold,fg(green) %s |@ %n", objectModel.id, instance,
                        objectModel.name);
                List<Integer> availableResources = objectEnabler.getAvailableResourceIds(instance);
                availableResources.forEach(resourceId -> {
                    ResourceModel resourceModel = objectModel.resources.get(resourceId);
                    parent.printfAnsi("  /%d : @|bold,fg(cyan) %s |@ %n", resourceId, resourceModel.name);
                });
            });
        }
    }

    /**
     * A command to create object enabler.
     */
    @Command(name = "create", description = "Enable a new Object", headerHeading = "%n", footer = "")
    static class CreateCommand implements Runnable {

        @Parameters(description = "Id of the LWM2M object to enable", index = "0")
        private Integer objectId;

        @Parameters(description = "Version of the LwM2M object to enable, if not precised the most recent one is picked",
                    index = "1",
                    arity = "0..1",
                    converter = VersionConverter.class)
        private Version version;

        @ParentCommand
        InteractiveCommands parent;

        @Override
        public void run() {
            if (parent.client.getObjectTree().getObjectEnabler(objectId) != null) {
                parent.printf("Object %d already enabled.%n", objectId).flush();
            } else {
                ObjectModel objectModel;
                if (version != null)
                    objectModel = parent.repository.getObjectModel(objectId, version);
                else {
                    objectModel = parent.repository.getObjectModel(objectId);
                }
                if (objectModel == null) {
                    if (version == null) {
                        parent.printf("Unable to enable Object %d : there no model for this object.%n", objectId);
                    } else {
                        parent.printf("Unable to enable Object %d : there no model for this object in version %s.%n",
                                objectId, version);
                    }
                    parent.flush();
                } else {
                    ObjectsInitializer objectsInitializer = new ObjectsInitializer(new StaticModel(objectModel));
                    objectsInitializer.setDummyInstancesForObject(objectId);
                    LwM2mObjectEnabler object = objectsInitializer.create(objectId);
                    parent.client.getObjectTree().addObjectEnabler(object);
                }
            }
        }
    }

    /**
     * A command to delete object enabler.
     */
    @Command(name = "delete", description = "Disable a new object", headerHeading = "%n", footer = "")
    static class DeleteCommand implements Runnable {

        @Parameters(description = "Id of the LWM2M object to enable")
        private Integer objectId;

        @ParentCommand
        InteractiveCommands parent;

        @Override
        public void run() {
            if (objectId == 0 || objectId == 1 || objectId == 3) {
                parent.printf("Object %d can not be disabled.%n", objectId).flush();
            } else if (parent.client.getObjectTree().getObjectEnabler(objectId) == null) {
                parent.printf("Object %d is not enabled.%n", objectId).flush();
            } else {
                parent.client.getObjectTree().removeObjectEnabler(objectId);
            }
        }
    }

    /**
     * A command to send an update request.
     */
    @Command(name = "update", description = "Trigger a registration update.", headerHeading = "%n", footer = "")
    static class UpdateCommand implements Runnable {

        @ParentCommand
        InteractiveCommands parent;

        @Override
        public void run() {
            parent.client.triggerRegistrationUpdate();
        }
    }

    /**
     * A command to sebd data.
     */
    @Command(name = "send",
             description = "Send data to server",
             subcommands = { SendCurrentValue.class, SendCollectedValue.class },
             synopsisSubcommandLabel = "(current-value | collected-value)",
             headerHeading = "%n",
             footer = "")
    static class SendCommand implements Runnable {

        @Option(names = { "-c", "--content-format" },
                defaultValue = "SENML_CBOR",
                description = { //
                        "Name (e.g. SENML_JSON) or code (e.g. 110) of Content Format used to send data.", //
                        "Default : ${DEFAULT-VALUE}" },
                converter = SendContentFormatConverver.class)
        ContentFormat contentFormat;

        public static class SendContentFormatConverver extends ContentFormatConverter {
            public SendContentFormatConverver() {
                super(ContentFormat.SENML_CBOR, ContentFormat.SENML_JSON);
            }
        }

        long timeout = 2000; // ms

        @Spec
        CommandSpec spec;

        @ParentCommand
        InteractiveCommands parent;

        @Override
        public void run() {
            throw new ParameterException(spec.commandLine(), "Missing required subcommand");
        }
    }

    @Command(name = "current-value", description = "Send current value", headerHeading = "%n", footer = "")
    static class SendCurrentValue implements Runnable {

        @Parameters(description = "Paths of data to send.", converter = StringLwM2mPathConverter.class, arity = "1..*")
        private List<String> paths;

        @ParentCommand
        SendCommand sendCommand;

        @Override
        public void run() {
            Map<String, ServerIdentity> registeredServers = sendCommand.parent.client.getRegisteredServers();
            if (registeredServers.isEmpty()) {
                sendCommand.parent.printf("There is no registered server to send to.%n").flush();
            }
            for (final ServerIdentity server : registeredServers.values()) {
                LOG.info("Sending Data to {} using {}.", server, sendCommand.contentFormat);
                ResponseCallback<SendResponse> responseCallback = (response) -> {
                    if (response.isSuccess())
                        LOG.info("Data sent successfully to {} [{}].", server, response.getCode());
                    else
                        LOG.info("Send data to {} failed [{}] : {}.", server, response.getCode(),
                                response.getErrorMessage() == null ? "" : response.getErrorMessage());
                };
                ErrorCallback errorCallback = (e) -> LOG.warn("Unable to send data to {}.", server, e);
                sendCommand.parent.client.getSendService().sendData(server, sendCommand.contentFormat, paths,
                        sendCommand.timeout, responseCallback, errorCallback);
            }
        }
    }

    @Command(name = "collected-value",
             description = "Send values collected with 'collect' command",
             headerHeading = "%n",
             footer = "")
    static class SendCollectedValue implements Runnable {
        @ParentCommand
        SendCommand sendCommand;

        @Override
        public void run() {
            // get registered servers
            Map<String, ServerIdentity> registeredServers = sendCommand.parent.client.getRegisteredServers();
            if (registeredServers.isEmpty()) {
                sendCommand.parent.printf("There is no registered server to send to.%n").flush();
            }
            // for each server send data
            for (final ServerIdentity server : registeredServers.values()) {
                LOG.info("Sending Collected data to {} using {}.", server, sendCommand.contentFormat);
                // send collected data
                SendService sendService = sendCommand.parent.client.getSendService();
                try {
                    sendService.getDataSender(ManualDataSender.DEFAULT_NAME, ManualDataSender.class)
                            .sendCollectedData(server, sendCommand.contentFormat, sendCommand.timeout, false);
                } catch (NoDataException e) {
                    sendCommand.parent.printf("No data collected, use `collect` command before.%n").flush();
                }
            }
        }
    }

    /**
     * A command to collect data.
     */
    @Command(name = "collect",
             description = "Collect data to send it later with 'send' command",
             headerHeading = "%n",
             footer = "")
    static class CollectCommand implements Runnable {

        @Parameters(description = "Paths of data to collect.", converter = LwM2mPathConverter.class)
        private List<LwM2mPath> paths;

        @ParentCommand
        InteractiveCommands parent;

        @Override
        public void run() {
            parent.client.getSendService().getDataSender(ManualDataSender.DEFAULT_NAME, ManualDataSender.class)
                    .collectData(paths);
        }
    }

    /**
     * A command to move client.
     */
    @Command(name = "move",
             description = "Simulate client mouvement.",
             headerHeading = "%n",
             footer = "",
             sortOptions = false)
    static class MoveCommand implements Runnable {

        @ParentCommand
        InteractiveCommands parent;

        @Option(names = { "-w", "north" }, description = "Move to the North")
        boolean north;

        @Option(names = { "-a", "east" }, description = "Move to the East")
        boolean east;

        @Option(names = { "-s", "south" }, description = "Move to the South")
        boolean south;

        @Option(names = { "-d", "west" }, description = "Move to the West")
        boolean west;

        @Override
        public void run() {
            LwM2mObjectEnabler objectEnabler = parent.client.getObjectTree().getObjectEnabler(LwM2mId.LOCATION);
            if (objectEnabler != null && objectEnabler instanceof ObjectEnabler) {
                LwM2mInstanceEnabler instance = ((ObjectEnabler) objectEnabler).getInstance(0);
                if (instance instanceof MyLocation) {
                    MyLocation location = (MyLocation) instance;
                    if (north)
                        location.moveLocation("w");
                    if (east)
                        location.moveLocation("a");
                    if (south)
                        location.moveLocation("s");
                    if (west)
                        location.moveLocation("d");
                }
            }
        }
    }
}

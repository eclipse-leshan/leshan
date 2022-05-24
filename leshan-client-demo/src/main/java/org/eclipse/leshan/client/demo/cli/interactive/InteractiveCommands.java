package org.eclipse.leshan.client.demo.cli.interactive;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.datacollector.ManualDataSender;
import org.eclipse.leshan.client.demo.MyLocation;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.CollectCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.CreateCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.DeleteCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.MoveCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.SendCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.UpdateCommand;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.client.servers.ServerIdentity;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.demo.cli.converters.ContentFormatConverter;
import org.eclipse.leshan.core.demo.cli.converters.StringLwM2mPathConverter;
import org.eclipse.leshan.core.demo.cli.converters.VersionConverter;
import org.eclipse.leshan.core.demo.cli.interactive.JLineInteractiveCommands;
import org.eclipse.leshan.core.model.LwM2mModelRepository;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.model.StaticModel;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.response.ErrorCallback;
import org.eclipse.leshan.core.response.ResponseCallback;
import org.eclipse.leshan.core.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jline.console.ConsoleReader;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Interactive commands for the Leshan Client Demo
 */
@Command(name = "",
         description = "@|bold,underline Leshan Client Demo Interactive Console :|@%n",
         footer = { "%n@|italic Press Ctl-C to exit.|@%n" },
         subcommands = { HelpCommand.class, CreateCommand.class, DeleteCommand.class, UpdateCommand.class,
                 SendCommand.class, CollectCommand.class, MoveCommand.class },
         customSynopsis = { "" },
         synopsisHeading = "")
public class InteractiveCommands implements Runnable, JLineInteractiveCommands {

    private static final Logger LOG = LoggerFactory.getLogger(InteractiveCommands.class);

    private PrintWriter out;

    private LeshanClient client;
    private LwM2mModelRepository repository;
    private ManualDataSender manualDataSender;

    public InteractiveCommands(LeshanClient client, LwM2mModelRepository repository,
            ManualDataSender manualDataSender) {
        this.client = client;
        this.repository = repository;
        this.manualDataSender = manualDataSender;
    }

    @Override
    public void setConsole(ConsoleReader console) {
        out = new PrintWriter(console.getOutput());
    }

    @Override
    public void run() {
        out.print(new CommandLine(this).getUsageMessage());
        out.flush();
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
                parent.out.printf("Object %d already enabled.%n", objectId);
                parent.out.flush();
            } else {
                ObjectModel objectModel;
                if (version != null)
                    objectModel = parent.repository.getObjectModel(objectId, version);
                else {
                    objectModel = parent.repository.getObjectModel(objectId);
                }
                if (objectModel == null) {
                    if (version == null) {
                        parent.out.printf("Unable to enable Object %d : there no model for this object.%n", objectId);
                    } else {
                        parent.out.printf(
                                "Unable to enable Object %d : there no model for this object in version %s.%n",
                                objectId, version);
                    }
                    parent.out.flush();
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
                parent.out.printf("Object %d can not be disabled.", objectId);
                parent.out.flush();
            } else if (parent.client.getObjectTree().getObjectEnabler(objectId) == null) {
                parent.out.printf("Object %d is not enabled.", objectId);
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
    @Command(name = "send", description = "Send data to server", headerHeading = "%n", footer = "")
    static class SendCommand implements Runnable {

        @Parameters(description = "paths of data to send.", converter = StringLwM2mPathConverter.class)
        private List<String> paths;

        @Option(names = { "-c", "--content-format" },
                defaultValue = "SENML_CBOR",
                description = { //
                        "Name (e.g. SENML_JSON) or code (e.g. 110) of Content Format used to send data.", //
                        "Default : ${DEFAULT-VALUE}" },
                converter = SendContentFormatConverver.class)
        ContentFormat contentFormat;

        @Option(names = { "-cd", "--collected-data" },
                defaultValue = "false",
                description = { "to send data collected with 'collect' command.", "Default: false" })
        Boolean sendCollectedData;

        public static class SendContentFormatConverver extends ContentFormatConverter {
            public SendContentFormatConverver() {
                super(ContentFormat.SENML_CBOR, ContentFormat.SENML_JSON);
            }
        }

        @ParentCommand
        InteractiveCommands parent;

        @Override
        public void run() {
            Map<String, ServerIdentity> registeredServers = parent.client.getRegisteredServers();
            if (registeredServers.isEmpty()) {
                parent.out.printf("There is no registered server to send to.\n");
                parent.out.flush();
            }

            for (final ServerIdentity server : registeredServers.values()) {
                LOG.info("Sending Data to {} using {}.", server, contentFormat);
                int timeoutInMs = 2000;
                ResponseCallback<SendResponse> responseCallback = (response) -> {
                    if (response.isSuccess())
                        LOG.info("Data sent successfully to {} [{}].", server, response.getCode());
                    else
                        LOG.info("Send data to {} failed [{}] : {}.", server, response.getCode(),
                                response.getErrorMessage() == null ? "" : response.getErrorMessage());
                };
                ErrorCallback errorCallback = (e) -> LOG.warn("Unable to send data to {}.", server, e);
                if (sendCollectedData) {
                    parent.manualDataSender.sendCollectedData(server, contentFormat, timeoutInMs);
                } else {
                    parent.client.sendData(server, contentFormat, paths, timeoutInMs, responseCallback, errorCallback);
                }
            }
        }
    }

    /**
     * A command to sebd data.
     */
    @Command(name = "collect",
             description = "Collect data to send it later with 'send' command",
             headerHeading = "%n",
             footer = "")
    static class CollectCommand implements Runnable {

        @Parameters(description = "paths of data to collect.", converter = StringLwM2mPathConverter.class)
        private List<String> paths; // TODO we should use LWM2M path

        @ParentCommand
        InteractiveCommands parent;

        @Override
        public void run() {
            parent.manualDataSender.collectData(LwM2mPath.getLwM2mPathList(paths).toArray(new LwM2mPath[paths.size()]));
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
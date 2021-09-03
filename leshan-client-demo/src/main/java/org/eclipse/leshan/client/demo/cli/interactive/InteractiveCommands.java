package org.eclipse.leshan.client.demo.cli.interactive;

import java.io.PrintWriter;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.demo.MyLocation;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.CreateCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.DeleteCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.MoveCommand;
import org.eclipse.leshan.client.demo.cli.interactive.InteractiveCommands.UpdateCommand;
import org.eclipse.leshan.client.resource.LwM2mInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.LwM2mId;
import org.eclipse.leshan.core.demo.cli.interactive.JLineInteractiveCommands;
import org.eclipse.leshan.core.model.LwM2mModel;

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
                 MoveCommand.class },
         customSynopsis = { "" },
         synopsisHeading = "")
public class InteractiveCommands implements Runnable, JLineInteractiveCommands {

    private PrintWriter out;

    private LeshanClient client;
    private LwM2mModel model;

    public InteractiveCommands(LeshanClient client, LwM2mModel model) {
        this.client = client;
        this.model = model;
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

        @Parameters(description = "Id of the LWM2M object to enable")
        private Integer objectId;

        @ParentCommand
        InteractiveCommands parent;

        @Override
        public void run() {
            if (parent.client.getObjectTree().getObjectEnabler(objectId) != null) {
                parent.out.printf("Object %d already enabled.%n", objectId);
                parent.out.flush();
            } else if (parent.model.getObjectModel(objectId) == null) {
                parent.out.printf("Unable to enable Object %d : there no model for this.%n", objectId);
                parent.out.flush();
            } else {
                ObjectsInitializer objectsInitializer = new ObjectsInitializer(parent.model);
                objectsInitializer.setDummyInstancesForObject(objectId);
                LwM2mObjectEnabler object = objectsInitializer.create(objectId);
                parent.client.getObjectTree().addObjectEnabler(object);
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
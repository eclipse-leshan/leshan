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
 *     Zebra Technologies - initial API and implementation
 *     Sierra Wireless, - initial API and implementation
 *     Bosch Software Innovations GmbH, - initial API and implementation
 *******************************************************************************/

package org.eclipse.leshan.client.example;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.exchange.LwM2mExchange;
import org.eclipse.leshan.client.request.AbstractRegisteredLwM2mClientRequest;
import org.eclipse.leshan.client.request.DeregisterRequest;
import org.eclipse.leshan.client.request.RegisterRequest;
import org.eclipse.leshan.client.request.identifier.ClientIdentifier;
import org.eclipse.leshan.client.resource.LwM2mClientObjectDefinition;
import org.eclipse.leshan.client.resource.MultipleResourceDefinition;
import org.eclipse.leshan.client.resource.SingleResourceDefinition;
import org.eclipse.leshan.client.resource.integer.IntegerLwM2mExchange;
import org.eclipse.leshan.client.resource.integer.IntegerLwM2mResource;
import org.eclipse.leshan.client.resource.multiple.MultipleLwM2mExchange;
import org.eclipse.leshan.client.resource.multiple.MultipleLwM2mResource;
import org.eclipse.leshan.client.resource.string.StringLwM2mExchange;
import org.eclipse.leshan.client.resource.string.StringLwM2mResource;
import org.eclipse.leshan.client.resource.time.TimeLwM2mExchange;
import org.eclipse.leshan.client.resource.time.TimeLwM2mResource;
import org.eclipse.leshan.client.response.ExecuteResponse;
import org.eclipse.leshan.client.response.OperationResponse;

/*
 * To build: 
 * mvn assembly:assembly -DdescriptorId=jar-with-dependencies
 * To use:
 * java -jar target/leshan-client-*-SNAPSHOT-jar-with-dependencies.jar 127.0.0.1 5683 9000
 */
public class LeshanClientExample {
    private ClientIdentifier clientIdentifier;

    public static void main(final String[] args) {
        if (args.length < 4) {
            System.out
                    .println("Usage:\njava -jar target/leshan-client-example-*-SNAPSHOT-jar-with-dependencies.jar [Client IP] [Client port] [Server IP] [Server Port]");
        } else {
            new LeshanClientExample(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
        }
    }

    public LeshanClientExample(final String localHostName, final int localPort, final String serverHostName,
            final int serverPort) {
        final LwM2mClientObjectDefinition objectDevice = createObjectDefinition();
        final InetSocketAddress clientAddress = new InetSocketAddress(localHostName, localPort);
        final InetSocketAddress serverAddress = new InetSocketAddress(serverHostName, serverPort);

        final LeshanClient client = new LeshanClient(clientAddress, serverAddress, objectDevice);
        // Start the client
        client.start();

        // Register to the server provided
        final String endpointIdentifier = UUID.randomUUID().toString();
        final RegisterRequest registerRequest = new RegisterRequest(endpointIdentifier, new HashMap<String, String>());
        final OperationResponse operationResponse = client.send(registerRequest);

        // Report registration response.
        System.out.println("Device Registration (Success? " + operationResponse.isSuccess() + ")");
        if (operationResponse.isSuccess()) {
            System.out
                    .println("\tDevice: Registered Client Location '" + operationResponse.getClientIdentifier() + "'");
            clientIdentifier = operationResponse.getClientIdentifier();
        } else {
            System.err.println("\tDevice Registration Error: " + operationResponse.getErrorMessage());
            System.err
                    .println("If you're having issues connecting to the LWM2M endpoint, try using the DTLS port instead");
        }

        // Deregister on shutdown and stop client.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (clientIdentifier != null) {
                    System.out.println("\tDevice: Deregistering Client '" + clientIdentifier + "'");
                    final AbstractRegisteredLwM2mClientRequest deregisterRequest = new DeregisterRequest(
                            clientIdentifier);
                    final OperationResponse deregisterResponse = client.send(deregisterRequest);
                    client.stop();
                }
            }
        });
    }

    private LwM2mClientObjectDefinition createObjectDefinition() {
        // Create an object model
        final StringValueResource manufacturerResource = new StringValueResource("Leshan Example Device", 0);
        final StringValueResource modelResource = new StringValueResource("Model 500", 1);
        final StringValueResource serialNumberResource = new StringValueResource("LT-500-000-0001", 2);
        final StringValueResource firmwareResource = new StringValueResource("1.0.0", 3);
        final ExecutableResource rebootResource = new ExecutableResource(4);
        final ExecutableResource factoryResetResource = new ExecutableResource(5);
        final MultipleLwM2mResource powerAvailablePowerResource = new IntegerMultipleResource(new Integer[] { 0, 4 });
        final MultipleLwM2mResource powerSourceVoltageResource = new IntegerMultipleResource(new Integer[] { 12000,
                                5000 });
        final IntegerMultipleResource powerSourceCurrentResource = new IntegerMultipleResource(new Integer[] { 150, 75 });
        final IntegerValueResource batteryLevelResource = new IntegerValueResource(92, 9);
        final MemoryFreeResource memoryFreeResource = new MemoryFreeResource();
        final IntegerMultipleResource errorCodeResource = new IntegerMultipleResource(new Integer[] { 0 });
        final TimeResource currentTimeResource = new TimeResource();
        final StringValueResource utcOffsetResource = new StringValueResource(new SimpleDateFormat("X").format(Calendar
                .getInstance().getTime()), 14);
        final StringValueResource timezoneResource = new StringValueResource(TimeZone.getDefault().getID(), 15);
        final StringValueResource bindingsResource = new StringValueResource("U", 16);

        final LwM2mClientObjectDefinition objectDevice = new LwM2mClientObjectDefinition(3, true, true,
                new SingleResourceDefinition(0, manufacturerResource, true), new SingleResourceDefinition(1,
                        modelResource, true), new SingleResourceDefinition(2, serialNumberResource, true),
                new SingleResourceDefinition(3, firmwareResource, true), new SingleResourceDefinition(4,
                        rebootResource, true), new SingleResourceDefinition(5, factoryResetResource, true),
                new MultipleResourceDefinition(6, powerAvailablePowerResource, true), new MultipleResourceDefinition(7,
                        powerSourceVoltageResource, true), new MultipleResourceDefinition(8, powerSourceCurrentResource,
                        true), new SingleResourceDefinition(9, batteryLevelResource, true),
                new SingleResourceDefinition(10, memoryFreeResource, true), new MultipleResourceDefinition(11,
                        errorCodeResource, true), new SingleResourceDefinition(12, new ExecutableResource(12), true),
                new SingleResourceDefinition(13, currentTimeResource, true), new SingleResourceDefinition(14,
                        utcOffsetResource, true), new SingleResourceDefinition(15, timezoneResource, true),
                new SingleResourceDefinition(16, bindingsResource, true));
        return objectDevice;
    }

    public class TimeResource extends TimeLwM2mResource {
        @Override
        public void handleRead(final TimeLwM2mExchange exchange) {
            System.out.println("\tDevice: Reading Current Device Time.");
            exchange.respondContent(new Date());
        }
    }

    public class MemoryFreeResource extends IntegerLwM2mResource {
        @Override
        public void handleRead(final IntegerLwM2mExchange exchange) {
            System.out.println("\tDevice: Reading Memory Free Resource");
            final Random rand = new Random();
            exchange.respondContent(114 + rand.nextInt(50));
        }
    }

    private class IntegerMultipleResource extends MultipleLwM2mResource {

        private final Map<Integer, byte[]> values;

        public IntegerMultipleResource(final Integer[] values) {
            this.values = new HashMap<>();
            for (int i = 0; i < values.length; i++) {
                this.values.put(i, ByteBuffer.allocate(4).putInt(values[i]).array());
            }
        }

        @Override
        public void handleRead(final MultipleLwM2mExchange exchange) {
            exchange.respondContent(values);
        }
    }

    public class StringValueResource extends StringLwM2mResource {

        private String value;
        private final int resourceId;

        public StringValueResource(final String initialValue, final int resourceId) {
            value = initialValue;
            this.resourceId = resourceId;
        }

        public void setValue(final String newValue) {
            value = newValue;
            notifyResourceUpdated();
        }

        public String getValue() {
            return value;
        }

        @Override
        public void handleWrite(final StringLwM2mExchange exchange) {
            System.out.println("\tDevice: Writing on Resource " + resourceId);
            setValue(exchange.getRequestPayload());

            exchange.respondSuccess();
        }

        @Override
        public void handleRead(final StringLwM2mExchange exchange) {
            System.out.println("\tDevice: Reading on Resource " + resourceId);
            exchange.respondContent(value);
        }

    }

    public class IntegerValueResource extends IntegerLwM2mResource {

        private Integer value;
        private final int resourceId;

        public IntegerValueResource(final int initialValue, final int resourceId) {
            value = initialValue;
            this.resourceId = resourceId;
        }

        public void setValue(final Integer newValue) {
            value = newValue;
            notifyResourceUpdated();
        }

        public Integer getValue() {
            return value;
        }

        @Override
        public void handleWrite(final IntegerLwM2mExchange exchange) {
            System.out.println("\tDevice: Writing on Integer Resource " + resourceId);
            setValue(exchange.getRequestPayload());

            exchange.respondSuccess();
        }

        @Override
        public void handleRead(final IntegerLwM2mExchange exchange) {
            System.out.println("\tDevice: Reading on IntegerResource " + resourceId);
            exchange.respondContent(value);
        }

    }

    public class ExecutableResource extends StringLwM2mResource {

        private final int resourceId;

        public ExecutableResource(final int resourceId) {
            this.resourceId = resourceId;
        }

        @Override
        public void handleExecute(final LwM2mExchange exchange) {
            System.out.println("Executing on Resource " + resourceId);

            exchange.respond(ExecuteResponse.success());
        }

        @Override
        protected void handleWrite(final StringLwM2mExchange exchange) {
            exchange.respondSuccess();
        }

    }
}

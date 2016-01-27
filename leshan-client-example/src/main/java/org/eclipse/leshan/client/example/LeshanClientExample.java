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

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.eclipse.leshan.ResponseCode;
import org.eclipse.leshan.client.californium.LeshanClient;
import org.eclipse.leshan.client.resource.BaseInstanceEnabler;
import org.eclipse.leshan.client.resource.LwM2mObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectEnabler;
import org.eclipse.leshan.client.resource.ObjectsInitializer;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.request.DeregisterRequest;
import org.eclipse.leshan.core.request.RegisterRequest;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.RegisterResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/*
 * To build:
 * mvn assembly:assembly -DdescriptorId=jar-with-dependencies
 * To use:
 * java -jar target/leshan-client-*-SNAPSHOT-jar-with-dependencies.jar 127.0.0.1 5683
 */
public class LeshanClientExample {
    private static String registrationID;
    private static String endpointIdentifier;
    private final Location locationInstance = new Location();
    private LeshanClient client;

    private static final Logger LOG = LoggerFactory.getLogger(LeshanClientExample.class);


    public static void main(final String[] args) {
        LeshanClientExample clientExample = null;
        if (args.length != 4 && args.length != 2) {
            System.out.println(
                    "Usage:\njava -jar target/leshan-client-example-*-SNAPSHOT-jar-with-dependencies.jar [ClientIP] [ClientPort] ServerIP ServerPort");
        }
        else {
            if (args.length == 4)
                clientExample = new LeshanClientExample(args[0], Integer.parseInt(args[1]), args[2],
                        Integer.parseInt(args[3]));
            else
                clientExample = new LeshanClientExample("0", 0, args[0], Integer.parseInt(args[1]));
        }
    }

    public LeshanClientExample(final String localHostName, final int localPort, final String serverHostName,
                               final int serverPort) {

        // Initialize object list
        ObjectsInitializer initializer = new ObjectsInitializer();

        initializer.setClassForObject(3, Device.class);
        initializer.setClassForObject(500, HeartBeat.class);
        initializer.setClassForObject(5, FirmwareUpdate.class);
        initializer.setInstancesForObject(6, locationInstance);
        List<ObjectEnabler> enablers = initializer.createMandatory();
        enablers.add(initializer.create(6));

        // Create client
        final InetSocketAddress clientAddress = new InetSocketAddress(localHostName, localPort);
        final InetSocketAddress serverAddress = new InetSocketAddress(serverHostName, serverPort);

        /*final LeshanClient client = new LeshanClient(clientAddress, serverAddress, new ArrayList<LwM2mObjectEnabler>(
                enablers));*/

        client = new LeshanClient(clientAddress, serverAddress, new ArrayList<LwM2mObjectEnabler>(enablers));

        // Start the client
        client.start();

        // Register to the server
        //final String endpointIdentifier = UUID.randomUUID().toString();
        endpointIdentifier = UUID.randomUUID().toString();
        RegisterResponse response = client.send(new RegisterRequest(endpointIdentifier));
        if (response == null) {
            System.out.println("Registration request timeout");
            return;
        }

        System.out.println("Device Registration (Success? " + response.getCode() + ")");
        if (response.getCode() != ResponseCode.CREATED) {
            // TODO Should we have a error message on response ?
            // System.err.println("\tDevice Registration Error: " + response.getErrorMessage());
            System.err
                    .println("If you're having issues connecting to the LWM2M endpoint, try using the DTLS port instead");
            return;
        }

        registrationID = response.getRegistrationID();
        System.out.println("\tDevice: Registered Client Location '" + registrationID + "'");

        // Deregister on shutdown and stop client.
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (registrationID != null) {
                    System.out.println("\tDevice: Deregistering Client '" + registrationID + "'");
                    client.send(new DeregisterRequest(registrationID), 1000);
                    client.stop();
                }
            }
        });

        // Change the location through the Console
        /*Scanner scanner = new Scanner(System.in);
        System.out.println("Press 'w','a','s','d' to change reported Location.");
        while (scanner.hasNext()) {
            String nextMove = scanner.next();
            locationInstance.moveLocation(nextMove);
        }
        scanner.close();*/
    }

    public static class Device extends BaseInstanceEnabler {

        public Device() {
            // notify new date each 5 second
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    fireResourcesChange(13);
                }
            }, 5000, 5000);
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Device Resource " + resourceid);
            switch (resourceid) {
                case 0:
                    return ReadResponse.success(resourceid, getManufacturer());
                case 1:
                    return ReadResponse.success(resourceid, getModelNumber());
                case 2:
                    return ReadResponse.success(resourceid, getSerialNumber());
                case 3:
                    return ReadResponse.success(resourceid, getFirmwareVersion());
                case 9:
                    return ReadResponse.success(resourceid, getBatteryLevel());
                case 10:
                    return ReadResponse.success(resourceid, getMemoryFree());
                case 11:
                    Map<Integer, Long> errorCodes = new HashMap<>();
                    errorCodes.put(0, getErrorCode());
                    return ReadResponse.success(resourceid, errorCodes, Type.INTEGER);
                case 13:
                    return ReadResponse.success(resourceid, getCurrentTime());
                case 14:
                    return ReadResponse.success(resourceid, getUtcOffset());
                case 15:
                    return ReadResponse.success(resourceid, getTimezone());
                case 16:
                    return ReadResponse.success(resourceid, getSupportedBinding());
                default:
                    return super.read(resourceid);
            }
        }

        @Override
        public ExecuteResponse execute(int resourceid, String params) {
            System.out.println("Execute on Device resource " + resourceid);
            if (params != null && params.length() != 0)
                System.out.println("\t params " + params);
            return ExecuteResponse.success();
        }

        @Override
        public WriteResponse write(int resourceid, LwM2mResource value) {
            System.out.println("Write on Device Resource " + resourceid + " value " + value);
            switch (resourceid) {
                case 13:
                    return WriteResponse.notFound();
                case 14:
                    setUtcOffset((String) value.getValue());
                    fireResourcesChange(resourceid);
                    return WriteResponse.success();
                case 15:
                    setTimezone((String) value.getValue());
                    fireResourcesChange(resourceid);
                    return WriteResponse.success();
                default:
                    return super.write(resourceid, value);
            }
        }

        private String getManufacturer() {
            return "Leshan Example Device";
        }

        private String getModelNumber() {
            return "Model 500";
        }

        private String getSerialNumber() {
            return "LT-500-000-0001";
        }

        private String getFirmwareVersion() {
            return "1.0.0";
        }

        private long getErrorCode() {
            return 0;
        }

        private int getBatteryLevel() {
            final Random rand = new Random();
            return rand.nextInt(100);
        }

        private int getMemoryFree() {
            final Random rand = new Random();
            return rand.nextInt(50) + 114;
        }

        private Date getCurrentTime() {
            return new Date();
        }

        private String utcOffset = new SimpleDateFormat("X").format(Calendar.getInstance().getTime());;

        private String getUtcOffset() {
            return utcOffset;
        }

        private void setUtcOffset(String t) {
            utcOffset = t;
        }

        private String timeZone = TimeZone.getDefault().getID();

        private String getTimezone() {
            return timeZone;
        }

        private void setTimezone(String t) {
            timeZone = t;
        }

        private String getSupportedBinding() {
            return "U";
        }
    }

    public static class Location extends BaseInstanceEnabler {
        private Random random;
        private float latitude;
        private float longitude;
        private Date timestamp;

        public Location() {
            random = new Random();
            latitude = Float.valueOf(random.nextInt(180));
            longitude = Float.valueOf(random.nextInt(360));
            timestamp = new Date();
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on Location Resource " + resourceid);
            switch (resourceid) {
                case 0:
                    return ReadResponse.success(resourceid, getLatitude());
                case 1:
                    return ReadResponse.success(resourceid, getLongitude());
                case 5:
                    return ReadResponse.success(resourceid, getTimestamp());
                default:
                    return super.read(resourceid);
            }
        }

        public void moveLocation(String nextMove) {
            switch (nextMove.charAt(0)) {
                case 'w':
                    moveLatitude(1.0f);
                    break;
                case 'a':
                    moveLongitude(-1.0f);
                    break;
                case 's':
                    moveLatitude(-1.0f);
                    break;
                case 'd':
                    moveLongitude(1.0f);
                    break;
            }
        }

        private void moveLatitude(float delta) {
            latitude = latitude + delta;
            timestamp = new Date();
            fireResourcesChange(0, 5);
        }

        private void moveLongitude(float delta) {
            longitude = longitude + delta;
            timestamp = new Date();
            fireResourcesChange(1, 5);
        }

        public String getLatitude() {
            return Float.toString(latitude - 90.0f);
        }

        public String getLongitude() {
            return Float.toString(longitude - 180.f);
        }

        public Date getTimestamp() {
            return timestamp;
        }
    }

    public static class HeartBeat extends BaseInstanceEnabler {
        private String logMessage = "Hello World !!!";

        public String getLogMessage() {
            return logMessage;
        }

        public void setLogMessage(String logMessage) {
            this.logMessage = logMessage;
        }

        @Override
        public ReadResponse read(int resourceid) {
            System.out.println("Read on HeartBeat " + resourceid);
            //return ReadResponse.success(new LwM2mResource(resourceid, Value.newStringValue(logMessage)));
            return ReadResponse.success(resourceid, logMessage);
        }

        @Override
        public WriteResponse write(int resourceid, LwM2mResource value) {
            setLogMessage((String) value.getValue());
            fireResourcesChange(resourceid);
            return WriteResponse.success();
        }
    }

    public static class FirmwareUpdate extends BaseInstanceEnabler {
        private String packageURI;
        private byte[] updatePackage;
        private byte[] updateFile;

        //private HttpClient client = new HttpClient(packageURI);

        public String getPackageURI() {
            return packageURI;
        }

        public void setPackageURI(String packageURI) {
            this.packageURI = packageURI;
        }

        public byte[] getUpdatePackage() {
            return updatePackage;
        }

        public void setUpdatePackage(byte[] updatePackage) {
            this.updatePackage = updatePackage;
        }

        public byte[] getUpdateFile() {
            return updateFile;
        }

        public void setUpdateFile(byte[] updateFile) {
            this.updateFile = updateFile;
        }

        @Override
        public ReadResponse read(int resourceid) {
            switch (resourceid){
                case 1:
                    System.out.println("Read on Firmware Update: Package URI - " + resourceid);
                    //return ReadResponse.success(new LwM2mResource(resourceid, Value.newStringValue(getPackageURI())));
                    return ReadResponse.success(resourceid, getPackageURI());
                case 6:
                    System.out.println("Read on Upload Firmware Update File : " + resourceid);
                    return ReadResponse.success(resourceid, getUpdateFile());
                default:
                    return super.read(resourceid);
            }
        }

        @Override
        public WriteResponse write(int resourceid, LwM2mResource value) {
            switch (resourceid){
                case 0:
                    setUpdatePackage((byte[]) value.getValue());
                    System.out.println("package is : " + getUpdatePackage().length);
                    /*String val = (String) value.getValue();*/
                    //System.out.println("updatePackage : " + updatePackage);
                    writeToFileOnDisk(updatePackage, getClass().getClassLoader());
                    return WriteResponse.success();
                case 1:
                    setPackageURI((String) value.getValue());
                    return WriteResponse.success();
                case 6:
                    byte[] valRecd = (byte[]) value.getValue();
                    setUpdateFile(valRecd);
                    System.out.println("getClass().getClassLoader() : " + getClass().getClassLoader());
                    LOG.info("getClass().getClassLoader() : " + getClass().getClassLoader());
                    writeToFileOnDisk(valRecd, getClass().getClassLoader());
                    System.out.println("valRecd len : " + valRecd.length);
                    LOG.info("valRecd len : " + valRecd.length);
                    return WriteResponse.success();
                default:
                    return super.write(resourceid, value);
            }
        }

        @Override
        public ExecuteResponse execute(int resourceid, String params) {
            System.out.println("Execute on FirmwareUpdate resource " + resourceid);
            if (params != null && params.length() != 0)
                System.out.println("\t params " + params);
            /*
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(packageURI);
            HttpResponse response = null;
            try {
                response = httpClient.execute(httpGet);
                int len = response.getEntity().getContent().available();
                byte[] bytes = new byte[len];
                response.getEntity().getContent().read(bytes);
                InputStream inputStream = response.getEntity().getContent();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                while ((nRead = inputStream.read(bytes, 0, bytes.length)) != -1) {
                    buffer.write(bytes, 0, nRead);
                }
                buffer.flush();
                System.out.println("Content : " + buffer.toByteArray());
            }catch (IOException ex){
                System.out.println("Exception Thrown : " + ex);
            }
            System.out.println("response recd : " + (response!=null ? response : "response is null"));
            */
            try {
                URL url = new URL(packageURI);
                URLConnection connection = url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null){
                    System.out.println("Content : " + inputLine);
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ExecuteResponse.success();
        }
    }

    public LeshanClient getClient() {
        return client;
    }

    protected static void writeToFileOnDisk(String fileContent,  ClassLoader classLoader ){
        try{
            System.out.println("Path : " + classLoader.getResource("test/firmwareUpdateRcd.bin").getPath());
            Path path = Paths.get(classLoader.getResource("test/firmwareUpdateRcd.bin").getPath().replace("/C:", ""));
            try (BufferedWriter writer = Files.newBufferedWriter(path, CREATE, APPEND)){
                writer.write(fileContent);
            }catch(IOException ex){
                System.out.println("Exception thrown : " + ex);
            }
        }catch (Exception ex){
            System.out.println("Exception thrown : " + ex.getMessage());
        }
        System.out.println("Done writting !");
    }

    protected static void writeToFileOnDisk(byte[] fileContent,  ClassLoader classLoader ){
        try{
            // System.out.println("Path : " + classLoader.getResource("test/firmwareUpdateRcd.bin").getPath());
            Path path = Paths.get("C:\\Leshan\\leshan\\leshan-client-example\\target\\classes\\test\\firmwareUpdateRcd.bin");
            Files.write(path, fileContent);
            /*ContentType.DEFAULT_BINARY
            File file = new File(path.toString());
            file.setWritable(true);
            file.*/
        }catch (Exception ex){
            System.out.println("Exception thrown : " + ex.getMessage());
        }
        System.out.println("Done writting !");
    }

}

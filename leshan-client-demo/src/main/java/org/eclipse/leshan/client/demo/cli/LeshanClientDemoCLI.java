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
package org.eclipse.leshan.client.demo.cli;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.leshan.core.CertificateUsage;
import org.eclipse.leshan.core.demo.cli.MultiParameterException;
import org.eclipse.leshan.core.demo.cli.StandardHelpOptions;
import org.eclipse.leshan.core.demo.cli.VersionProvider;
import org.eclipse.leshan.core.demo.cli.converters.CIDConverter;
import org.eclipse.leshan.core.demo.cli.converters.PortConverter;
import org.eclipse.leshan.core.demo.cli.converters.StrictlyPositiveIntegerConverter;
import org.eclipse.leshan.core.util.StringUtils;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * This is the class defining the Command Line Interface of Leshan Client Demo.
 */
@Command(name = "leshan-client-demo",
         sortOptions = false,
         description = "%n"//
                 + "@|italic " //
                 + "This is a LWM2M client demo implemented with Leshan library.%n" //
                 + "You can launch it without any option and it will try to register to a LWM2M server at " + "coap://"
                 + LeshanClientDemoCLI.DEFAULT_COAP_URL + ".%n" //
                 + "%n" //
                 + "Californium is used as CoAP library and some CoAP parameters can be tweaked in 'Californium.properties' file." //
                 + "|@%n%n",
         versionProvider = VersionProvider.class)
public class LeshanClientDemoCLI implements Runnable {

    public static final String DEFAULT_COAP_URL = "localhost:" + CoAP.DEFAULT_COAP_PORT;
    public static final String DEFAULT_COAPS_URL = "localhost:" + CoAP.DEFAULT_COAP_SECURE_PORT;

    private static String defaultEndpoint() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "LeshanClientDemo";
        }
    }

    @Mixin
    private StandardHelpOptions helpsOptions;

    /* ********************************** General Section ******************************** */
    @ArgGroup(validate = false, heading = "%n")
    public GeneralSection main = new GeneralSection();

    public static class GeneralSection {

        @Option(names = { "-u", "--server-url" },
                description = { //
                        "Set the server URL. If port is missing it will be added automatically with default value.", //
                        "Default: ", //
                        "  - " + DEFAULT_COAP_URL + " for coap", //
                        "  - " + DEFAULT_COAPS_URL + " for coaps" })
        public String url;

        @Option(names = { "-b", "--bootstrap" },
                description = { "Do bootstrap instead of registration.",
                        "In this case your server-url should target a LWM2M bootstrap server instead of a LWM2M server." })
        public boolean bootstrap;

        @Option(names = { "-n", "--endpoint-name" },
                description = { //
                        "Set the endpoint name of the Client.", //
                        "Default the hostname or 'LeshanClientDemo' if no hostname." })
        public String endpoint = LeshanClientDemoCLI.defaultEndpoint();

        @Option(names = { "-l", "--lifetime" },
                defaultValue = "300" /* 5 minutes */,
                description = { //
                        "The registration lifetime in seconds.", //
                        "Ignored if -b is used.", //
                        "Default : ${DEFAULT-VALUE}s." },
                converter = StrictlyPositiveIntegerConverter.class)
        public Integer lifetimeInSec;

        @Option(names = { "-cp", "--communication-period" },
                description = { //
                        "The communication period in seconds", //
                        "It should be smaller than the lifetime.", //
                        "It will be used even if -b is used." },
                converter = StrictlyPositiveIntegerConverter.class)
        public Integer comPeriodInSec;

        @Option(names = { "-q", "--queue-mode" }, description = { "Client use queue mode (not fully implemented)." })
        public boolean queueMode;

        @Option(names = { "-lh", "--local-address" },
                description = { //
                        "Set the local CoAP address of the Client.", //
                        "Default: any local address." })
        public String localAddress;

        @Option(names = { "-lp", "--local-port" },
                defaultValue = "0",
                description = { //
                        "Set the local CoAP port of the Client.", //
                        "Default: A valid unsused port value between 0 and 65535." },
                converter = PortConverter.class)
        public Integer localPort;

        @Option(names = { "-m", "--models-folder" },
                description = { //
                        "A folder which contains object models in OMA DDF(xml)format." })
        public File modelsFolder;

        @Option(names = { "-aa", "--additional-attributes" },
                description = { //
                        "Use additional attributes at registration time.", //
                        "syntax is :", //
                        "-aa attrName1=attrValue1,attrName2=\\\"attrValue2\\\"" },
                split = ",")

        public Map<String, String> additionalAttributes;

        @Option(names = { "-bsaa", "--bootstrap-additional-attributes" },
                description = { //
                        "Use additional attributes at bootstrap time.", //
                        "syntax is :", //
                        " -bsaa attrName1=attrValue1,attrName2=\\\"attrValue2\\\"" },
                split = ",")

        public Map<String, String> bsAdditionalAttributes;

        @Option(names = { "-ocf", "--support-old-format" },
                description = { //
                        "Activate support of old/unofficial content format.", //
                        "See https://github.com/eclipse/leshan/pull/720" })
        public boolean supportOldFormat;
    }

    /* ********************************** Location Section ******************************** */
    @ArgGroup(validate = false,
              heading = "%n@|bold,underline Object Location Options|@ %n%n"//
                      + "@|italic " //
                      + "A very Simple implementation of Object (6) location with simulated values is provided. Those options aim to set this object." //
                      + "|@%n%n")
    public LocationSection location = new LocationSection();

    public static class LocationSection {
        @Option(names = { "-pos", "--initial-position" },
                defaultValue = "random",
                description = { //
                        "Set the initial location (latitude, longitude) of the device to be reported by the Location object.", //
                        "Format: lat_float:long_float" },
                converter = PositionConverter.class)
        public Position position;

        public static class Position {
            public Float latitude;
            public Float longitude;
        };

        private static class PositionConverter implements ITypeConverter<Position> {
            @Override
            public Position convert(String pos) {
                Position position = new Position();
                if (pos.equals("random"))
                    return position;

                int colon = pos.indexOf(':');
                if (colon == -1 || colon == 0 || colon == pos.length() - 1)
                    throw new IllegalArgumentException(
                            "Position must be a set of two floats separated by a colon, e.g. 48.131:11.459");
                position.latitude = Float.valueOf(pos.substring(0, colon));
                position.longitude = Float.valueOf(pos.substring(colon + 1));
                return position;
            }
        };

        @Option(names = { "-sf", "--scale-factor" },
                defaultValue = "1.0",
                description = { //
                        "Scale factor to apply when shifting position.", //
                        "Default is ${DEFAULT-VALUE}." })
        public Float scaleFactor;

    }

    /* ********************************** DTLS Section ******************************** */
    @ArgGroup(validate = false,
              heading = "%n@|bold,underline DTLS Options|@ %n%n"//
                      + "@|italic " //
                      + "Here some options aiming to configure the client behavior when it uses CoAP over DTLS." //
                      + "%n" //
                      + "Scandium is used as DTLS library and some DTLS parameters can be tweaked in 'Californium.properties' file." //
                      + "|@%n%n")
    public DTLSSection dtls = new DTLSSection();

    public static class DTLSSection {

        @Option(names = { "-r", "--rehanshake-on-update" },
                description = { //
                        "Force reconnection/rehandshake on registration update." })
        public boolean reconnectOnUpdate;

        @Option(names = { "-f", "--force-full-handshake" },
                description = { //
                        "By default client will try to resume DTLS session by using abbreviated Handshake. This option force to always do a full handshake." })
        public boolean forceFullhandshake;

        @Option(names = { "-cid", "--connection-id" },
                defaultValue = "off",
                description = { //
                        "Control usage of DTLS connection ID.", //
                        "- 'on' to activate Connection ID support (same as -cid 0)", //
                        "- 'off' to deactivate it", //
                        "- Positive value define the size in byte of CID generated.", //
                        "- 0 value means we accept to use CID but will not generated one for foreign peer.", //
                        "Default: off" },
                converter = ClientCIDConverter.class)
        public Integer cid;

        private static class ClientCIDConverter extends CIDConverter {
            public ClientCIDConverter() {
                super(0);
            }
        };

        @Option(names = { "-c", "--cipher-suites" }, //
                description = { //
                        "Define cipher suites to use.", //
                        "CipherCuite enum value separated by ',' without spaces.", //
                        "E.g: TLS_PSK_WITH_AES_128_CCM_8,TLS_PSK_WITH_AES_128_CCM " },
                split = ",")
        public List<CipherSuite> ciphers;

        @Option(names = { "-oc", "--support-deprecated-ciphers" },
                description = { //
                        "Activate support of old/deprecated cipher suites." })
        public boolean supportDeprecatedCiphers;
    }

    /* ********************************** Identity Section ******************************** */
    @ArgGroup(exclusive = true)
    public IdentitySection identity = new IdentitySection();

    @Spec
    CommandSpec spec;

    @Override
    public void run() {
        // Some post-validation which imply several options.
        // For validation about only one option, just use ITypeConverter instead

        // check certificate usage
        if (identity.isx509()) {
            if (identity.getX509().certUsage == CertificateUsage.SERVICE_CERTIFICATE_CONSTRAINT
                    && identity.getX509().trustStore.isEmpty()) {
                throw new MultiParameterException(spec.commandLine(),
                        "You need to set a truststore when you are using \"service certificate constraint\" usage",
                        "-cu", "-ts");
            }
        }

        normalizedServerUrl();
    }

    protected void normalizedServerUrl() {
        String url = main.url;
        if (url == null)
            url = "localhost";

        // try to guess if port is present.
        String[] splittedUrl = url.split(":");
        String port = splittedUrl[splittedUrl.length - 1];
        if (!StringUtils.isNumeric(port)) {
            // it seems port is not present, so we try to add it
            if (identity.hasIdentity()) {
                main.url = url + ":" + CoAP.DEFAULT_COAP_SECURE_PORT;
            } else {
                main.url = url + ":" + CoAP.DEFAULT_COAP_PORT;
            }
        }

        // try to guess if scheme is present :
        if (!main.url.contains("://")) {
            // it seems scheme is not present try to add it
            if (identity.hasIdentity()) {
                main.url = "coaps://" + main.url;
            } else {
                main.url = "coap://" + main.url;
            }
        }
    }
}

package org.eclipse.leshan.standalone;

import org.eclipse.jetty.plus.webapp.PlusConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.*;
import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.plus.webapp.EnvConfiguration;

import org.eclipse.leshan.server.californium.sandc_impl.SandCLeshanServer;
import org.eclipse.leshan.server.californium.sandc_impl.SandCLeshanServerBuilder;
import org.eclipse.leshan.server.impl.SecurityRegistryImpl;
import org.eclipse.leshan.standalone.servlet.ClientServlet;
import org.eclipse.leshan.standalone.servlet.ObjectSpecServlet;
import org.eclipse.leshan.standalone.servlet.SandCEventServlet;
import org.eclipse.leshan.standalone.servlet.SecurityServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.MultipartConfigElement;
import javax.xml.bind.DatatypeConverter;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.*;

/**
 * Created by Jyotsna.Bhonde on 10/28/2015.
 */
public class SandCLeshanStandalone {
    private static final Logger LOG = LoggerFactory.getLogger(SandCLeshanStandalone.class);
    private Server server;

    private static SandCLeshanServer lwServer;

    public void start() {
        // Use those ENV variables for specifying the interface to be bound for coap and coaps
        String iface = System.getenv("COAPIFACE");
        String ifaces = System.getenv("COAPSIFACE");

        // Build LWM2M server
        SandCLeshanServerBuilder builder = new SandCLeshanServerBuilder();
        if (iface != null && !iface.isEmpty()) {
            builder.setLocalAddress(iface.substring(0, iface.lastIndexOf(':')),
                    Integer.parseInt(iface.substring(iface.lastIndexOf(':') + 1, iface.length())));
        }
        if (ifaces != null && !ifaces.isEmpty()) {
            builder.setLocalAddressSecure(ifaces.substring(0, ifaces.lastIndexOf(':')),
                    Integer.parseInt(ifaces.substring(ifaces.lastIndexOf(':') + 1, ifaces.length())));
        }

        // Get public and private server key
        PrivateKey privateKey = null;
        PublicKey publicKey = null;
        // TODO @JB : these keys need to be updated for prodn deploy.
        try {
            // Get point values
            byte[] publicX = DatatypeConverter
                    .parseHexBinary("fcc28728c123b155be410fc1c0651da374fc6ebe7f96606e90d927d188894a73");
            byte[] publicY = DatatypeConverter
                    .parseHexBinary("d2ffaa73957d76984633fc1cc54d0b763ca0559a9dff9706e9f4557dacc3f52a");
            byte[] privateS = DatatypeConverter
                    .parseHexBinary("1dae121ba406802ef07c193c1ee4df91115aabd79c1ed7f4c0ef7ef6a5449400");

            // Get Elliptic Curve Parameter spec for secp256r1
            AlgorithmParameters algoParameters = AlgorithmParameters.getInstance("EC");
            algoParameters.init(new ECGenParameterSpec("secp256r1"));
            ECParameterSpec parameterSpec = algoParameters.getParameterSpec(ECParameterSpec.class);

            // Create key specs
            KeySpec publicKeySpec = new ECPublicKeySpec(new ECPoint(new BigInteger(publicX), new BigInteger(publicY)),
                    parameterSpec);
            KeySpec privateKeySpec = new ECPrivateKeySpec(new BigInteger(privateS), parameterSpec);

            // Get keys
            publicKey = KeyFactory.getInstance("EC").generatePublic(publicKeySpec);
            privateKey = KeyFactory.getInstance("EC").generatePrivate(privateKeySpec);

            builder.setSecurityRegistry(new SecurityRegistryImpl(privateKey, publicKey));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | InvalidParameterSpecException e) {
            LOG.warn("Unable to load RPK.", e);
        }

        lwServer = builder.build();
        lwServer.start();

        // Now prepare and start jetty
        String webPort = System.getenv("PORT");
        if (webPort == null || webPort.isEmpty()) {
            webPort = System.getProperty("PORT");
        }
        if (webPort == null || webPort.isEmpty()) {
            webPort = "8081";
        }
        server = new Server(Integer.valueOf(webPort));
        Configuration.ClassList classlist = Configuration.ClassList
                .setServerDefault(server);
        classlist.addAfter("org.eclipse.jetty.webapp.FragmentConfiguration",
                "org.eclipse.jetty.plus.webapp.EnvConfiguration",
                "org.eclipse.jetty.plus.webapp.PlusConfiguration");
        classlist.addBefore(
                "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                "org.eclipse.jetty.annotations.AnnotationConfiguration");


        WebAppContext root = new WebAppContext();
/*
        root.setConfigurations(new Configuration[] {
                new FragmentConfiguration(),
                new EnvConfiguration(),
                new PlusConfiguration(),
                new AnnotationConfiguration(), new WebXmlConfiguration(),
                new WebInfConfiguration(), //new TagLibConfiguration(),
                new MetaInfConfiguration(),
                new JettyWebXmlConfiguration()
        });
*/

        root.setContextPath("/");
        root.setResourceBase(this.getClass().getClassLoader().getResource("webapp").toExternalForm());
        root.setParentLoaderPriority(true);
        server.setHandler(root);

        // Create Servlet
        SandCEventServlet eventServlet = new SandCEventServlet(lwServer, lwServer.getSecureAddress().getPort());
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/event/*");

        ServletHolder clientServletHolder = new ServletHolder(new ClientServlet(lwServer, lwServer.getSecureAddress()
                .getPort()));
        clientServletHolder.getRegistration().setMultipartConfig(new MultipartConfigElement(System.getProperty("java.io.tmpdir")));
        root.addServlet(clientServletHolder, "/api/clients/*");


        ServletHolder securityServletHolder = new ServletHolder(new SecurityServlet(lwServer.getSecurityRegistry()));
        root.addServlet(securityServletHolder, "/api/security/*");

        ServletHolder objectSpecServletHolder = new ServletHolder(new ObjectSpecServlet(lwServer.getModelProvider()));
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");

        // Start jetty
        try {
            server.start();
        } catch (Exception e) {
            LOG.error("jetty error", e);
        }
    }

    public void stop() {
        try {
            lwServer.destroy();
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        LOG.debug("Starting up now ...");
        new SandCLeshanStandalone().start();
    }

    public static SandCLeshanServer getLwServer() {
        return lwServer;
    }

    // ClientRegistry can be added. Here : https://github.com/hekonsek/leshan/commit/d9959172731f82cae434c45f0e8c7fd0e3a473e2
}

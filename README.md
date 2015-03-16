[Build Status](https://hudson.eclipse.org/leshan/)

Leshan
======

Leshan is a OMA Lightweight M2M server and client implementation.

What is OMA LWM2M: 
http://technical.openmobilealliance.org/Technical/release_program/lightweightM2M_v1_0.aspx

The specification: 
http://member.openmobilealliance.org/ftp/Public_documents/DM/LightweightM2M/

Introduction to LWM2M:
http://fr.slideshare.net/zdshelby/oma-lightweightm2-mtutorial

Contact
-------

Join the project mailing list : [Subscribe](https://dev.eclipse.org/mailman/listinfo/leshan-dev)

[Archives](https://dev.eclipse.org/mhonarc/lists/leshan-dev/)

Mail address: leshan-dev@eclipse.org

Test Sandbox
------------

You can try it live on our server demo instance: http://leshan.eclipse.org/


Server
======

Get and run last binary
-----------------------

```
wget https://hudson.eclipse.org/leshan/job/leshan/lastSuccessfulBuild/artifact/leshan-standalone.jar
java -jar ./leshan-standalone.jar
```

Compile & Run
-------------

```
mvn install
cd leshan-standalone
mvn assembly:assembly -DdescriptorId=jar-with-dependencies
```

Run:

```
java -jar target/leshan-standalone-*-SNAPSHOT-jar-with-dependencies.jar 
```

Connect on Leshan UI: http://localhost:8080

Leshan provides a very simple UI to get the list of connected clients and interact with clients resources.

Now you can register your LWM2M client using:

[Eclipse Wakaama](http://eclipse.org/wakaama) or its lua binding [lualwm2m] (https://github.com/sbernard31/lualwm2m).

Leshan-client library or use the example LeshanDevice.  To build the LeshanDevice example, first compile the leshan-client:

```
mvn install
cd leshan-client-example
mvn assembly:assembly -DdescriptorId=jar-with-dependencies
```

Then run, setting the hostname and port of the server:

```
java -jar target/leshan-client-example-*-SNAPSHOT-jar-with-dependencies.jar localhost 5683
```

The list of the registered clients: http://localhost:8080/api/clients

![Leshan](https://raw.github.com/msangoi/leshan/master/leshan-capture.png)

Code with eclipse
-----------------
You need to add the M2_REPO to your java classpath variables. To do that you can execute the following command:

```
mvn -Declipse.workspace=<path-to-eclipse-workspace> eclipse:add-maven-repo
```
An eclipse restart is needed (if you already have it open).

You can also do that inside eclipse: From the menu bar, select Window > Preferences. Select the *Java > Build Path > Classpath Variables* page.

Now, you need to eclipsify leshan java projects,so run:

```
mvn eclipse:eclipse
```

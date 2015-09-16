[Build Status](https://hudson.eclipse.org/leshan/)

![Leshan](https://eclipse.org/leshan/img/multicolor-leshan.png)

[Leshan](https://eclipse.org/leshan) is a OMA Lightweight M2M server and client java implementation.

[What is OMA LWM2M ?](http://technical.openmobilealliance.org/Technical/release_program/lightweightM2M_v1_0.aspx)  
[The specification](http://member.openmobilealliance.org/ftp/Public_documents/DM/LightweightM2M/).  
[Introduction to LWM2M](http://fr.slideshare.net/zdshelby/oma-lightweightm2-mtutorial).  

Leshan provides libraries which help people to develop their own Lightweight M2M server and client.
The project also provides a Lightweight M2M standalone server as an example of the Leshan Server API and for testing purpose.

Contact
-------

Join the project mailing list : [subscribe](https://dev.eclipse.org/mailman/listinfo/leshan-dev).  
Mail address: leshan-dev@eclipse.org.  
Access to [leshan-dev archives](https://dev.eclipse.org/mhonarc/lists/leshan-dev/).  

Test Server Sandbox
------------

You can try live our server demo instance on: http://leshan.eclipse.org/  
(Automatic deployment of master branch)

Test Server locally
-----------------------
Get and run the last binary of our demo server :
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

Connect on Leshan demo UI: http://localhost:8080  
Leshan provides a very simple UI to get the list of connected clients and interact with clients resources.

Now you can register a LWM2M client.  
You could try the LeshanDevice example, the [Eclipse Wakaama](http://eclipse.org/wakaama) test client or script samples of its lua binding [lualwm2m] (https://github.com/sbernard31/lualwm2m).
 
To build the LeshanDevice example, first compile:
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

![Leshan](https://eclipse.org/leshan/img/capture_for_github.png)

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

Modules
-----------------
`Leshan-core` : commons elements.  
`Leshan-server-core` : server lwm2m logic.  
`Leshan-server-cf` : server implementation based on [californium](https://github.com/eclipse/californium).  
`Leshan-client-core` : client lwm2m logic.  
`Leshan-client-cf` : client implementation based on [californium](https://github.com/eclipse/californium).  
`Leshan-all` : every previous modules in 1 jar.  
`Leshan-client-example` : a sample of client API.  
`Leshan-standalone` : a demo server with a web UI.  
`Leshan-bs-server` : a bootstarp demo server.  
`Leshan-integration-tests` : integration automatic tests.  


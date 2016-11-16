[Build Status](https://hudson.eclipse.org/leshan/)

![Leshan](https://eclipse.org/leshan/img/multicolor-leshan.png)

[Eclipse Leshan™](https://eclipse.org/leshan) is an OMA Lightweight M2M server and client Java implementation.

[What is OMA LWM2M ?](http://technical.openmobilealliance.org/Technical/release_program/lightweightM2M_v1_0.aspx)  
[The specification](http://member.openmobilealliance.org/ftp/Public_documents/DM/LightweightM2M/).  
[Introduction to LWM2M](http://fr.slideshare.net/zdshelby/oma-lightweightm2-mtutorial).  

Leshan provides libraries which help people to develop their own Lightweight M2M server and client.  
The project also provides a client, a server and a bootstrap server demonstration as an example of the Leshan API and for testing purpose.

Contact
-------

Join the project mailing list : [subscribe](https://dev.eclipse.org/mailman/listinfo/leshan-dev).  
Mail address: leshan-dev@eclipse.org.  
Access to [leshan-dev archives](https://dev.eclipse.org/mhonarc/lists/leshan-dev/).  

Test Server Sandbox
------------

You can try live our servers demos instances:

* The **lwm2m server** at http://leshan.eclipse.org/  
   _(coap://leshan.eclipse.org:5683  and coaps://leshan.eclipse.org:5684)_  
* The **bootstrap server** at http://leshan.eclipse.org/bs/  
   _(coap://leshan.eclipse.org:5783  and coaps://leshan.eclipse.org:5784)_  

(Automatic deployment of master branch)

Test Leshan locally
-----------------------
Get and run the last binary of our demo **server** :
```
wget https://hudson.eclipse.org/leshan/job/leshan/lastSuccessfulBuild/artifact/leshan-server-demo.jar
java -jar ./leshan-server-demo.jar
```
Get and run the last binary of our demo **client** :
```
wget https://hudson.eclipse.org/leshan/job/leshan/lastSuccessfulBuild/artifact/leshan-client-demo.jar
java -jar ./leshan-client-demo.jar
```
Get and run the last binary of our **boostrap** demo server :
```
wget https://hudson.eclipse.org/leshan/job/leshan/lastSuccessfulBuild/artifact/leshan-bsserver-demo.jar
java -jar ./leshan-bsserver-demo.jar
```

Compile & Run
-------------

```
mvn clean install
```

Run demo **server**:
```
java -jar leshan-server-demo/target/leshan-server-demo-*-SNAPSHOT-jar-with-dependencies.jar 
```

Connect on Leshan demo UI: http://localhost:8080  
Leshan provides a very simple UI to get the list of connected clients and interact with clients resources.

Now you can register a LWM2M client by running our **client** demo:
```
java -jar leshan-client-demo/target/leshan-client-demo-*-SNAPSHOT-jar-with-dependencies.jar 
```
or trying the [Eclipse Wakaama](http://eclipse.org/wakaama) test client or script samples of its lua binding [lualwm2m] (https://github.com/sbernard31/lualwm2m).


You can also try our **bootstrap** demo server:
```
java -jar leshan-bsserver-demo/target/leshan-bsserver-demo-*-SNAPSHOT-jar-with-dependencies.jar 
```

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
`Leshan-client-demo` : a simple demo client.  
`Leshan-server-demo` : a lwm2m demo server with a web UI.  
`Leshan-bsserver-demo` : a bootstarp demo server with a web UI.  
`Leshan-integration-tests` : integration automatic tests.  


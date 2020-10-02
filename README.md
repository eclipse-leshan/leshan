![Leshan](https://eclipse.org/leshan/img/multicolor-leshan.png)

[Eclipse Leshan™](https://eclipse.org/leshan) is an OMA Lightweight M2M server and client Java implementation.

[What is OMA LWM2M ?](https://omaspecworks.org/what-is-oma-specworks/iot/lightweight-m2m-lwm2m/)  
[Object and Resource Registry](http://www.openmobilealliance.org/wp/OMNA/LwM2M/LwM2MRegistry.html).  

Leshan provides libraries which help people to develop their own Lightweight M2M server and client.  
The project also provides a client, a server and a bootstrap server demonstration as an example of the Leshan API and for testing purpose.

| LWM2M Version Targeted | Leshan Version | Development State |  Build Status	|  Standalone Demos |
| - | - | - | - | - |
| v1.0.2 <br/>[core specification(pdf)](http://openmobilealliance.org/release/LightweightM2M/V1_0_2-20180209-A/OMA-TS-LightweightM2M-V1_0_2-20180209-A.pdf) | [v1.x](https://github.com/eclipse/leshan/tree/1.x) <br/> [Supported features](https://github.com/eclipse/leshan/wiki/LWM2M-Supported-features)  | stable released | [jenkins-1.x](https://ci.eclipse.org/leshan/job/leshan-1.x/) | [server-demo](https://ci.eclipse.org/leshan/job/leshan-1.x/lastSuccessfulBuild/artifact/leshan-server-demo.jar)<br/> [client-demo](https://ci.eclipse.org/leshan/job/leshan-1.x/lastSuccessfulBuild/artifact/leshan-client-demo.jar) <br/> [bsserver-demo](https://ci.eclipse.org/leshan/job/leshan-1.x/lastSuccessfulBuild/artifact/leshan-bsserver-demo.jar) |
| **v1.1.1** <br/> [core specification(html)](http://www.openmobilealliance.org/release/LightweightM2M/V1_1_1-20190617-A/HTML-Version/OMA-TS-LightweightM2M_Core-V1_1_1-20190617-A.html) <br/> [transport bindings(html)](http://www.openmobilealliance.org/release/LightweightM2M/V1_1_1-20190617-A/HTML-Version/OMA-TS-LightweightM2M_Transport-V1_1_1-20190617-A.html)| [**v2.x** (master)](https://github.com/eclipse/leshan/tree/master) <br/> [Supported features](https://github.com/eclipse/leshan/wiki/LWM2M-1.1-supported-features) | **in development**  |[jenkins-master](https://ci.eclipse.org/leshan/job/leshan/)     | [server-demo](https://ci.eclipse.org/leshan/job/leshan/lastSuccessfulBuild/artifact/leshan-server-demo.jar)<br/> [client-demo](https://ci.eclipse.org/leshan/job/leshan/lastSuccessfulBuild/artifact/leshan-client-demo.jar) <br/> [bsserver-demo](https://ci.eclipse.org/leshan/job/leshan/lastSuccessfulBuild/artifact/leshan-bsserver-demo.jar)  |


Release (stable and milestones) are available on [maven central](https://search.maven.org/search?q=org.eclipse.leshan).  
Nightly build (snapshot) are available on [eclipse repo](https://repo.eclipse.org/#view-repositories;leshan-snapshots~browsestorage). ([more details](https://github.com/eclipse/leshan/pull/885))

The Leshan Documentation  is available in our [wiki :blue_book:](https://github.com/eclipse/leshan/wiki).

Contact
-------

Join the project mailing list : [subscribe](https://dev.eclipse.org/mailman/listinfo/leshan-dev).  
Mail address: leshan-dev@eclipse.org.  
Access to [leshan-dev archives](https://dev.eclipse.org/mhonarc/lists/leshan-dev/).  

Test Server Sandbox
------------

You can try live our servers demos instances:

* The **lwm2m server** at https://leshan.eclipseprojects.io/  
   _(coap://leshan.eclipseprojects.io:5683  and coaps://leshan.eclipseprojects.io:5684)_  
* The **bootstrap server** at https://leshan.eclipseprojects.io/bs/  
   _(coap://leshan.eclipseprojects.io:5783  and coaps://leshan.eclipseprojects.io:5784)_  

(Automatic deployment of master branch)

![Leshan](https://eclipse.org/leshan/img/capture_for_github.png)

Test Leshan Demos locally
-----------------------
Get and run the last binary of our demo **server** :
```
wget https://ci.eclipse.org/leshan/job/leshan/lastSuccessfulBuild/artifact/leshan-server-demo.jar
java -jar ./leshan-server-demo.jar
```
Get and run the last binary of our demo **client** :
```
wget https://ci.eclipse.org/leshan/job/leshan/lastSuccessfulBuild/artifact/leshan-client-demo.jar
java -jar ./leshan-client-demo.jar
```
Get and run the last binary of our **bootstrap** demo server :
```
wget https://ci.eclipse.org/leshan/job/leshan/lastSuccessfulBuild/artifact/leshan-bsserver-demo.jar
java -jar ./leshan-bsserver-demo.jar
```
:information_source: : _All the demos have a `--help` option._

Compile Leshan & Run Demos
-------------
Get sources :
```bash
#using ssh
git clone git@github.com:eclipse/leshan.git
```
or
```
#using https
git clone https://github.com/eclipse/leshan.git

```

Compile it, by running in leshan root folder :

```
mvn clean install
```

Run demo **server**:
```
java -jar leshan-server-demo/target/leshan-server-demo-*-SNAPSHOT-jar-with-dependencies.jar 
```

Connect on Leshan demo UI: http://localhost:8080  
Leshan server Demo provides a very simple UI to get the list of connected clients and interact with clients resources.

Now you can register a LWM2M client by running our **client** demo:
```
java -jar leshan-client-demo/target/leshan-client-demo-*-SNAPSHOT-jar-with-dependencies.jar 
```
or trying the [Eclipse Wakaama](http://eclipse.org/wakaama) test client.

You can also try our **bootstrap** demo server:
```
java -jar leshan-bsserver-demo/target/leshan-bsserver-demo-*-SNAPSHOT-jar-with-dependencies.jar 
```

Let's start to code !
---------------------
Now you played a bit with our demo, you should start to code your own server or client using our [Getting-started](https://github.com/eclipse/leshan/wiki/Getting-started) guide.

[Build Status](https://hudson.eclipse.org/leshan/)

![Leshan](https://eclipse.org/leshan/img/multicolor-leshan.png)

[Eclipse Leshan™](https://eclipse.org/leshan) is an OMA Lightweight M2M server and client Java implementation.

[What is OMA LWM2M ?](http://www.openmobilealliance.org/wp/overviews/lightweightm2m_overview.html)  
[The specification](http://openmobilealliance.org/release/LightweightM2M/V1_0_2-20180209-A/OMA-TS-LightweightM2M-V1_0_2-20180209-A.pdf).  
[Object and Resource Registry](http://www.openmobilealliance.org/wp/OMNA/LwM2M/LwM2MRegistry.html).  

Leshan provides libraries which help people to develop their own Lightweight M2M server and client.  
The project also provides a client, a server and a bootstrap server demonstration as an example of the Leshan API and for testing purpose.

[Here](https://github.com/eclipse/leshan/wiki/LWM2M-Supported-features) you can see which part of the specification is currently covered by Leshan.

The Leshan Documentation  is available in our [wiki :blue_book:](https://github.com/eclipse/leshan/wiki).

Contact
-------

Join the project mailing list : [subscribe](https://dev.eclipse.org/mailman/listinfo/leshan-dev).  
Mail address: leshan-dev@eclipse.org.  
Access to [leshan-dev archives](https://dev.eclipse.org/mhonarc/lists/leshan-dev/).  

Test Server Sandbox
------------

You can try live our servers demos instances:

* The **lwm2m server** at https://leshan.eclipse.org/  
   _(coap://leshan.eclipse.org:5683  and coaps://leshan.eclipse.org:5684)_  
* The **bootstrap server** at https://leshan.eclipse.org/bs/  
   _(coap://leshan.eclipse.org:5783  and coaps://leshan.eclipse.org:5784)_  

(Automatic deployment of master branch)

![Leshan](https://eclipse.org/leshan/img/capture_for_github.png)

Test Leshan Demos locally
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
Get and run the last binary of our **bootstrap** demo server :
```
wget https://hudson.eclipse.org/leshan/job/leshan/lastSuccessfulBuild/artifact/leshan-bsserver-demo.jar
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

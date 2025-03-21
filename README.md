<h1 align="center">

<a href="[https://otterdog.eclipse.org](https://eclipse.dev/leshan/)">
  <img src="https://eclipse.org/leshan/img/multicolor-leshan.png">
</a>
</h1>
<a href="https://scorecard.dev/viewer/?uri=github.com/eclipse-leshan/leshan"><img alt="OpenSSF Scorecard" src="https://api.securityscorecards.dev/projects/github.com/eclipse-leshan/leshan/badge" /></a>
<a href="https://www.bestpractices.dev/projects/10034"><img alt="OpenSSF Best Practices" src="https://www.bestpractices.dev/projects/10034/badge" /></a>
<a href="https://sonarcloud.io/summary/new_code?id=eclipse-leshan_leshan"><img alt="Quality Gate Status" src="https://sonarcloud.io/api/project_badges/measure?project=eclipse-leshan_leshan&metric=alert_status"></a>

  
</p>

[Eclipse Leshan™](https://eclipse.dev/leshan/) is an OMA Lightweight M2M server and client Java implementation.

[What is OMA LWM2M ?](https://omaspecworks.org/what-is-oma-specworks/iot/lightweight-m2m-lwm2m/)  
[LWM2M Specifications](https://github.com/eclipse/leshan/wiki/Lightweight-M2M-Specification).  

Leshan provides libraries which help people to develop their own Lightweight M2M server and client.  
The project also provides a client, a server and a bootstrap server demonstration as an example of the Leshan API and for testing purpose.

| LWM2M Version <br> Targeted | Leshan  <br> Version | Minimal <br> Java Version | Development <br> State |  Build Status   |  Standalone <br> Demos |
| - | - | - | - | - | - |
| [v1.0.x](https://github.com/eclipse/leshan/wiki/Lightweight-M2M-Specification#lightweight-m2m-v10x) | [v1.x](https://github.com/eclipse/leshan/tree/1.x) <br/> [Supported features](https://github.com/eclipse/leshan/wiki/LWM2M-Supported-features) | Java 7 ([more details](https://github.com/eclipse-leshan/leshan/tree/1.x/documentation/Requirement.md)) | stable released | [jenkins-1.x](https://ci.eclipse.org/leshan/job/leshan-ci/job/1.x/) | [server-demo](https://ci.eclipse.org/leshan/job/leshan-ci/job/1.x/lastSuccessfulBuild/artifact/leshan-demo-server.jar)<br/> [client-demo](https://ci.eclipse.org/leshan/job/leshan-ci/job/1.x/lastSuccessfulBuild/artifact/leshan-demo-client.jar) <br/> [bsserver-demo](https://ci.eclipse.org/leshan/job/leshan-ci/job/1.x/lastSuccessfulBuild/artifact/leshan-demo-bsserver.jar) |
| [**v1.1.x**](https://github.com/eclipse/leshan/wiki/Lightweight-M2M-Specification#lightweight-m2m-v11x)| [**v2.x** (master)](https://github.com/eclipse/leshan/tree/master) <br/> [Supported features](https://github.com/eclipse/leshan/wiki/LWM2M-1.1-supported-features) | Java 8 ([more details](./documentation/Requirement.md)) | **in development**  |[jenkins-master](https://ci.eclipse.org/leshan/job/leshan-ci/job/master/)     | [server-demo](https://ci.eclipse.org/leshan/job/leshan-ci/job/master/lastSuccessfulBuild/artifact/leshan-demo-server.jar)<br/> [client-demo](https://ci.eclipse.org/leshan/job/leshan-ci/job/master/lastSuccessfulBuild/artifact/leshan-demo-client.jar) <br/> [bsserver-demo](https://ci.eclipse.org/leshan/job/leshan-ci/job/master/lastSuccessfulBuild/artifact/leshan-demo-bsserver.jar)  |


Release (stable and milestones) are available on [maven central](https://search.maven.org/search?q=org.eclipse.leshan).  
Nightly build (snapshot) are available on [eclipse repo](https://repo.eclipse.org/#view-repositories;leshan-snapshots~browsestorage). ([more details](https://github.com/eclipse/leshan/pull/885))

The Leshan Documentation  is available in our [wiki :blue_book:](https://github.com/eclipse/leshan/wiki).

Contact
-------
If you have any **questions**, **feedback** or **bugs** to report, please use [github issue](https://github.com/eclipse-leshan/leshan/issues).  
For **vulnerabilities**, have a look at our [Security Policy](https://github.com/eclipse-leshan/leshan/security/policy).  
If you want to **contribute**, take a look at our [Contribution Guide](https://github.com/eclipse-leshan/leshan/blob/master/CONTRIBUTING.md).


We also have a mail list but it is not so much used :
Join the project mailing list : [subscribe](https://dev.eclipse.org/mailman/listinfo/leshan-dev).  
Mail address: leshan-dev@eclipse.org.  
Access to [leshan-dev archives](https://dev.eclipse.org/mhonarc/lists/leshan-dev/).  

License
-------

This work is dual-licensed under the Eclipse Public License v2.0 and Eclipse Distribution License v1.0

`SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause`

Test Server Sandbox
------------

You can try live our servers demos instances:

* The **lwm2m server** at https://leshan.eclipseprojects.io/  
   _(coap://leshan.eclipseprojects.io:5683  and coaps://leshan.eclipseprojects.io:5684)_  
* The **bootstrap server** at https://leshan.eclipseprojects.io/bs/  
   _(coap://leshan.eclipseprojects.io:5783  and coaps://leshan.eclipseprojects.io:5784)_  

(Automatic deployment of master branch)

![Leshan](https://www.eclipse.org/leshan/img/capture_for_github-v2.png)

Test Leshan Demos locally
-----------------------
Get and run the last binary of our demo **server** :
```
wget https://ci.eclipse.org/leshan/job/leshan-ci/job/master/lastSuccessfulBuild/artifact/leshan-demo-server.jar
java -jar ./leshan-demo-server.jar
```
Get and run the last binary of our demo **client** :
```
wget https://ci.eclipse.org/leshan/job/leshan-ci/job/master/lastSuccessfulBuild/artifact/leshan-demo-client.jar
java -jar ./leshan-demo-client.jar
```
Get and run the last binary of our **bootstrap** demo server :
```
wget https://ci.eclipse.org/leshan/job/leshan-ci/job/master/lastSuccessfulBuild/artifact/leshan-demo-bsserver.jar
java -jar ./leshan-demo-bsserver.jar
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
```bash
#using https
git clone https://github.com/eclipse/leshan.git

```

Compile it, by running in leshan root folder :

```
mvn clean install
```

Run demo **server**:
```
java -jar leshan-demo-server/target/leshan-demo-server-*-SNAPSHOT-jar-with-dependencies.jar 
```

Connect on Leshan demo UI: http://localhost:8080  
Leshan server Demo provides a very simple UI to get the list of connected clients and interact with clients resources.

Now you can register a LWM2M client by running our **client** demo:
```
java -jar leshan-demo-client/target/leshan-demo-client-*-SNAPSHOT-jar-with-dependencies.jar 
```
or trying the [Eclipse Wakaama](http://eclipse.org/wakaama) test client.

You can also try our **bootstrap** demo server:
```
java -jar leshan-demo-bsserver/target/leshan-demo-bsserver-*-SNAPSHOT-jar-with-dependencies.jar 
```

Let's start to code !
---------------------
Now you played a bit with our demo, you should start to code your own server or client using our [Getting-started](https://github.com/eclipse/leshan/wiki/Getting-started) guide.

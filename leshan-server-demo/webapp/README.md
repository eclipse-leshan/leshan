# webapp

The **leshan-server-demo** webapp is based on [Vue.js](https://vuejs.org/).  
To be able to enjoy the "single file component" feature we need to use a modern module build system for JavaScript.  
Here we are using [Yarn](https://yarnpkg.com/) and [Babel](https://babeljs.io/) tooling. 

The JavaScript build is integrated in Maven thanks to [frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin) and so there is nothing special to do at build time. The classic `mvn clean install` works.

But for the **development workflow this is a bit more complicated.**

## Development workflow

### Tooling & IDE
You will **need to** install **[Yarn](https://yarnpkg.com)** to have a good development experience. (/!\ in Debian there is [binary name issue](https://bugs.debian.org/cgi-bin/bugreport.cgi?bug=940511#34).)  
Without it, the alternative would be to Maven build each time you want to test your code which is a very bad idea...

About IDE, Even if you should be able to use your preferred Java IDE, actually Leshan is mainly developed with Eclipse IDE.
Unfortunately there isn't good open-source plug-in to code with Vue.js in Eclipse. ([The best hope is sticked in License issue](https://github.com/eclipse/wildwebdeveloper/issues/83))  
So we advice to use [vscodium](https://vscodium.com/) which has a good Vue.js support.
 
At browser side, you can **eventually** use [vue-devtools plug-in](https://github.com/vuejs/vue-devtools).

### Workflow development 
Before to be able to develop you need to install all webapp's dependencies :
 
```
yarn install
```
Now you need to launch the `LeshanServerDemo` application with your java IDE.
Then launch :

```
yarn serve
```
This will start a dev server (based on webpack-dev-server ) that comes with Hot-Module-Replacement (HMR) working out of the box. Meaning now each time you modify a fronted webapp file, the modification will be automatically applied. So just edit your files and see the changes in your browser.  
_Note that :_ webapp is available on a different pot 8088 instead of default 8080. (see vue.config.js)

## To go further

### Compiles and minifies for production

This doesn't make to much sense to do that as this is done automatically by Maven build but you can eventually build the frontend webapp only like this :  

```
yarn build
```

### Lints and fixes files

This is generally automatically do by your IDE (like Vscodium) but you can launch it manually like this : 

```
yarn lint
```

### Customize configuration
See [Configuration Reference](https://cli.vuejs.org/config/).

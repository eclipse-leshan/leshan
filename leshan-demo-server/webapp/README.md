# webapp

The **leshan-demo-server** webapp is based on [Vue 2](https://v2.vuejs.org/).  
To be able to enjoy the "single file component" feature we need to use a modern module build system for JavaScript.  
Here we are using [npm](https://nodejs.org/en/learn/getting-started/an-introduction-to-the-npm-package-manager) and [Vite](https://vitejs.dev/) tooling. 

The JavaScript build is integrated in Maven thanks to [frontend-maven-plugin](https://github.com/eirslett/frontend-maven-plugin) and so there is nothing special to do at build time. The classic `mvn clean install` works.

But for the **development workflow this is a bit more complicated.**

## Development workflow

### Tooling & IDE
You will **need to** install **[nodejs](https://nodejs.org/en)** to have a good development experience.
Without it, the alternative would be to Maven build each time you want to test your code which is a very bad idea...

About IDE, Even if you should be able to use your preferred Java IDE, actually Leshan is mainly developed with Eclipse IDE.
Unfortunately there isn't good open-source plug-in to code with Vue.js in Eclipse. ([The best hope is sticked in License issue](https://github.com/eclipse/wildwebdeveloper/issues/83))  
So we advice to use [vscodium](https://vscodium.com/) which has a good Vue.js support.
 
At browser side, you can **eventually** use [vue-devtools plug-in](https://github.com/vuejs/vue-devtools).

### Workflow development 
Before to be able to develop you need to install all webapp's dependencies :
 
```
npm install
```
Now you need to launch the `LeshanServerDemo` application with your java IDE.
Then launch :

```
npm run serve
```
This will start a dev serve that comes with Hot-Module-Replacement (HMR) working out of the box. Meaning now each time you modify a fronted webapp file, the modification will be automatically applied. So just edit your files and see the changes in your browser.  
_Note that :_ webapp is available on a different pot 8088 instead of default 8080. (see vite.config.js)

## To go further

### Compiles and minifies for production

This doesn't make to much sense to do that as this is done automatically by Maven build but you can eventually build the frontend webapp only like this :  

```
npm run build
```

Once it is built you can preview it on port 8088 with : 
```
npm run preview
```

### Lints and fixes files

This is generally automatically do by your IDE (like vscodium) but you can launch it manually like this : 

```
npm run lint
```

### Common maintenance tasks

#### Update direct dependencies

Regularly we need to update direct dependencies, this can be done with : 

```
npm outdated
```
(we should test https://www.npmjs.com/package/npm-check-updates)

#### Update indirect depndencies

Sometime it is needed to upgrade indirect dependencies especially for security issues. (e.g. [dependabot alert](https://github.com/eclipse/leshan/security/dependabot)) 

We don't do that since we move from `yarn` to `npm`, so we don't know how to do for now. (documentation should be updated when we will do it)

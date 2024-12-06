
// Plugins
import Components from 'unplugin-vue-components/vite'
import Vue from '@vitejs/plugin-vue'
import legacy from '@vitejs/plugin-legacy'
import Vuetify, { transformAssetUrls } from 'vite-plugin-vuetify'
import ViteFonts from 'unplugin-fonts/vite'
import VueRouter from 'unplugin-vue-router/vite'

// Utilities
import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'

import { nodeResolve } from "@rollup/plugin-node-resolve";
import browserslistToEsbuild from "browserslist-to-esbuild";
import viteCompression from "vite-plugin-compression";
import { visualizer } from "rollup-plugin-visualizer";

const outputDir = process.env.MAVEN_OUTPUT_DIR
  ? process.env.MAVEN_OUTPUT_DIR
  : "../target/dist";

// https://vitejs.dev/config/
export default defineConfig({
  //base: "./", // should be deleted ?
  plugins: [
    VueRouter(),
    Vue({
      template: { transformAssetUrls }
    }),
    // https://github.com/vuetifyjs/vuetify-loader/tree/master/packages/vite-plugin#readme
    Vuetify({
      autoImport: true,
    }),
    Components(),
    ViteFonts({
      google: {
        families: [{
          name: 'Roboto',
          styles: 'wght@100;300;400;500;700;900',
        }],
      },
    }),
    legacy({ targets: ['defaults', 'not IE 11'] }),
    nodeResolve({
      modulePaths: [fileURLToPath(new URL('./node_modules', import.meta.url))],
    }),
    !process.env.REPORT ? viteCompression() : null,
    process.env.REPORT
      ? visualizer({
        template: "treemap", //"sunburst",
        filename: outputDir + "/stats.html",
        open: true,
        gzipSize: true,
      })
      : null,
  ],
  build: {
    outDir: outputDir,
    target: browserslistToEsbuild(),
  },
  preview: {
    port: 8088,
  },
  server: {
    port: 8088,
    proxy: {
      "/api": {
        // 8080 is the default port for leshan-demo-server if you change it in development phase you need to change this value too.
        target: "http://localhost:8080",
        ws: true,
        changeOrigin: true,
      },
    },
    // // Workarround : https://stackoverflow.com/questions/71783075/sse-doent-work-with-vue-cli-devserver-proxy
    // compress: false,
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL('./src', import.meta.url)),
      "@leshan-demo-servers-shared": fileURLToPath(new URL('../../leshan-demo-servers-shared/webapp/src', import.meta.url)),
    },
    extensions: [".js", ".vue"],
  },
});

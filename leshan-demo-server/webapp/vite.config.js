// Plugins
import Components from 'unplugin-vue-components/vite'
import Vue from '@vitejs/plugin-vue'
import Vuetify, { transformAssetUrls } from 'vite-plugin-vuetify'
import webfontDownload from 'vite-plugin-webfont-dl';
import VueRouter from 'unplugin-vue-router/vite'


// Utilities
import { defineConfig } from 'vite'
import { fileURLToPath, URL } from 'node:url'

import { nodeResolve } from "@rollup/plugin-node-resolve";
import viteCompression from "vite-plugin-compression";
import { visualizer } from "rollup-plugin-visualizer";
import csp from "vite-plugin-csp-guard";


const outputDir = process.env.MAVEN_OUTPUT_DIR
  ? process.env.MAVEN_OUTPUT_DIR
  : "../target/dist";

// https://vitejs.dev/config/
export default defineConfig({
  base: "./",
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
    csp({
      dev: {
        run: false, outlierSupport: ["vue"]
      },
      policy: {
        "font-src": ["'self' data:"],
        "img-src": ["'self'", "data:"],
        // We can not remove unsafe-inline because of vuetify
        // See :
        // - https://github.com/vuetifyjs/vuetify/issues/15973
        // - https://github.com/eclipse-leshan/leshan/issues/1682
        "style-src-elem": ["'self'", "'unsafe-inline'"],
      }
    }),
    webfontDownload(
      [
        'https://fonts.googleapis.com/css2?family=Roboto:wght@100;300;400;500;700;900&display=swap',
      ], {
      async: false,
      injectAsStyleTag: false,
    }),
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

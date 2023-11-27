import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue2";
import { nodeResolve } from "@rollup/plugin-node-resolve";
import { VuetifyResolver } from "unplugin-vue-components/resolvers";
import Components from "unplugin-vue-components/vite";
import browserslistToEsbuild from "browserslist-to-esbuild";
import viteCompression from "vite-plugin-compression";
import { visualizer } from "rollup-plugin-visualizer";

import path from "path";

const outputDir = process.env.MAVEN_OUTPUT_DIR
  ? process.env.MAVEN_OUTPUT_DIR
  : "../target/dist";

// https://vitejs.dev/config/
export default defineConfig({
  base: "./",
  plugins: [
    vue(),
    Components({
      resolvers: [
        // Vuetify
        VuetifyResolver(),
      ],
      // Vue version of project.
      version: 2.7,
    }),
    nodeResolve({
      modulePaths: [path.resolve("./node_modules")],
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
        // 8080 is the default port for leshan-server-demo if you change it in development phase you need to change this value too.
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
      "@": path.resolve(__dirname, "./src"),
      "@leshan-server-core-demo": path.join(
        __dirname,
        "../../leshan-server-core-demo/webapp/src"
      ),
    },
    extensions: [".js", ".vue"],
  },
});

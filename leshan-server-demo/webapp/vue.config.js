const path = require("path");

module.exports = {
  // https://cli.vuejs.org/config/
  outputDir: process.env.MAVEN_OUTPUT_DIR
    ? process.env.MAVEN_OUTPUT_DIR
    : "../target/dist",
  publicPath: "./",
  transpileDependencies: ["vuetify"],
  devServer: {
    proxy: {
      "/api": {
        // 8080 is the default port for leshan-server-demo if you change it in development phase you need to change this value too.
        target: "http://localhost:8080",
        ws: true,
        changeOrigin: true,
      },
    },
    // Workarround : https://stackoverflow.com/questions/71783075/sse-doent-work-with-vue-cli-devserver-proxy
    compress: false,
  },
  configureWebpack: {
    resolve: {
      alias: {
        "@leshan-server-core-demo": path.join(
          __dirname,
          "../../leshan-server-core-demo/webapp/src"
        ),
      },
      extensions: [".js", ".vue"],
    },
  },
};

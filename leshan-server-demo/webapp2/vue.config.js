module.exports = {
  // https://cli.vuejs.org/config/
  outputDir: process.env.MAVEN_OUTPUT_DIR
    ? process.env.MAVEN_OUTPUT_DIR
    : "../target/dist",
  publicPath: "/v2/",
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
  },
};

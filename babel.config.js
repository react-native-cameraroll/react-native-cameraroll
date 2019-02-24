module.exports = {
  presets: ["module:metro-react-native-babel-preset"],
  plugins: [
    [
      "module-resolver",
      {
        alias: {
          "@react-native-community/cameraroll": "./js/CameraRoll.js"
        },
        cwd: "babelrc"
      }
    ]
  ]
};

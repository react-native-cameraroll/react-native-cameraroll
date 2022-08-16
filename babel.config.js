module.exports = {
  presets: ['module:metro-react-native-babel-preset'],
  plugins: [
    [
      'module-resolver',
      {
        alias: {
          '@react-native-community/cameraroll': './src/index.ts'
        },
        cwd: 'babelrc'
      }
    ]
  ]
};

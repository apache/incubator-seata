const path = require('path');
const { merge } = require('lodash');
const baseConfig = require('./webpack.config.base');

const mode = 'development';

const entry = {
  bundle: path.resolve(__dirname, '..', 'src/index.js'),
};

const alias = {
  'gg-editor': path.resolve(__dirname, '..', 'ggeditor'),
};

const externals = {
  'react-dom': {
    root: 'ReactDOM',
    commonjs2: 'react-dom',
    commonjs: 'react-dom',
    amd: 'react-dom',
  },
  'react-router-dom': {
    root: 'ReactRouterDOM',
    commonjs: 'react-router-dom',
    commonjs2: 'react-router-dom',
    amd: 'react-router-dom',
  },
  antd: {
    root: 'antd',
    commonjs: 'antd',
    commonjs2: 'antd',
    amd: 'antd',
  },
};

const devtool = 'cheap-module-eval-source-map';

const devServer = {
  contentBase: path.resolve(__dirname, '..', ''),
  publicPath: '/dist',
  disableHostCheck: true,
};

const output = {
  path: path.resolve(__dirname, '..', 'dist'),
  filename: '[name].js',
  libraryTarget: 'umd',
};

module.exports = merge(baseConfig, {
  mode,
  entry,
  resolve: {
    alias,
  },
  externals,
  devtool,
  devServer,
  output,
});

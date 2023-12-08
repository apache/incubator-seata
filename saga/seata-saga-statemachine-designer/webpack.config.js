const CopyPlugin = require('copy-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');

module.exports = (env, options) => {
  return {
    entry: {
      bundle: './src/index.js',
    },
    output: {
      path: `${__dirname}/dist`,
      filename: '[name].js',
    },
    plugins: [
      new CopyPlugin({
        patterns: [
          { from: 'public' },
        ],
      }),
      new MiniCssExtractPlugin(),
      new HtmlWebpackPlugin({
        template: 'src/index.html',
        filename: 'index.html',
      }),
    ],
    module: {
      rules: [
        {
          test: /\.m?js$/,
          exclude: /(node_modules|bower_components)/,
          use: {
            loader: 'babel-loader',
            options: {
              babelrc: true,
            },
          },
        },
        {
          test: /\.css$/,
          use: [MiniCssExtractPlugin.loader, 'css-loader'],
        },
      ],
    },
    devtool: options.mode === 'production' ? 'nosources-source-map' : 'source-map',
  };
};

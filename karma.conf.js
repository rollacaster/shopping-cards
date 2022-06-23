module.exports = function (config) {
  config.set({
    browsers: ['ChromeHeadless'],
    basePath: 'target/test/ci',
    files: ['karma.js', 'mockServiceWorker.js'],
    frameworks: ['cljs-test'],
    plugins: ['karma-cljs-test', 'karma-chrome-launcher'],
    colors: true,
    logLevel: config.LOG_INFO,
    client: {
      args: ['shadow.test.karma.init'],
      singleRun: true,
    },
    customContextFile: 'index.html',
    proxies: {
      '/mockServiceWorker.js': '/base/mockServiceWorker.js',
    },
  })
}

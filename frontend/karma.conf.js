// Karma configuration used by `ng test` (see angular.json "karmaConfig").
// Locally: interactive Chrome. CI: `npm run test:ci` runs ChromeHeadlessCI once and writes
// JUnit results (reports/junit) and coverage (coverage/frontend) for the CI report job.
module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('karma-junit-reporter'),
    ],
    client: {
      jasmine: { random: true },
      clearContext: false,
    },
    jasmineHtmlReporter: { suppressAll: true },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/frontend'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'lcovonly' },
        { type: 'cobertura', file: 'cobertura-coverage.xml' },
        { type: 'text-summary' },
      ],
    },
    junitReporter: {
      outputDir: require('path').join(__dirname, './reports/junit'),
      outputFile: 'TEST-frontend.xml',
      useBrowserName: false,
      suite: 'frontend',
    },
    reporters: ['progress', 'kjhtml', 'junit'],
    browsers: ['Chrome'],
    customLaunchers: {
      // Containers and CI runners often run as root, where Chrome requires --no-sandbox.
      ChromeHeadlessCI: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage'],
      },
    },
    restartOnFileChange: true,
  });
};

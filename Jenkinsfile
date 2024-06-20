#!groovy

@Library("saas-build-pipeline@DM-7101-elk-managed-pipeline") _

jdpPipeline(
  type: 'gradle',
  appName: 'plat-jdp-lifecycle-notification-service',
  tasks: ['clean', 'build','sonarqube'],
  appDomain: 'JDP',
  configProject: 'JDP-SAASOPS',
  stagingConfigRepo: 'plat-jdp-test',
  buildAndTest: true,
  buildInDocker: true,
  deployToSandbox: true,
  qualityGate: true,
  postmanTest: true,
  checkmarx: true,
  checkmarxAllBranches: false,
  blackduck: true,
  blackduckAllBranches: false,
  pushPreview: true,
  pushRelease: true,
  promoteToStaging: true,
  cleanUpDev: true
)

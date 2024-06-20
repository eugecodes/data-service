#!/bin/bash

# This script will execute the functional scripts that come with the code.
# When this script runs as part of the build, it needs the artifactory username and pwd and that's why they are requested
# as input parameters. For local runs there is no need to pass in these.
#
# The client credentials for the sbx and dev environment are setup already and will be part of a setup script for the environment.
# These credentials are for a tenant that has been created specifically for testing, but they can be used by anyone else.
#
# Usage:
#   $1: environment to run against: sbx or dev
#   $2: artifactory username
#   $3: artifactory password
#   $4: jaas config for event hub
#   $5: automation user password
#   $6: for the client secret id

target=$1
username=$2
password=$3
jaas_config=$4
automation_pwd=$5
client_secret=$6

if [ -z "$target" ] || [ "$target" != "sandbox" ] && [ "$target" != "dev" ] && [ "$target" != "test" ] && [ "$target" != "staging" ]; then
  echo "Environment should be: sandbox or dev (development) or test or tst-eus2-01(staging)"
  exit 3
fi

if [ "$target" = "sandbox" ]; then
  test_suite="smoke"
  env_url="-Dautomation.jdp.url=https://jdpjdasbx01-api.jdadelivers.com/ \
  -Dbootstrap.servers=evhn-jdp-jda-sbx-01.servicebus.windows.net:9093 \
  -Dsasl.jaas.config='${jaas_config}' \
  -Dautomation.jdp.m2m.APIM.url=https://api-tst.jdadelivers.com/ \
  -Dautomation.jdp.m2m.clientId=86b8b9d1-403c-43d0-97f1-b4bb339d7ec3 \
  -Dautomation.jdp.m2m.clientSecret='${client_secret}' \
  -Dautomation.jdp.m2m.scope=https://blueyonderinteroptestus.onmicrosoft.com/6ad21424-b4b2-462e-80df-252ec22dd6a9/.default \
  -Dautomation.jdp.realmId=a1b6b9a3-130f-430a-8aba-60b5c45a862f \
  -Dautomation.jdp.m2m.LIAM_token.url=https://login.microsoftonline.com/blueyonderinteroptestus.onmicrosoft.com/oauth2/v2.0/token"
  env_name="Sandbox"
elif [ "$target" = "dev" ]; then
  test_suite="acceptance"
  env_url="-Dautomation.jdp.url=https://jdpbydev01-api.jdadelivers.com/ \
  -Dbootstrap.servers=evhn-jdp-by-dev-01.servicebus.windows.net:9093 \
  -Dsasl.jaas.config='${jaas_config}' \
  -Dautomation.jdp.m2m.APIM.url=https://api-tst.jdadelivers.com/ \
  -Dautomation.jdp.m2m.clientId=c768f2ae-eb5d-4ef2-9cf7-8643a6f7f274 \
  -Dautomation.jdp.m2m.clientSecret='${client_secret}' \
  -Dautomation.jdp.m2m.scope=https://blueyonderinteroptestus.onmicrosoft.com/6ad21424-b4b2-462e-80df-252ec22dd6a9/.default \
  -Dautomation.jdp.realmId=d359dd75-ec7c-4035-ab3b-f370946a5e4a \
  -Dautomation.jdp.m2m.LIAM_token.url=https://login.microsoftonline.com/blueyonderinteroptestus.onmicrosoft.com/oauth2/v2.0/token"
  env_name="Dev"
elif [ "$target" = "test" ]; then
  test_suite="regression"
  env_url="-Dautomation.jdp.url=https://jdpbytsteus202-api.jdadelivers.com/ \
  -Dbootstrap.servers=evhn-jdp-by-qa-eus2-01.servicebus.windows.net:9093 \
  -Dsasl.jaas.config='${jaas_config}' \
  -Dautomation.jdp.m2m.APIM.url=https://api-tst.jdadelivers.com/ \
  -Dautomation.jdp.m2m.clientId=11e8b3d6-baac-45f9-85cb-6ff7ac8cec0f \
  -Dautomation.jdp.m2m.clientSecret=${client_secret} \
  -Dautomation.jdp.m2m.scope=https://blueyonderinteroptestus.onmicrosoft.com/6ad21424-b4b2-462e-80df-252ec22dd6a9/.default \
  -Dautomation.jdp.realmId=d15e0b43-1244-406c-9f08-21a9e2baf0df \
  -Dautomation.jdp.m2m.LIAM_token.url=https://login.microsoftonline.com/blueyonderinteroptestus.onmicrosoft.com/oauth2/v2.0/token"
  env_name="test"
elif [ "$target" = "staging" ]; then
  test_suite="regression"
  env_url="-Dautomation.jdp.url=https://jdpbytsteus201-api.jdadelivers.com/ \
  -Dbootstrap.servers=evhn-jdp-by-tst-eus2-01.servicebus.windows.net:9093 \
  -Dsasl.jaas.config='${jaas_config}' \
  -Dautomation.jdp.m2m.APIM.url=https://api-tst.jdadelivers.com/ \
  -Dautomation.jdp.m2m.clientId=64bc8411-0c40-4b4e-9a2e-1a912a75d053 \
  -Dautomation.jdp.m2m.clientSecret=${client_secret} \
  -Dautomation.jdp.m2m.scope=https://blueyonderinteroptestus.onmicrosoft.com/6ad21424-b4b2-462e-80df-252ec22dd6a9/.default \
  -Dautomation.jdp.realmId=5c335b18-b076-4c1b-bbe1-3aefe66a01ba \
  -Dautomation.jdp.m2m.LIAM_token.url=https://login.microsoftonline.com/blueyonderinteroptestus.onmicrosoft.com/oauth2/v2.0/token"
  env_name="staging"
fi

gradle_tasks="clean functionalTest"
base_gradle_parameters="-Dgroup=$test_suite -Dautomation.jdp.credentials='${automation_pwd}' -Dautomation.jdp.ingestion.status.retry=3"

if [ $# -ne 6 ]; then
  gradle_parameters="$base_gradle_parameters"
else
  gradle_parameters="-PartifactoryUsername=$username -PartifactoryPassword=$password $base_gradle_parameters"
fi

echo "Running functional tests in ${env_name} environment ..."
echo "gradlew $gradle_tasks $gradle_parameters ${env_url}"
echo "${gradle_tasks} ${gradle_parameters} ${env_url}" | xargs ./gradlew

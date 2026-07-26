#!/usr/bin/env sh
set -eu

project_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$project_root"

./mvnw -B -ntp clean verify

set -- target/jdoor-assist-*-all.jar
if [ "$#" -ne 1 ] || [ ! -f "$1" ]; then
  echo "Expected exactly one shaded JAR." >&2
  exit 1
fi

jar_path=$1
jar_name=$(basename "$jar_path")
app_version=${jar_name#jdoor-assist-}
app_version=${app_version%-all.jar}
case "$app_version" in
  ''|*[!0-9.]*)
    echo "Cannot derive a jpackage-compatible version from $jar_name." >&2
    exit 1
    ;;
esac
mkdir -p target/jpackage-input target/package
cp "$jar_path" "target/jpackage-input/$jar_name"

jpackage \
  --type app-image \
  --name "JDoor Assist" \
  --description "Consent-first encrypted remote assistance" \
  --vendor "Djenis Ejupi and contributors" \
  --app-version "$app_version" \
  --dest target/package \
  --input target/jpackage-input \
  --main-jar "$jar_name" \
  --main-class com.jdoor.JDoorApplication

echo "Created app image below target/package/."

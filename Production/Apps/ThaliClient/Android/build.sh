#!/bin/bash

# directory containing this script
PROJECT_DIR=$(cd $(dirname $0) ; pwd)

EXTENSION_SRC=$PROJECT_DIR/src
APP_SRC=$PROJECT_DIR/../web

# build the extension
echo
echo "********* BUILDING EXTENSION..."
echo
cd $EXTENSION_SRC
ant

# location of Crosswalk Android (downloaded during extension build)
XWALK_DIR=$EXTENSION_SRC/lib/`ls lib/ | grep 'crosswalk-'`

# build the apks
echo
echo "********* BUILDING ANDROID APK FILES..."
cd $XWALK_DIR
python make_apk.py --enable-remote-debugging --manifest=$APP_SRC/manifest.json --extensions=$EXTENSION_SRC/thaliclient-extension/

# back to where we started
cd $PROJECT_DIR

# show the location of the output apk files
echo
echo "********* APK FILES GENERATED:"
APKS=`ls $XWALK_DIR/thali_client*.apk`
for apk in $APKS ; do
  echo $apk
done
echo

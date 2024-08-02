#!/bin/bash

BASE_PATH=$(pwd)
README_PATH="${BASE_PATH}/README.md"
CHANGE_TOOL_PATH="/opt/android_change_package"
BUILD_DEMO="-BUILD_DEMO"

############################################################################################################################
# 处理 main/java/com/sigmob/sdk/base/common/Constants.java IS_TEST 字段
############################################################################################################################
if [ "$1" == "framework" ]; then
  IS_TEST=false
  BUILD_DEMO=""
fi

IS_TEST_LINE_NUM=$(grep -n "IS_TEST" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java | awk -F: '{print $1}')

sed -i '' "${IS_TEST_LINE_NUM}d" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java

if [ "${IS_TEST}" == "false" ]; then
  INSERT_CODE="\    public static final Boolean IS_TEST = false;"
else
  INSERT_CODE="\    public static final Boolean IS_TEST = true;"
fi

sed -i "" "${IS_TEST_LINE_NUM} a\ 
${INSERT_CODE}
" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java

grep "IS_TEST" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java
############################################################################################################################
# 处理 main/java/com/sigmob/sdk/base/common/Constants.java SDK_FOLDER 字段
############################################################################################################################
if [ "${NEED_NEW_PACKAGENAME}" == "true" ]; then
  SDK_FOLDER_LINE_NUM=$(grep -n "SDK_FOLDER" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java | awk -F: '{print $1}')

  sed -i '' "${SDK_FOLDER_LINE_NUM}d" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java

  INSERT_CODE="\    public static final String SDK_FOLDER = \"${NEW_PACKAGENAME}\";"

  sed -i "" "${SDK_FOLDER_LINE_NUM} a\ 
${INSERT_CODE}
" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java

  grep "SDK_FOLDER" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java
fi

############################################################################################################################
# 处理 main/java/com/sigmob/sdk/base/common/Constants.java GOOGLE_PLAY 字段
############################################################################################################################

GOOGLE_PLAY_LINE_NUM=$(grep -n "GOOGLE_PLAY" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java | awk -F: '{print $1}')

sed -i '' "${GOOGLE_PLAY_LINE_NUM}d" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java

if [ "${IS_GOOGLE_PLAY}" == "false" ]; then
  INSERT_CODE="\    public static final boolean GOOGLE_PLAY = false;"
  mv ${BASE_PATH}/build-CN.gradle ${BASE_PATH}/app/build.gradle
else
  INSERT_CODE="\    public static final boolean GOOGLE_PLAY = true;"
  mv ${BASE_PATH}/build-GP.gradle ${BASE_PATH}/app/build.gradle
fi

sed -i "" "${GOOGLE_PLAY_LINE_NUM} a\ 
${INSERT_CODE}
" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java

grep "GOOGLE_PLAY" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java
############################################################################################################################
# Gradle Build
############################################################################################################################

rm -rf ${BASE_PATH}/sigmob-sdk/src/main/AndroidManifest.xml
if [ "${IS_GOOGLE_PLAY}" == "true" ]; then
  mv ${BASE_PATH}/AndroidManifest.xml.gp ${BASE_PATH}/sigmob-sdk/src/main/AndroidManifest.xml
else
  mv ${BASE_PATH}/AndroidManifest.xml.cn ${BASE_PATH}/sigmob-sdk/src/main/AndroidManifest.xml
fi

mv -f ${BASE_PATH}/gradle.properties.1 ${BASE_PATH}/gradle.properties

${BASE_PATH}/gradlew clean

BUILD_VERSION=$(grep "SDK_VERSION" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java | awk '{match($NF,/\"(.+)\";/,m);print m[1];}')

mv ${BASE_PATH}/sigmob-sdk/src/main/java/com/sigmob/sdk/SigmobFileProvider.java.bak ${BASE_PATH}/sigmob-sdk/src/main/java/com/sigmob/sdk/SigmobFileProvider.java

if [ $? != 0 ]; then
  echo "mv ${BASE_PATH}/sigmob-sdk/src/main/java/com/sigmob/sdk/SigmobFileProvider.java error!"
  exit -1
fi

function build_ota_plist_android() {
  APK_URL="http://sigci.happyelements.net/ciservice/projects/${CI_DEMO_NAME}/${VERSION}/${CI_DEMO_NAME}.apk"
  if [ "${IS_TEST}" == "true" ]; then
    APK_URL="http://sigci.happyelements.net/ciservice/projects/${CI_DEMO_NAME}/${VERSION}/${CI_DEMO_NAME}-test.apk"
  fi
  echo "Generating project.plist"
  cat <<EOF >/opt/php/ciservice/web/projects/${CI_DEMO_NAME}/project.plist
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>items</key>
  <array>
    <dict>
      <key>assets</key>
      <array>
        <dict>
          <key>kind</key>
          <string>software-package</string>
          <key>url</key>
          <string>$APK_URL</string>
        </dict>
      </array>
    </dict>
  </array>
</dict>
</plist>
EOF
}

set -x

NEW_BUILD_VERSION=$(echo ${BUILD_VERSION} | awk -F. '{print $1"."$2".1"$3}')

OLD_BUILD_VERSION=$(echo ${BUILD_VERSION} | awk -F. '{print $1"\\\."$2"\\\."$3}')

if [ "${NEW_PACKAGENAME}" != "wind" ]; then
  NEW_BUILD_VERSION=$(echo ${BUILD_VERSION} | awk -F. '{print $1"."$2".2"$3}')
fi

NEW_BUNDLE_PATH=${BASE_PATH}/${NEW_BUILD_VERSION}
echo ${NEW_BUNDLE_PATH}
echo ${OLD_BUILD_VERSION}
set +x

function repackAAR() {
  for AARFILEPATH in $(cat ${BASE_PATH}/aarList.txt); do
    rm -rf ${NEW_BUNDLE_PATH}/repackAAR
    unzip $AARFILEPATH -d ${NEW_BUNDLE_PATH}/repackAAR
    cd ${NEW_BUNDLE_PATH}/repackAAR
    ${ANDROID_HOME}/build-tools/28.0.3/dx --dex --output=${NEW_BUNDLE_PATH}/repackAAR/classes.dex ${NEW_BUNDLE_PATH}/repackAAR/classes.jar
    java -jar ${CHANGE_TOOL_PATH}/baksmali-2.3.4.jar d ${NEW_BUNDLE_PATH}/repackAAR/classes.dex -o ${NEW_BUNDLE_PATH}/repackAAR/out
    rm -rf ${NEW_BUNDLE_PATH}/repackAAR/classes.dex
    ls -l ${NEW_BUNDLE_PATH}/repackAAR
    mv ${NEW_BUNDLE_PATH}/repackAAR/out/com/sigmob ${NEW_BUNDLE_PATH}/repackAAR/out/com/${NEW_PACKAGENAME}
    find ${NEW_BUNDLE_PATH}/repackAAR/out -name "*.smali" | xargs sed -i "" "s/Lcom\/sigmob/Lcom\/${NEW_PACKAGENAME}/g"
    find ${NEW_BUNDLE_PATH}/repackAAR/out -name "*.smali" | xargs sed -i "" "s/${OLD_BUILD_VERSION}/${NEW_BUILD_VERSION}/g"

    java -jar ${CHANGE_TOOL_PATH}/smali-2.3.4.jar a ${NEW_BUNDLE_PATH}/repackAAR/out -o ${NEW_BUNDLE_PATH}/repackAAR/classes.dex
    rm -rf ${NEW_BUNDLE_PATH}/repackAAR/classes.jar
    sh ${CHANGE_TOOL_PATH}/dex-tools-2.1-SNAPSHOT/d2j-dex2jar.sh ${NEW_BUNDLE_PATH}/repackAAR/classes.dex -o ${NEW_BUNDLE_PATH}/repackAAR/classes.jar

    rm -rf ${NEW_BUNDLE_PATH}/repackAAR/classes.dex
    rm -rf ${NEW_BUNDLE_PATH}/repackAAR/out
    find ${NEW_BUNDLE_PATH}/repackAAR/ -name "AndroidManifest.xml" | xargs sed -i "" "s/com.sigmob/com.${NEW_PACKAGENAME}/g"
    find ${NEW_BUNDLE_PATH}/repackAAR/ -name "proguard.txt" | xargs sed -i "" "s/com.sigmob/com.${NEW_PACKAGENAME}/g"

    cd ${NEW_BUNDLE_PATH}/repackAAR/
    zip -q -r $AARFILEPATH * -x "*/\.*" -x "\.*"
    cd ..
  done

}

function changeBundle() {
  # replace package name
  if [ -n "${NEW_PACKAGENAME}" ] && [ "${NEED_NEW_PACKAGENAME}" == "true" ]; then
    rm -rf ${NEW_BUNDLE_PATH}
    unzip -q ${FRAMEWORK_ZIP} -d ${NEW_BUNDLE_PATH}

    # repackAAR
    #    rm -rf ${NEW_BUNDLE_PATH}/repackAAR
    #    unzip ${NEW_BUNDLE_PATH}/AAR/windAd-${BUILD_VERSION}.aar -d ${NEW_BUNDLE_PATH}/repackAAR
    #    find ${NEW_BUNDLE_PATH}/repackAAR/ -name "AndroidManifest.xml" | xargs sed -i "" "s/com.sigmob/com.${NEW_PACKAGENAME}/g"
    #    find ${NEW_BUNDLE_PATH}/repackAAR/ -name "proguard*.txt" | xargs sed -i "" "s/com.sigmob/com.${NEW_PACKAGENAME}/g"
    #    ${ANDROID_HOME}/build-tools/28.0.3/dx --dex --output=${NEW_BUNDLE_PATH}/repackAAR/classes.dex ${NEW_BUNDLE_PATH}/repackAAR/classes.jar
    #    java -jar ${CHANGE_TOOL_PATH}/baksmali-2.3.4.jar d ${NEW_BUNDLE_PATH}/repackAAR/classes.dex -o ${NEW_BUNDLE_PATH}/repackAAR/out
    #    rm -rf ${NEW_BUNDLE_PATH}/repackAAR/classes.dex
    #    ls -l ${NEW_BUNDLE_PATH}/repackAAR
    #    mv ${NEW_BUNDLE_PATH}/repackAAR/out/com/sigmob ${NEW_BUNDLE_PATH}/repackAAR/out/com/${NEW_PACKAGENAME}
    #    find ${NEW_BUNDLE_PATH}/repackAAR/out -name "*.smali" | xargs sed -i "" "s/Lcom\/sigmob/Lcom\/${NEW_PACKAGENAME}/g"
    #    find ${NEW_BUNDLE_PATH}/repackAAR/out -name "*.smali" | xargs sed -i "" "s/${OLD_BUILD_VERSION}/${NEW_BUILD_VERSION}/g"
    #    java -jar ${CHANGE_TOOL_PATH}/smali-2.3.4.jar a ${NEW_BUNDLE_PATH}/repackAAR/out -o ${NEW_BUNDLE_PATH}/repackAAR/classes.dex
    #    rm -rf ${NEW_BUNDLE_PATH}/repackAAR/classes.jar ${NEW_BUNDLE_PATH}/repackAAR/out
    #
    #    sh ${CHANGE_TOOL_PATH}/dex-tools-2.1-SNAPSHOT/d2j-dex2jar.sh ${NEW_BUNDLE_PATH}/repackAAR/classes.dex -o ${NEW_BUNDLE_PATH}/repackAAR/classes.jar
    #
    #    rm -rf ${NEW_BUNDLE_PATH}/repackAAR/classes.dex
    #    cd ${NEW_BUNDLE_PATH}/repackAAR/
    #    zip -q -r ${NEW_BUNDLE_PATH}/windAd-${BUILD_VERSION}.aar * -x "*/\.*" -x "\.*"
    #    cd ..
    #    rm -rf ${NEW_BUNDLE_PATH}/AAR/*
    #    mv ${NEW_BUNDLE_PATH}/windAd-${BUILD_VERSION}.aar ${NEW_BUNDLE_PATH}/AAR

    #    rm -rf ${NEW_BUNDLE_PATH}/windAd
    #    cp -rf ${BASE_PATH}/windAd-bundle ${NEW_BUNDLE_PATH}/windAd

    #    find ${NEW_BUNDLE_PATH}/WindAd/app/src/main/java/com/sigmob/android/demo/ -name "*.java" | xargs sed -i "" "s/com.sigmob.windad/com.${NEW_PACKAGENAME}.windad/g"
    #    find ${NEW_BUNDLE_PATH}/WindAd/app/src/main/ -name "AndroidManifest.xml" | xargs sed -i "" "s/com.sigmob.sdk.SigmobXFileProvider/com.${NEW_PACKAGENAME}.sdk.SigmobXFileProvider/g"
    #    find ${NEW_BUNDLE_PATH}/WindAd/app/ -name "proguard-rules.pro" | xargs sed -i "" "s/com.sigmob/com.${NEW_PACKAGENAME}/g"
    #
    #    cp ${NEW_BUNDLE_PATH}/AAR/windAd-${BUILD_VERSION}.aar ${NEW_BUNDLE_PATH}/WindAd/app/libs/
    #
    #    find ${NEW_BUNDLE_PATH}/windAd -name "proguard*.txt" | xargs sed -i "" "s/com.sigmob/com.${NEW_PACKAGENAME}/g"
    find ${NEW_BUNDLE_PATH}/ -name "README.md" | xargs sed -i "" "s/com.sigmob/com.${NEW_PACKAGENAME}/g"

    cd ${BASE_PATH}

    rm ${BASE_PATH}/aarList.txt

    find ${NEW_BUNDLE_PATH} -type f -name "*-${BUILD_VERSION}.aar" >${BASE_PATH}/aarList.txt

    repackAAR

    RENAME_FRAMEWORK_BASE="/opt/php/ciservice/web/android_rename"

    mkdir -p ${RENAME_FRAMEWORK_BASE}/${BUILD_VERSION}${BUILD_DEMO}

    REPACK_ZIP="Wind-Android-Bundle-${BUILD_VERSION}.zip"
    zip -ry ../${REPACK_ZIP} "./" -x "*/\.*" -x "\.*"

    FRAMEWORK_ZIP=${RENAME_FRAMEWORK_BASE}/${BUILD_VERSION}${BUILD_DEMO}/${REPACK_ZIP}
    cp ../${REPACK_ZIP} ${FRAMEWORK_ZIP}
    cd ..
    rm -rf ${NEW_BUNDLE_PATH}
  fi
}

#########################################################replaceTanx###################################################################

function replaceTanx() {

  echo "-----------------replaceTanx------------"

  rm -rf windAAR windSmali classes.dex

  unzip -o app/libs/rename-windAd-${BUILD_VERSION}.aar -d windAAR

  rm -rf windAAR/jni

  ${ANDROID_HOME}/build-tools/28.0.3/dx --dex --output=classes.dex windAAR/classes.jar

  java -jar ${CHANGE_TOOL_PATH}/baksmali-2.3.4.jar d classes.dex -o windSmali

  rm -rf windSmali/com/tan

  rm -rf classes.dex

  rm -rf windAAR/classes.jar

  java -jar ${CHANGE_TOOL_PATH}/smali-2.3.4.jar a windSmali -o classes.dex

  sh ${CHANGE_TOOL_PATH}/dex-tools-2.1-SNAPSHOT/d2j-dex2jar.sh classes.dex -o windAAR/classes.jar

  cd windAAR

  zip -q -r rename-windAd-${BUILD_VERSION}.aar *

  mv -f rename-windAd-${BUILD_VERSION}.aar ${BASE_PATH}/WindTwinDemo/app/libs/rename-windAd-${BUILD_VERSION}.aar

  cd ${BASE_PATH}/WindTwinDemo

  rm -rf windAAR windSmali classes.dex
}

########################################################构建Framework####################################################################

if [ "${IS_GOOGLE_PLAY}" == "true" ]; then
  mv ${BASE_PATH}/build-SigCommon-gp.gradle ${BASE_PATH}/common/build.gradle
else
  mv ${BASE_PATH}/build-SigCommon-cn.gradle ${BASE_PATH}/common/build.gradle
fi

${BASE_PATH}/gradlew :common:assembleRelease -PbuildVersion=${BUILD_VERSION}
if [ $? != 0 ]; then
  echo "gradlew assembleRelease error!"
  exit -1
fi

${BASE_PATH}/gradlew :sigmob-sdk:assembleRelease -PbuildVersion=${BUILD_VERSION}
if [ $? != 0 ]; then
  echo "gradlew assembleRelease error!"
  exit -1
fi

rm -rf ${BASE_PATH}/WindAd
if [ "${IS_GOOGLE_PLAY}" == "true" ]; then
  mv ${BASE_PATH}/WindAdGP ${BASE_PATH}/WindAd
  if [ "${NEED_NEW_PACKAGENAME}" == "true" ] && [ "${NEW_PACKAGENAME}" == "xmlywind" ]; then
    mv ${BASE_PATH}/DemoFile/AndroidManifest.xml.xmly.gp ${BASE_PATH}/WindAd/app/src/main/AndroidManifest.xml
  fi
else
  mv ${BASE_PATH}/WindAdCN ${BASE_PATH}/WindAd
  if [ "${NEED_NEW_PACKAGENAME}" == "true" ] && [ "${NEW_PACKAGENAME}" == "xmlywind" ]; then
    mv ${BASE_PATH}/DemoFile/AndroidManifest.xml.xmly.cn ${BASE_PATH}/WindAd/app/src/main/AndroidManifest.xml
  fi
fi

rm -rf ${BASE_PATH}/OutZip/*

${BASE_PATH}/gradlew copyAAR -PbuildVersion=${BUILD_VERSION}
if [ $? != 0 ]; then
  echo "gradlew copyAAR error!"
  exit -1
fi

${BASE_PATH}/gradlew buildZip -PbuildVersion=${BUILD_VERSION}

FRAMEWORK_BASE="/opt/php/ciservice/web/apk"
rm -rf ${FRAMEWORK_BASE}/${BUILD_VERSION}${BUILD_DEMO}
mkdir ${FRAMEWORK_BASE}/${BUILD_VERSION}${BUILD_DEMO}

FRAMEWORK_ZIP=${BASE_PATH}/OutZip/Sigmob-android-${BUILD_VERSION}.zip

if [ "${IS_GOOGLE_PLAY}" == "true" ]; then
  cp ${FRAMEWORK_ZIP} ${FRAMEWORK_BASE}/${BUILD_VERSION}${BUILD_DEMO}/Wind-android-gp-${BUILD_VERSION}.zip
else
  cp ${FRAMEWORK_ZIP} ${FRAMEWORK_BASE}/${BUILD_VERSION}${BUILD_DEMO}
fi

if [ "${NEED_NEW_PACKAGENAME}" == "true" ]; then
  changeBundle
fi

########################################################构建APK####################################################################

set -x
if [ "$1" == "buildApk" ]; then

  CI_DEMO_NAME=WindDemo

  FRAMEWORK_DEMO=${BASE_PATH}/FrameworkDemo

  rm -rf ${BASE_PATH}/"FrameworkDemo"

  ls -l ${BASE_PATH}/WindDemo/app/libs

  unzip -q ${FRAMEWORK_ZIP} -d ${FRAMEWORK_DEMO}

  ls -l ${FRAMEWORK_DEMO}

  rm -rf ${BASE_PATH}/WindDemo/app/libs/WindAd-*.aar

  cp ${FRAMEWORK_DEMO}/AAR/WindAd-${BUILD_VERSION}.aar ${BASE_PATH}/WindDemo/app/libs

  cp ${FRAMEWORK_DEMO}/AAR/Common-${BUILD_VERSION}.aar ${BASE_PATH}/WindDemo/app/libs

  if [ "${NEED_NEW_PACKAGENAME}" == "true" ]; then
    find ${BASE_PATH}/WindDemo/app/src/main/java/com/sigmob/sigmob/ -name "*.java" | xargs sed -i "" "s/com.sigmob.windad/com.${NEW_PACKAGENAME}.windad/g"
    find ${BASE_PATH}/WindDemo/app/src/main/ -name "AndroidManifest.xml" | xargs sed -i "" "s/com.wind.sdk.SigmobXFileProvider/com.${NEW_PACKAGENAME}.sdk.SigmobXFileProvider/g"
  fi

  VERSION=$(git rev-list HEAD --first-parent --count)
  VERSION=$(expr ${VERSION} + 100)
  rm -rf /opt/php/ciservice/web/projects/${CI_DEMO_NAME}/${VERSION}
  mkdir -p /opt/php/ciservice/web/projects/${CI_DEMO_NAME}/${VERSION}

  APK_BASE="/opt/php/ciservice/web/projects/${CI_DEMO_NAME}/${VERSION}"

  APK_RELEASE="${APK_BASE}/${CI_DEMO_NAME}.apk"
  DEMO_ZIP="${APK_BASE}/${CI_DEMO_NAME}.zip"

  if [ "${IS_TEST}" == "true" ]; then
    APK_RELEASE="${APK_BASE}/${CI_DEMO_NAME}-test.apk"
    DEMO_ZIP="${APK_BASE}/${CI_DEMO_NAME}-test.zip"
  fi

  cd ${BASE_PATH}/WindDemo

  chmod 0755 ${BASE_PATH}/WindDemo/gradlew

  ${BASE_PATH}/WindDemo/gradlew build -PbuildVersion=${BUILD_VERSION}

  cp ${BASE_PATH}/WindDemo/app/build/outputs/apk/release/app-release.apk ${APK_RELEASE}

  rm -rf ${BASE_PATH}/WindDemo/build
  rm -rf ${BASE_PATH}/WindDemo/app/build

  rm -rf ${BASE_PATH}/WindDemo.zip

  zip -ry ${BASE_PATH}/WindDemo.zip "./" -x "*/\.*" -x "\.*" -x "app/build"

  cp -f ${BASE_PATH}/WindDemo.zip ${DEMO_ZIP}
  build_ota_plist_android

elif [ "$1" == "buildXmlyApk" ]; then
  ############################################################################################################################
  # 处理 main/java/com/sigmob/sdk/base/common/Constants.java SDK_FOLDER 字段
  ############################################################################################################################
  if [ "${NEED_NEW_PACKAGENAME}" == "true" ]; then
    SDK_FOLDER_LINE_NUM=$(grep -n "SDK_FOLDER" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java | awk -F: '{print $1}')

    sed -i '' "${SDK_FOLDER_LINE_NUM}d" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java

    INSERT_CODE="\    public static final String SDK_FOLDER = \"sigmob\";"

    sed -i "" "${SDK_FOLDER_LINE_NUM} a\ 
${INSERT_CODE}
" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java

    grep "SDK_FOLDER" ${BASE_PATH}/common/src/main/java/com/sigmob/sdk/common/Constants.java
  fi
  #######################################################先把原来打好的改包名的包copy出来,待会会重新打包#####################################################################
  XMLY_FRAMEWORK_ZIP=${BASE_PATH}/OutZipXmly

  rm -rf ${XMLY_FRAMEWORK_ZIP}
  mkdir ${XMLY_FRAMEWORK_ZIP}

  cp -f ${FRAMEWORK_ZIP} ${XMLY_FRAMEWORK_ZIP}/xmly.zip

  ########################################################第二次构建Framework####################################################################

  ${BASE_PATH}/gradlew clean


  ${BASE_PATH}/gradlew buildJar -PbuildVersion=${BUILD_VERSION}
  ${BASE_PATH}/gradlew buildProguardJar -PbuildVersion=${BUILD_VERSION}

  ${BASE_PATH}/gradlew :windad-sdk:assembleRelease -PbuildVersion=${BUILD_VERSION}
  if [ $? != 0 ]; then
    echo "gradlew assembleRelease error!"
    exit -1
  fi

  rm -rf ${BASE_PATH}/OutZip/*
  ${BASE_PATH}/gradlew copyJar -PbuildVersion=${BUILD_VERSION}
  if [ $? != 0 ]; then
    echo "gradlew copyJar error!"
    exit -1
  fi
  ${BASE_PATH}/gradlew copyAAR -PbuildVersion=${BUILD_VERSION}
  if [ $? != 0 ]; then
    echo "gradlew copyAAR error!"
    exit -1
  fi

  rm -f ${BASE_PATH}/sigmob-sdk/src/main/java/com/sigmob/sdk/SigmobFileProvider.java
  mv -f ${BASE_PATH}/gradle.properties.2 ${BASE_PATH}/gradle.properties

  ${BASE_PATH}/gradlew build -PbuildVersion=${BUILD_VERSION}

  if [ $? != 0 ]; then
    echo "gradlew build error!"
    exit -1
  fi

  ${BASE_PATH}/gradlew buildZip -PbuildVersion=${BUILD_VERSION}
  if [ "${IS_GOOGLE_PLAY}" == "true" ]; then
    ${BASE_PATH}/gradlew buildOthersZip -PbuildVersion=${BUILD_VERSION}
  elif [ "${IS_MEDIATION_CN}" == "true" ]; then

    mv -f ${BASE_PATH}/others-cn/toutiaoX ${BASE_PATH}/toutiao-X
    if [ "${NEED_NEW_PACKAGENAME}" == "false" ]; then
      echo "delete toutiaox adapter"
      rm -rf ${BASE_PATH}/adapterLibs-cn/sigmob-toutiaox-adapters*.aar
    fi
    ${BASE_PATH}/gradlew buildOthersCNZip -PbuildVersion=${BUILD_VERSION}
  else
    echo ""
  fi

  if [ "${IS_GOOGLE_PLAY}" == "true" ]; then
    FRAMEWORK_ZIP=${BASE_PATH}/OutZip/Sigmob-mediation-android-gp-${BUILD_VERSION}.zip
  elif [ "${IS_MEDIATION_CN}" == "true" ]; then
    FRAMEWORK_ZIP=${BASE_PATH}/OutZip/Sigmob-mediation-android-cn-${BUILD_VERSION}.zip
  else
    FRAMEWORK_ZIP=${BASE_PATH}/OutZip/Sigmob-Android-cn-${BUILD_VERSION}.zip
  fi

  ####################################################正常的sdk已打好########################################################################

  CI_DEMO_NAME=com.sigmob.android.demo

  FRAMEWORK_DEMO=${BASE_PATH}/FrameworkDemo

  OLD_FRAMEWORK_DEMO=${BASE_PATH}/OldFrameworkDemo

  rm -rf ${FRAMEWORK_DEMO}
  rm -rf ${OLD_FRAMEWORK_DEMO}

  unzip -q ${FRAMEWORK_ZIP} -d ${FRAMEWORK_DEMO}
  unzip -q ${XMLY_FRAMEWORK_ZIP}/xmly.zip -d ${OLD_FRAMEWORK_DEMO}

  rm -rf ${BASE_PATH}/WindTwinDemo/app/libs/windAd-*.aar
  rm -rf ${BASE_PATH}/WindTwinDemo/app/libs/rename-windAd-*.aar

  cp ${FRAMEWORK_DEMO}/AAR/windAd-${BUILD_VERSION}.aar ${BASE_PATH}/WindTwinDemo/app/libs/windAd-${BUILD_VERSION}.aar
  cp ${OLD_FRAMEWORK_DEMO}/AAR/windAd-${BUILD_VERSION}.aar ${BASE_PATH}/WindTwinDemo/app/libs/rename-windAd-${BUILD_VERSION}.aar

  VERSION=$(git rev-list HEAD --first-parent --count)
  VERSION=$(expr ${VERSION} + 100)
  rm -rf /opt/php/ciservice/web/projects/${CI_DEMO_NAME}/${VERSION}
  mkdir -p /opt/php/ciservice/web/projects/${CI_DEMO_NAME}/${VERSION}

  APK_BASE="/opt/php/ciservice/web/projects/${CI_DEMO_NAME}/${VERSION}"

  APK_RELEASE="${APK_BASE}/${CI_DEMO_NAME}.apk"
  DEMO_ZIP="${APK_BASE}/${CI_DEMO_NAME}.zip"

  if [ "${IS_TEST}" == "true" ]; then
    APK_RELEASE="${APK_BASE}/${CI_DEMO_NAME}-test.apk"
    DEMO_ZIP="${APK_BASE}/${CI_DEMO_NAME}-test.zip"
  fi

  cd ${BASE_PATH}/WindTwinDemo

  replaceTanx

  chmod 0755 ${BASE_PATH}/WindTwinDemo/gradlew

  ${BASE_PATH}/WindTwinDemo/gradlew build -PbuildVersion=${BUILD_VERSION}

  cp ${BASE_PATH}/WindTwinDemo/app/build/outputs/apk/release/app-release.apk ${APK_RELEASE}

  rm -rf ${BASE_PATH}/WindTwinDemo/build

  rm -rf ${BASE_PATH}/WindTwinDemo.zip

  zip -ry ${BASE_PATH}/WindTwinDemo.zip "./" -x "*/\.*" -x "\.*" -x "app/build"

  cp -f ${BASE_PATH}/WindTwinDemo.zip ${DEMO_ZIP}
  build_ota_plist_android
fi

set +x

#########################################################发布###################################################################

if [ "${IS_PUBLISH}" == "true" ]; then
  # upload to Aliyun OSS
  set -x
  if [ "${IS_GOOGLE_PLAY}" == "true" ]; then
    # RELEASE_APK_NAME="${FRAMEWORK_BASE}/${BUILD_VERSION}/Sigmob-Android-googleplay-${BUILD_VERSION}.zip"
    # ZIP_MD5=`md5 -q ${RELEASE_APK_NAME}`
    # python /opt/aliyun/oss_upload.py -k android/${BUILD_VERSION}_sigmob_${ZIP_MD5}/Sigmob_googleplay_${BUILD_VERSION}.zip -f ${RELEASE_APK_NAME}
    echo "Do Nothing!"
  elif [ "${IS_MEDIATION_CN}" == "true" ]; then
    if [ "$NEW_PACKAGENAME" == "xmlywind" ]; then
      RELEASE_APK_NAME="${RENAME_FRAMEWORK_BASE}/${BUILD_VERSION}/sigmob-android-cn-bundle-${BUILD_VERSION}.zip"
      ZIP_MD5=$(md5 -q ${RELEASE_APK_NAME})
      python /opt/aliyun/oss_upload.py -k android/${BUILD_VERSION}_xmly_sigmob_${ZIP_MD5}/sigmob-android-cn-bundle-${BUILD_VERSION}.zip -f ${RELEASE_APK_NAME}
    else
      RELEASE_APK_NAME="${FRAMEWORK_BASE}/${BUILD_VERSION}/sigmob-mediation-android-cn-${BUILD_VERSION}.zip"
      ZIP_MD5=$(md5 -q ${RELEASE_APK_NAME})
      python /opt/aliyun/oss_upload.py -k android/${BUILD_VERSION}_sigmob_${ZIP_MD5}/sigmob-mediation-android-cn-${BUILD_VERSION}.zip -f ${RELEASE_APK_NAME}
    fi
  else
    RELEASE_APK_NAME="${FRAMEWORK_BASE}/${BUILD_VERSION}/Sigmob-Android-cn-${BUILD_VERSION}.zip"
    ZIP_MD5=$(md5 -q ${RELEASE_APK_NAME})
    python /opt/aliyun/oss_upload.py -k android/${BUILD_VERSION}_sigmob_${ZIP_MD5}/sigmob-android-cn-${BUILD_VERSION}.zip -f ${RELEASE_APK_NAME}
  fi
  if [ $? -eq 0 ]; then
    echo 'Android Upload Aliyun OSS SUCCESS!'
  else
    echo 'Android Upload Aliyun OSS ERROR!'
    exit -1
  fi

  if [ "${IS_GOOGLE_PLAY}" == "true" ]; then
    ALL_RELEASE_APK_NAME="${FRAMEWORK_BASE}/${BUILD_VERSION}/Sigmob-mediation-android-gp-${BUILD_VERSION}.zip"
    ALL_ZIP_MD5=$(md5 -q ${ALL_RELEASE_APK_NAME})
    python /opt/aliyun/oss_upload.py -k android/${BUILD_VERSION}_sigmob_${ALL_ZIP_MD5}/sigmob-mediation-android-gp-${BUILD_VERSION}.zip -f ${ALL_RELEASE_APK_NAME}
    if [ $? -eq 0 ]; then
      echo 'Sigmob-mediation-android-gp Upload Aliyun OSS SUCCESS!'
    else
      echo 'Sigmob-mediation-android-gp Upload Aliyun OSS ERROR!'
      exit -1
    fi
  fi

  # sync sdk zip info to http://manager.sigmob.com
  INSTRUCTION=$(awk -v RS='### ' '{if(NR>1){if($1=="'${BUILD_VERSION}'"){print $0}}}' ${BASE_PATH}/CHANGELOG.MD)
  if [ -z "${INSTRUCTION}" ]; then
    INSTRUCTION="version update"
  fi

  CDN_URL="https://sdkres.sigmob.cn/android/${BUILD_VERSION}_sigmob_${ZIP_MD5}/sigmob-android-cn-${BUILD_VERSION}.zip"
  if [ "${IS_GOOGLE_PLAY}" == "true" ]; then
    # CDN_URL="https://sdkres.sigmob.cn/android/${BUILD_VERSION}_sigmob_${ZIP_MD5}/Sigmob_googleplay_${BUILD_VERSION}.zip"
    MEDIATION_ANDROID_GP_CDN_URL="https://sdkres.sigmob.cn/android/${BUILD_VERSION}_sigmob_${ALL_ZIP_MD5}/sigmob-mediation-android-gp-${BUILD_VERSION}.zip"
  elif [ "${IS_MEDIATION_CN}" == "true" ]; then
    if [ "$NEW_PACKAGENAME" == "xmlywind" ]; then
      MEDIATION_ANDROID_CN_CDN_URL="https://sdkres.sigmob.cn/android/${BUILD_VERSION}_xmly_sigmob_${ZIP_MD5}/sigmob-android-cn-bundle-${BUILD_VERSION}.zip"
    else
      MEDIATION_ANDROID_CN_CDN_URL="https://sdkres.sigmob.cn/android/${BUILD_VERSION}_sigmob_${ZIP_MD5}/sigmob-mediation-android-cn-${BUILD_VERSION}.zip"
    fi
  fi
  set +x
  echo "============================================================================================================"
  if [ "${IS_GOOGLE_PLAY}" == "true" ]; then
    echo ${MEDIATION_ANDROID_GP_CDN_URL}
  elif [ "${IS_MEDIATION_CN}" == "true" ]; then
    echo ${MEDIATION_ANDROID_CN_CDN_URL}
  else
    echo ${CDN_URL}
  fi
  echo "============================================================================================================"
  set -x
  NOW=$(date +%s)
  NONCE=$(expr ${NOW} \* 1000)

  SIGN_SRC="/ssp/sdk/ci/syncreadme"$NONCE"MTVlMWFmNjRkOTRjYWI0ZmNmZmYzNTg4NDBlMjFhMmI="
  SIGN=$(md5 -q -s ${SIGN_SRC})

  HOST="manager.sigmob.com"
  #HOST="testc.sigmob.com"

  SYNC_URL="https://${HOST}/ssp/sdk/ci/syncreadme?nonce=${NONCE}&sign=${SIGN}"

  if [ "${IS_GOOGLE_PLAY}" == "true" ]; then
    # android+gp （暂时不用）
    # curl -s --data-urlencode "os=2" --data-urlencode "version=${BUILD_VERSION}" --data-urlencode "cdn=${CDN_URL}" --data-urlencode "instructions=${INSTRUCTION}" --data-urlencode "source=googleplay" ${SYNC_URL}
    # android+gp（单+聚合）
    curl -s --data-urlencode "os=2" --data-urlencode "version=${BUILD_VERSION}" --data-urlencode "cdn=${MEDIATION_ANDROID_GP_CDN_URL}" --data-urlencode "instructions=${INSTRUCTION}" --data-urlencode "source=other" ${SYNC_URL}
  elif [ "${IS_MEDIATION_CN}" == "true" ]; then
    if [ "$NEW_PACKAGENAME" == "xmlywind" ]; then
      SOURCE="xmly"
    else
      SOURCE="other_cn"
    fi
    curl -s --data-urlencode "os=2" --data-urlencode "version=${BUILD_VERSION}" --data-urlencode "cdn=${MEDIATION_ANDROID_CN_CDN_URL}" --data-urlencode "instructions=${INSTRUCTION}" --data-urlencode "source=${SOURCE}" ${SYNC_URL}
  else
    # android
    curl -s --data-urlencode "os=2" --data-urlencode "version=${BUILD_VERSION}" --data-urlencode "cdn=${CDN_URL}" --data-urlencode "instructions=${INSTRUCTION}" --data-urlencode "source=sigmob" ${SYNC_URL}
  fi

  # 同步在线开发者文档服务
  scp ${README_PATH} root@docs.sigmob.cn:/data/sigmob_web/web/websrc/Sigmob-Admin/docs/sdk/android.md

fi

if [ -n "${NEW_TAG_NAME}" ]; then
  git tag -a ${NEW_TAG_NAME} -m 'Create tag by CI service'
  git push origin ${NEW_TAG_NAME}
fi

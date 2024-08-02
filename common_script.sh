#!/bin/bash

set -x

COMMON_BASE_PATH=$(pwd)

AAR_PATH="${COMMON_BASE_PATH}/output/AAR/"
COMMON_LOGGER_PATH=${COMMON_BASE_PATH}/logger/src/main/java/com/czhj/sdk/logger/SigmobLog.java
COMMON_CONSTANTS_PATH=${COMMON_BASE_PATH}/common/src/main/java/com/czhj/sdk/common/Constants.java

COMMON_GRADLE=${COMMON_BASE_PATH}/common/build.gradle
COMMON_VERSION_NAME=""

COMMON_IS_GOOGLE_PLAY="false"
COMMON_IS_PUBLISH="false"
COMMON_IS_DEBUG="false"

SNAPSHOT=""

Usage() { #定义函数Usage，输出脚本使用方法
  echo "Usage"
  echo "build_script :"
  echo "[-c ] 参数=Debug/Release"
  #  echo "[-g ] 无参数 (打包Google Play环境)"
  echo "[-p ] 无参数 代表是否发布"
  exit 0
}


while getopts ":hgpc:" opt; do
  case $opt in
  h)
    Usage
    exit
    ;;
  c)
    if [ "${OPTARG}" == "true" ]; then
      SNAPSHOT="-SNAPSHOT"
      COMMON_IS_DEBUG="true"
    fi
    ;;
  g)
    COMMON_IS_GOOGLE_PLAY="true"
    ;;
  p)
    COMMON_IS_PUBLISH="true"
    ;;
  *)
    Usage
    exit 0
    ;;
  esac
done

############################################################################################################################
# 处理 WIND_CONSTANTS_PATH COMMON_VERSION 字段
############################################################################################################################

function newTag() {
    if [ "${COMMON_IS_DEBUG}" == "false" ]; then
      if [ -n "${COMMON_VERSION_NAME}" ]; then
        git tag -a ${COMMON_VERSION_NAME} -m 'Create tag by CI service'
        git push origin ${COMMON_VERSION_NAME}
      fi
    fi
}

function pushMavenCommonSDK() {

  ${COMMON_BASE_PATH}/gradlew :common:uploadArchives -PbuildVersion=${COMMON_VERSION_NAME}${SNAPSHOT}
  if [ $? != 0 ]; then
    echo "gradlew pushMavenCommonSDK error!"
    exit -1
  fi

}

function setDebug() {

      echo "set COMMON_LOGGER_PATH Debug Status : " +${COMMON_IS_DEBUG}
      IS_DEBUG_LINE_NUM=$(grep -n "DEBUG" ${COMMON_LOGGER_PATH} | awk -F: '{print $1}'  | head -n 1)


      sed -i '' "${IS_DEBUG_LINE_NUM}d" ${COMMON_LOGGER_PATH}

      if [ "${COMMON_IS_DEBUG}" == "false" ]; then
        SIG_INSERT_CODE="\    private static final boolean DEBUG = false;"
      else
        SIG_INSERT_CODE="\    private static final boolean DEBUG = true;"
      fi

        sed -i "" "${IS_DEBUG_LINE_NUM} a\ 
    ${SIG_INSERT_CODE}
        " ${COMMON_LOGGER_PATH}

      grep "DEBUG" ${COMMON_LOGGER_PATH}
}


function changeGP() {
  if [ "${COMMON_IS_GOOGLE_PLAY}" == "true" ]; then
    echo "set COMMON_CONSTANTS_PATH GOOGLE_PLAY Status : " +${COMMON_IS_GOOGLE_PLAY}
    IS_GP_LINE_NUM=$(grep -n "GOOGLE_PLAY" ${COMMON_CONSTANTS_PATH} | awk -F: '{print $1}'  | head -n 1)

    sed -i '' "${IS_GP_LINE_NUM}d" ${COMMON_CONSTANTS_PATH}
    SIG_GP_INSERT_CODE="\    public static final boolean GOOGLE_PLAY = true;"

    sed -i "" "${IS_GP_LINE_NUM} a\ 
    ${SIG_GP_INSERT_CODE}
    " ${COMMON_CONSTANTS_PATH}

    grep "GOOGLE_PLAY" ${COMMON_CONSTANTS_PATH}
  fi
}


function buildCommonSDK() {

  echo "buildCommonSDK Debug Status : " +${SNAPSHOT}

  COMMON_BUILD_VERSION=$(grep "SDK_VERSION" ${COMMON_CONSTANTS_PATH} | awk '{match($NF,/[0-9]+/,m);print m[0]}')

  echo "COMMON_BUILD_VERSION " ${COMMON_BUILD_VERSION}

  COMMON_VERSION_NAME=$(echo $COMMON_BUILD_VERSION | awk '{print substr($NF,1,1)"."substr($NF,2,1)"."substr($NF,3)}')

  echo "COMMON_VERSION_NAME " ${COMMON_VERSION_NAME}

  if [ "${COMMON_IS_GOOGLE_PLAY}" == "true" ]; then
    cp -f ${COMMON_BASE_PATH}/build-SigCommon-gp.gradle ${COMMON_GRADLE}
  else
    cp -f ${COMMON_BASE_PATH}/build-SigCommon-cn.gradle ${COMMON_GRADLE}
  fi

  ${COMMON_BASE_PATH}/gradlew :common:clean
  ${COMMON_BASE_PATH}/gradlew :common:assembleRelease -PbuildVersion=${COMMON_VERSION_NAME}

  if [ $? != 0 ]; then
    echo "gradlew common assembleRelease error!"
    exit -1
  fi

  rm -rf ${AAR_PATH}

  mkdir -p ${AAR_PATH}

  ${COMMON_BASE_PATH}/gradlew :common:copyAAR -PbuildVersion=${COMMON_VERSION_NAME}
  if [ $? != 0 ]; then
    echo "gradlew common copyAAR error!"
    exit -1
  fi

  if [ "${COMMON_IS_PUBLISH}" == "true" ]; then
    pushMavenCommonSDK
    newTag
  fi

}

function build() {

  git submodule update --init --recursive

  setDebug
  changeGP
  buildCommonSDK
}

build

set +x

# ZipDemo

## WindAd Demo

* copyJar
./gradlew copyJar
###  Build Zip 
cd WindAd
./gradlew buildZip

## Sigmob Android SDK

## clean
./gradlew clean

### Build
./gradlew buildJar && ./gradlew buildProguardJar && ./gradlew build

### Output

### debugDemoApp
>  app/build/output/apk

### Jar Libs
* relase
 windAd-{versionName}.jar
* debug
 windAd-{versionName}-debug.jar
>app/libs/windAd/*.jar

### Mapping File
> app/libs/windAd/mapping.txt

### tips
* copyJar befor need BuildJar with SDK
* versionName is Tags name


***proto***

proto 定义要求

1. syntax = "proto3";
2. option optimize_for = LITE_RUNTIME;
3. option java_multiple_files = true;


编译命令：
* 必须带上--android
> java -jar wire-compiler-2.3.0-RC1-jar-with-dependencies.jar --proto_path=< proto 目录或者文件路径> --java_out=<输出路径> --android


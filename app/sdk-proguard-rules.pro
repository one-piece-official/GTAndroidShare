# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
#指定代码的压缩级别
-optimizationpasses 5

#包明不混合大小写
-dontusemixedcaseclassnames

#不去忽略非公共的库类
-dontskipnonpubliclibraryclasses

 #优化  不优化输入的类文件
-dontoptimize

-keepattributes Signature

 #预校验
-dontpreverify

 #混淆时是否记录日志
-verbose

 # 混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
-keep class **.R$* {*;}

#保护注解
-keepattributes *Annotation*
-keepattributes *JavascriptInterface*

# 保持哪些类不被混淆
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.content.Content
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService
#如果有引用v4包可以添加下面这行
-keep public class * extends android.support.v4.**
-keep public class * extends android.support.percent


-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.**
-dontwarn android.net.**

#忽略警告
-ignorewarnings

# 枚举类不能被混淆
-keepclassmembers enum * {
<fields>;
public static **[] values();
public static ** valueOf(java.lang.String);
}

# 保留自定义控件(继承自View)不能被混淆
-keep public class * extends android.view.View {
public <init>(android.content.Context);
public <init>(android.content.Context, android.util.AttributeSet);
public <init>(android.content.Context, android.util.AttributeSet, int);
public void set*(***);
*** get* ();
}

# 保留Parcelable序列化的类不能被混淆
-keep class * implements android.os.Parcelable{
public static final android.os.Parcelable$Creator *;
}

################Canary#################
-dontwarn com.squareup.haha.guava.**
-dontwarn com.squareup.haha.perflib.**
-dontwarn com.squareup.haha.trove.**
-dontwarn com.squareup.leakcanary.**
-keep class com.squareup.haha.** { *; }
-keep class com.squareup.leakcanary.** { *; }

# bugly
-dontwarn com.tencent.bugly.**
-keep public class com.tencent.bugly.**{*;}

#不混淆Serializable接口的子类中指定的某些成员变量和方法
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepclassmembers class ** {
  ** webpageUrl;
  ** mediaObject;
  ** title;
  ** description;
  ** thumbData;
  ** transaction;
  ** message;
  ** scene;
}

-printmapping mapping.txt #混淆后文件映射

# android.net
-dontwarn android.net.**
-keep class android.net.SSLCertificateSocketFactory{*;}

# androidx

-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**

# support-v4
-dontwarn android.support.v4.**
-keep class android.support.v4.** { *; }
-keep interface android.support.v4.** { *; }
-keep public class * extends android.support.v4.**


-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keep class sun.misc.Unsafe { *; }

-keep class com.sigmob.windad.**{
    public *;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-dontwarn com.bun.miitmdid.**
-dontwarn com.bun.supplier.**

-keep class com.bun.miitmdid.** {*;}
-keep class com.bun.supplier.** {*;}
-keep interface com.bun.supplier.** {*;}

-keep class com.czhj.sdk.common.models.Config{public *;}
-keep class com.sigmob.logger.**{public *;}
-keep class com.czhj.sdk.common.Constants{public *;}
-keep class com.czhj.sdk.common.CustomController{public *;}

-keep class com.sigmob.sdk.common.models.** {*; }
-keep class com.sigmob.sdk.base.models.** { *; }

-keep class com.czhj.devicehelper.DeviceHelper {public *; }
-keep class com.sigmob.devicehelper.oaId.helpers.DevicesIDsHelper$* {public *; }
-keep class com.czhj.sdk.common.mta.**{public *;}
-keep class com.sigmob.sdk.base.models.** {public *; }

-keep interface com.sigmob.wire.** { *; }

-keep class com.sigmob.wire.** { *; }

-keep interface com.sigmob.wire.** { *; }

-keep class com.sigmob.sdk.SigmobFileProvider {*;}
-keep class com.sigmob.sdk.SigmobXFileProvider {*;}


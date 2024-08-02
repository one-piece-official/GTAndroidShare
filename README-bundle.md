# Sigmob Android SDK 接入说明

[详细信息请参考Android在线文档](http://docs.sigmob.cn/#/sdk/android)

## 准备工作

* 1 到需要接入的渠道平台申请对应的appId、appKey、 placementId（先找接口商务提供）。

* 2 到sigmob流量变现管理平台配置app各渠道的参数（目前代为操作，且仅支持Sigmob渠道）。

* 3 解压我们提供的压缩包，把WindAd*.aar放入app的libs工程中。

## 集成步骤

### 添加Sigmob SDK 依赖

```
dependencies {

    	//windAd SDK aar文件 放入项目libs中
    	implementation fileTree(include: ['*.aar'], dir: 'libs')

}
```
***加入 AndroidX 或者 Android Support V4 依赖支持库(二选一)***

```
dependencies {

    //AndroidX
    implementation 'androidx.legacy:legacy-support-v4:1.0.0'
}
```

或者

```
dependencies {

    //Android support v4
    implementation 'com.android.support:support-v4:23.0.+'
}
```


#### Android 10 OAID 支持
  
**建议开发者优先集成OAID。Android 10 无法通过常规方式获取IMEI，影响广告测试及正式广告的填充。**

**MSA联盟官网地址: <http://www.msa-alliance.cn/col.jsp?id=120>**

* 1、把 msa_mdid_x.x.x.aar 或者 oaid_sdk_x.x.x.aar 拷贝到项的 libs 目录，并设置依赖，其中 x.x.x 代表版本号。

	 	implementation files('libs/msa_mdid_x.x.x.aar')
        或者
        implementation files('libs/oaid_sdk_x.x.x.aar')

* 2、将 supplierconfig.json 拷贝到项目 assets 目录下，并修改里边对应内容，特别是需要设置 appid 的部分。需要设置 appid 的部分需要去对应厂商的应用商店里注册自己的app。

* 3、**混淆设置**:

    >***现SDK支持oaid_sdk_1.0.10、1.0.13、1.0.22、1.0.23、1.0.25等5个版本、但是媒体必须按照以下混淆配置进行设置***

		-dontwarn com.bun.**
        -keep class com.bun.** {*;}
        -keep class a.**{*;}
        -keep class XI.CA.XI.**{*;}
        -keep class XI.K0.XI.**{*;}
        -keep class XI.XI.K0.**{*;}
        -keep class XI.vs.K0.**{*;}
        -keep class XI.xo.XI.XI.**{*;}
        -keep class com.asus.msa.SupplementaryDID.**{*;}
        -keep class com.asus.msa.sdid.**{*;}
        -keep class com.huawei.hms.ads.identifier.**{*;}
        -keep class com.samsung.android.deviceidservice.**{*;}
        -keep class com.zui.opendeviceidlibrary.**{*;}
        -keep class org.json.**{*;}
        -keep public class com.netease.nis.sdkwrapper.Utils {public <methods>;}

* 4、设置 gradle 编译选项，这块可以根据自己对平台的选择进里合里配置。

		ndk {
            	abiFilters 'armeabi-v7a','x86','arm64-v8a','x86_64','armeabi'
        	}
        packagingOptions {
            doNotStrip "*/armeabi-v7a/*.so"
            doNotStrip "*/x86/*.so"
            doNotStrip "*/arm64-v8a/*.so"
            doNotStrip "*/x86_64/*.so"
            doNotStrip "armeabi.so"
        }


#### 更新 AndroidManifest.xml

***权限声明***

```
<manifest>

    <!-- SDK所需要权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- 限于中国大陆Android 应用市场 -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" tools:node="replace"/>
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

</manifest>

```

***广告展示Activity 声明***

```
<manifest>

    <application>

        <!--广告展示Activity -->
        <activity
            android:name="com.wind.sdk.base.common.AdActivity"
            android:configChanges="keyboard|keyboardHidden|orientation|screenSize"
            android:theme="@style/sig_transparent_style" />

    </application>

</manifest>
```
***provider 声明(Android Support V4)***

```
<manifest>

    <application>

        <!-- targetSDKVersion >= 24时才需要添加这个provider。
       provider的authorities属性的值为${applicationId}.sigprovider -->

        <provider
            android:name="com.wind.sdk.SigmobFileProvider"
            android:authorities="${applicationId}.sigprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/sigmob_provider_paths"/>
        </provider>

    </application>

</manifest>
```

***provider 声明(AndroidX)***

```
<manifest>

    <application>

        <!-- targetSDKVersion >= 24时才需要添加这个provider，provider的authorities属性的值为${applicationId}.sigprovider -->
        <provider
            android:name="com.wind.sdk.SigmobXFileProvider"
            android:authorities="${applicationId}.sigprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/sigmob_provider_paths"/>
        </provider>

    </application>

</manifest>
```

***添加 provider 路径配置***
* 在项目结构下的res目录下添加一个xml文件夹，再新建一个sigmob_provider_paths.xml的文件，文件内容如下

```
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 这个下载路径不可以修改，SigDownload -->
    <external-cache-path name="SigMob_root" path="SigDownload" />
    <external-path name="SigMob_root_external" path="." />
</paths>
```

#### 混淆配置

```
# 优化 不优化输入的类文件

-dontoptimize

# androidx

-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**

# android.support.v4

-dontwarn android.support.v4.**
-keep class android.support.v4.** { *; }
-keep interface android.support.v4.** { *; }
-keep public class * extends android.support.v4.**

# WindAd

-keep class sun.misc.Unsafe { *; }
-dontwarn com.wind.**
-keep class com.wind.**.**{*;}

# miitmdid
-dontwarn com.bun.**
-keep class com.bun.** {*;}
-keep class a.**{*;}
-keep class XI.CA.XI.**{*;}
-keep class XI.K0.XI.**{*;}
-keep class XI.XI.K0.**{*;}
-keep class XI.vs.K0.**{*;}
-keep class XI.xo.XI.XI.**{*;}
-keep class com.asus.msa.SupplementaryDID.**{*;}
-keep class com.asus.msa.sdid.**{*;}
-keep class com.huawei.hms.ads.identifier.**{*;}
-keep class com.samsung.android.deviceidservice.**{*;}
-keep class com.zui.opendeviceidlibrary.**{*;}
-keep class org.json.**{*;}
-keep public class com.netease.nis.sdkwrapper.Utils {public <methods>;}

```




## SDK接口类介绍与广告接入示例代码

### SDK初始化
```
    WindAds ads = WindAds.sharedAds();

    ads.setAdult(true);//是否未成年/true成年/flase未成年
    ads.setPersonalizedAdvertisingOn(true);//是否关闭个性化推荐接口/true关闭/false开启

    //useMediation:true代表使用聚合服务;false:代表单接SigMob
    ads.startWithOptions(activity, new WindAdOptions(appId, appKey, false));
```


### 中国大陆权限授权接口（仅针对中国大陆）
```
    //主动READ_PHONE_STATE，WRITE_EXTERNAL_STORAGE，ACCESS_FINE_LOCATION 权限授权请求
    WindAds.requestPermission(actvity);
```

### 激励视频广告
***设置监听回调***

```
        private WindRewardedVideoAd windRewardedVideoAd;

        //placementId 必填,USER_ID,OPTIONS可不填，
        WindRewardAdRequest request = new WindRewardAdRequest(PLACEMENT_ID, USER_ID, OPTIONS);

        windRewardedVideoAd = new WindRewardedVideoAd(this, request);

        windRewardedVideoAd.setWindRewardedVideoAdListener(new WindRewardedVideoAdListener() {

            //仅sigmob渠道有回调，聚合其他平台无次回调
            @Override
            public void onVideoAdPreLoadSuccess(String placementId) {
                Toast.makeText(mContext, "激励视频广告数据返回成功", Toast.LENGTH_SHORT).show();
            }

            //仅sigmob渠道有回调，聚合其他平台无次回调
            @Override
            public void onVideoAdPreLoadFail(String placementId) {
                Toast.makeText(mContext, "激励视频广告数据返回失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVideoAdLoadSuccess(String placementId) {
                Toast.makeText(mContext, "激励视频广告缓存加载成功,可以播放", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVideoAdPlayStart(String placementId) {
                Toast.makeText(mContext, "激励视频广告播放开始", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVideoAdPlayEnd(String placementId) {
                Toast.makeText(mContext, "激励视频广告播放结束", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onVideoAdClicked(String placementId) {
                Toast.makeText(mContext, "激励视频广告CTA点击事件监听", Toast.LENGTH_SHORT).show();
            }

            //WindRewardInfo中isComplete方法返回是否完整播放
            @Override
            public void onVideoAdClosed(WindRewardInfo windRewardInfo, String placementId) {
                if (windRewardInfo.isComplete()) {
                    Toast.makeText(mContext, "激励视频广告完整播放，给予奖励", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "激励视频广告关闭", Toast.LENGTH_SHORT).show();
                }
            }

            /**
             * 加载广告错误回调
             * WindAdError 激励视频错误内容
             * placementId 广告位
             */
            @Override
            public void onVideoAdLoadError(WindAdError windAdError, String placementId) {
                Toast.makeText(mContext, "激励视频广告加载错误", Toast.LENGTH_SHORT).show();
            }

            /**
             * 播放错误回调
             * WindAdError 激励视频错误内容
             * placementId 广告位
             */
            @Override
            public void onVideoAdPlayError(WindAdError windAdError, String placementId) {
                Toast.makeText(mContext, "激励视频广告播放错误", Toast.LENGTH_SHORT).show();
            }
        });
```

***激励视频广告加载***
```
    /**
     *同一个windRewardedVideoAd不建议在广告playing中重复load
     *同一个windRewardedVideoAd在onVideoAdClosed中可以load下一次广告
     */
    if (windRewardedVideoAd != null) {
        windRewardedVideoAd.loadAd();
    }
```

***激励视频广告播放***
```
    try {
        /**
         *收到onVideoAdLoadSuccess回调代表广告已ready
         */
        if (windRewardedVideoAd != null && windRewardedVideoAd.isReady()) {
            //广告播放
            windRewardedVideoAd.show(this, option);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
```

### 插屏广告集成相关

**注意:** 如果聚合集成了广点通的渠道，则触发展示插屏广告的Activity需要配置：

    android:configChanges="keyboard|keyboardHidden|orientation|screenSize"


***设置监听回调***
```
        private WindInterstitialAd windInterstitialAd1;

        //placementId 必填,USER_ID,OPTIONS可不填，
        WindInterstitialAdRequest request = new WindInterstitialAdRequest(PLACEMENT_ID, USER_ID, OPTIONS);

        windInterstitialAd = new WindInterstitialAd(this, request);

        windInterstitialAd.setWindInterstitialAdListener(new WindInterstitialAdListener() {

            //仅sigmob渠道有回调，聚合其他平台无次回调
            @Override
            public void onInterstitialAdPreLoadSuccess(String placementId) {
                Toast.makeText(mContext, "插屏广告数据返回成功", Toast.LENGTH_SHORT).show();
            }

            //仅sigmob渠道有回调，聚合其他平台无次回调
            @Override
            public void onInterstitialAdPreLoadFail(String placementId) {
                Toast.makeText(mContext, "插屏广告数据返回失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onInterstitialAdLoadSuccess(String placementId) {
                Toast.makeText(mContext, "插屏广告缓存加载成功,可以播放", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onInterstitialAdPlayStart(String placementId) {
                Toast.makeText(mContext, "插屏广告播放开始", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onInterstitialAdPlayEnd(final String placementId) {
                Toast.makeText(mContext, "插屏广告播放结束", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onInterstitialAdClicked(String placementId) {
                Toast.makeText(mContext, "插屏广告点击事件监听", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onInterstitialAdClosed(String placementId) {
                Toast.makeText(mContext, "插屏广告关闭", Toast.LENGTH_SHORT).show();
            }

            /**
             * 加载广告错误回调
             * WindAdError 插屏错误内容
             * placementId 广告位
             */
            @Override
            public void onInterstitialAdLoadError(WindAdError windAdError, String placementId) {
                Toast.makeText(mContext, "插屏广告加载错误", Toast.LENGTH_SHORT).show();
            }

            /**
             * 播放错误回调
             * WindAdError 插屏错误内容
             * placementId 广告位
             */
            @Override
            public void onInterstitialAdPlayError(WindAdError windAdError, String placementId) {
                Toast.makeText(mContext, "插屏广告播放错误", Toast.LENGTH_SHORT).show();
            }
        });
```

***插屏广告加载***
```
    /**
     *同一个windInterstitialAd不建议在广告playing中重复load
     *同一个windInterstitialAd在onInterstitialAdClosed中可以load下一次广告
     */
    if (windInterstitialAd != null) {
        windInterstitialAd.loadAd();
    }
```

***插屏广告播放***
```
    try {
        /**
         *收到onInterstitialAdLoadSuccess回调代表广告已ready
         */
        if (windInterstitialAd != null && windInterstitialAd.isReady()) {
            //广告播放
            windInterstitialAd.show(this, option);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
```

### 开屏广告集成相关

> 目前开屏广告仅支持竖屏

***设置监听回调***
```
    // 开屏广告成功展示
    @Override
    public void onSplashAdSuccessPresent() {

    }

    /**
    *  开屏广告成功加载
    *  如果不是LoadAndShow模式,则需要调用showAd()方法展示广告
    *  adContainer 开屏内容展示容器,若传null，则默认进行全屏展示
    */
    @Override
    public void onSplashAdSuccessLoad() {
        if (!isLoadAndShow && mWindSplashAD.isReady()) {
            mWindSplashAD.showAd(adContainer);
        }
    }

    /**
    * 开屏广告展示失败
    * WindAdError 开屏广告错误内容
    * placementId 广告位
    */
    @Override
    public void onSplashAdFailToLoad(WindAdError error, String placementId) {
        //广告失败直接进入主页面
        jumpMainActivity();
    }
   
    // 开屏广告点击
    @Override
    public void onSplashAdClicked() {

    }

    // 开屏广告关闭
    @Override
    public void onSplashClosed() {
        //需要判断是否能进入主页面
        jumpWhenCanClick();
    }
```
***开屏广告点击注意事项：***

```

    /**
     * 设置一个变量来控制当前开屏页面是否可以跳转，当开屏广告为普链类广告时，点击会打开一个广告落地页，此时开发者还不能打开自己的App主页。当从广告落地页返回以后，
     * 才可以跳转到开发者自己的App主页；当开屏广告是App类广告时只会下载App。
     */

    public boolean canJumpImmediately = false;

    private void jumpWhenCanClick() {
        if (canJumpImmediately) {
            jumpMainActivity();
        } else {
            canJumpImmediately = true;
        }
    }

    /**
     * 不可点击的开屏，使用该jump方法，而不是用jumpWhenCanClick
     */
    private void jumpMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        this.startActivity(intent);
        this.finish();
    }

    @Override
    protected void onPause() {
        canJumpImmediately = false;
    }

    @Override
    protected void onResume() {
        if (canJumpImmediately) {
            jumpWhenCanClick();
        }
        canJumpImmediately = true;
    }
```

***开屏播放接口***

  ***此方式自适应广告展示大小，自带LOGO样式展示APP信息，无需开发者处理底部LOGO内容***

```
//PLACEMENT_ID必填
WindSplashAdRequest splashAdRequest = new WindSplashAdRequest(PLACEMENT_ID,USER_ID,OPTIONS);

/**
 * 广告结束，广告内容是否自动隐藏.默认是false
 * 若开屏和应用共用Activity，建议false。
 * 若开屏是单独Activity ，建议true。
*/
adRequest.setDisableAutoHideAd(true);

//广告允许最大等待返回时间
splashAdRequest.setFetchDelay(5);

/**
 * 设置开屏应用LOGO区域
 * 设置此项开屏会默认使用Window渲染
*/
splashAdRequest.setAppTitle(appTitle);

//设置开屏应用LOGO描述
splashAdRequest.setAppDesc(appDesc);

/**
 * 方法:
 *   WindSplashAD(Activity activity,WindSplashAdRequest splashAdRequest, WindSplashADListener adListener)
 * 参数说明:
 *   activity 开屏展示Activity
 *   adRequest WindAdRequest广告请求
 *   adListener 开屏事件监听
*/

WindSplashAD mWindSplashAD =  new WindSplashAD(activity,splashAdRequest,this);

/**
 * 建议默认实时加载并展示广告
*/
private boolean isLoadAndShow = true;

/**
 * adContainer 开屏内容展示容器,若传null，则默认进行全屏展示
*/
if (isLoadAndShow) {
    mWindSplashAD.loadAdAndShow(adContainer);//不需要再调用mWindSplashAD.showAd();
} else {
    mWindSplashAD.loadAdOnly();//需要在onSplashAdSuccessLoad回调里调用mWindSplashAD.showAd();
}

```





### 原生自渲染广告

**注意:** 原生自渲染广告具体实现可参考Demo中NativeAdUnifiedActivity、NativeAdUnifiedListActivity、NativeAdUnifiedRecycleActivity等

***原生自渲染广告加载***

```
    private ViewGroup adContainer;

    private WindNativeUnifiedAd windNativeUnifiedAd;

    private List<NativeADData> unifiedADDataList;

    WindNativeAdRequest windNativeAdRequest = new WindNativeAdRequest(placementId, String.valueOf(userID), 3, options);

    windNativeUnifiedAd = new WindNativeUnifiedAd(this, windNativeAdRequest);

    windNativeUnifiedAd.loadAd(new WindNativeUnifiedAd.NativeAdLoadListener() {
        //广告加载失败
        @Override
        public void onError(WindAdError error, String placementId) {
            Log.d("lance", "onError:" + error.toString() + ":" + placementId);
        }

        //广告加载成功
        @Override
        public void onFeedAdLoad(String placementId) {
            List<NativeADData> unifiedADData = windNativeUnifiedAd.getNativeADDataList();
                if (unifiedADData != null && unifiedADData.size() > 0) {
                    Log.d("lance", "onFeedAdLoad:" + unifiedADData.size());
                    unifiedADDataList = unifiedADData;
                }
            }
        });
     
```

***原生自渲染广告展示***
```

    /**
     * 请在收到onFeedAdLoad回调后再展示广告
     * windNativeUnifiedAd.getNativeADDataList()获取广告
     */

    if (unifiedADDataList != null && unifiedADDataList.size() > 0) {
        NativeADData nativeADData = unifiedADDataList.get(0);
        //创建一个装整个自渲染广告的容器
        WindNativeAdContainer windContainer = new WindNativeAdContainer(this);
        /**
         * 媒体自渲染的View
         * 需要继承SDK内部WindNativeAdRender类并实现接口
         */
        NativeAdDemoRender adRender = new NativeAdDemoRender();
        //将容器和view链接起来
        nativeADData.connectAdToView(this, windContainer, adRender);
        //设置dislike弹窗
        nativeADData.setDislikeInteractionCallback(this, new NativeADData.DislikeInteractionCallback() {
            @Override
            public void onShow() {
                Log.d("lance", "onShow: ");
            }

            @Override
            public void onSelected(int position, String value, boolean enforce) {
                Log.d("lance", "onSelected: " + position + ":" + value + ":" + enforce);
                if (adContainer != null) {
                    adContainer.removeAllViews();
                }
            }

            @Override
            public void onCancel() {
                Log.d("lance", "onADExposed: ");
            }
        });
        //媒体最终将要展示广告的容器
        if (adContainer != null) {
            adContainer.removeAllViews();
            adContainer.addView(windContainer);
        }
    }
    

    
```

***原生自渲染广告销毁***

```
    //原生广告单元的销毁
    if (unifiedADDataList != null && unifiedADDataList.size() > 0) {
        for (NativeADData ad : unifiedADDataList) {
            if (ad != null) {
                ad.destroy();
            }
        }
    }
    //原生请求广告对象的销毁
    if (windNativeUnifiedAd != null) {
        windNativeUnifiedAd.destroy();
    }
     
```


***原生自渲染广告对象***

```
public interface NativeADData {

    String getCTAText();

    String getTitle();

    String getDesc();

    Bitmap getAdLogo();

    String getIconUrl();

    /**
     * 获取广告样式
     *
     * @return
     */
    int getAdPatternType();


    /**
     * 适配其他家sdk的自渲染广告
     *
     * @param context
     * @param adContainer SigMob提供的根容器
     * @param adRender    真正渲染广告的对象
     */
    void connectAdToView(Context context, WindNativeAdContainer adContainer, WindNativeAdRender adRender);


    /**
     * @param context
     * @param imageViews      展示广告的Images
     * @param defaultImageRes 渲染图片失败时默认的图片
     */
    void bindImageViews(Context context, List<ImageView> imageViews, int defaultImageRes);

    /**
     * @param context
     * @param view                  自渲染的根View
     * @param clickableViews        可点击的View的列表
     * @param creativeViewList      用于下载或者拨打电话的View
     * @param disLikeView           dislike按钮
     * @param nativeAdEventListener 点击回调
     */
    void bindViewForInteraction(Context context, View view, List<View> clickableViews, List<View> creativeViewList, View disLikeView, NativeADEventListener nativeAdEventListener);

    /**
     * @param context
     * @param mediaLayout           装video的容器
     * @param nativeADMediaListener video播放监听
     */
    void bindMediaView(Context context, ViewGroup mediaLayout, NativeADMediaListener nativeADMediaListener);

    void destroy();

    void startVideo();

    void pauseVideo();

    void resumeVideo();

    void stopVideo();

    /**
     * 信息流视频回调接口
     */
    interface NativeADMediaListener {

        //视频加载成功
        void onVideoLoad();

        //视频加载失败
        void onVideoError(WindAdError error);

        //视频开始播放
        void onVideoStart();

        //视频暂停播放
        void onVideoPause();

        //视频继续播放
        void onVideoResume();

        //视频完成播放
        void onVideoCompleted();
    }

    /**
     * 建议传当前activity，否则可能会影响dislike对话框弹出
     *
     * @param activity
     * @param dislikeInteractionCallback
     */
    void setDislikeInteractionCallback(Activity activity, DislikeInteractionCallback dislikeInteractionCallback);

    /**
     * dislike回调接口
     */
    interface DislikeInteractionCallback {
        void onShow();

        void onSelected(int position, String value, boolean enforce);

        void onCancel();
    }

}

```


## Mediation接入

***渠道支持列表***

Sigmob 做为聚合SDK目前支持开屏广告和视频广告，同时支持热插拔模式。我们为每个广告渠道提供了一个aar文件的适配器。

海外支持的渠道列表：

| 渠道名 | version | 适配器 |
| --- | --- | --- |
| Admob | 19.7.0 | sigmob-admob-adapters-19.7.0.aar |
| AppLovin | 9.12.0| sigmob-applovin-adapters-9.12.0.aar|
| Facebook | 5.8.0 | sigmob-facebookaudiencenetwork-adapters-5.8.0.aar |
| ironSource | 7.1.6.1 | sigmob-ironsource-adapters-7.1.6.1.aar |
| Mintegral | 15.5.01 | sigmob-mintegral-adapters-15.5.01.aar |
| Unityads | 3.7.1 | sigmob-unityads-adapters-3.7.1.aar |
| Vungle | 6.9.1 | sigmob-vungle-adapters-6.9.1.aar |
| 穿山甲 | 3.1.0.1 | sigmob-toutiao-gp-adapters-3.1.0.1.aar |
| Tapjoy| 12.7.0 | sigmob-tapjoy-adapters-12.7.0.aar |

国内支持的渠道列表：

| 渠道名 | version | 适配器 |
| --- | --- | --- |
| 穿山甲 | 3.9.0.7| sigmob-toutiao-adapters-3.9.0.7.aar |
| 腾讯优量汇 | 4.380.1250| sigmob-gdt-adapters-4.380.1250.aar |
| OneWay | 2.4.5| sigmob-oneway-adapters-2.4.5.aar |
| Mintegral中国区| 15.5.02 | sigmob-mintegral-cn-adapters-15.5.02.aar |
| 快手广告 | 3.3.12| sigmob-kuaishou-adapters-3.3.12.aar |
| Unityads | 3.7.1| sigmob-unityads-adapters-3.7.1.aar |
| Vungle| 6.9.1 | sigmob-vungle-adapters-6.9.1.aar |
| Tapjoy| 12.7.0 | sigmob-tapjoy-adapters-12.7.0.aar |


## 国内渠道集成接入

* 手工适配器依赖管理:需要将others-cn 拷贝到 app/libs

```
dependencies {

    implementation fileTree(include: ['*.jar','*.aar'], dir: 'libs/others-cn')

 }
```

###  腾讯优量汇 (仅中国大陆渠道)

* 更新 AndroidManiftest.xml


```
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

```

* Provider 定义

```
    <provider
        android:name="com.qq.e.comm.GDTFileProvider"
        android:authorities="${applicationId}.gdt.fileprovider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/gdt_file_path" />
    </provider>
```
* gdt_file_path.xml 内容

```
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <!-- 这个下载路径也不可以修改，必须为com_qq_e_download -->
    <external-cache-path
        name="gdt_sdk_download_path1"
        path="com_qq_e_download" />
    <cache-path
        name="gdt_sdk_download_path2"
        path="com_qq_e_download" />
</paths>
```

* Jar包手动依赖

```
dependencies {

    //腾讯优量汇
    implementation fileTree(include: ["*.jar","*.aar"], dir: 'libs/others-cn/gdt')

 }
```

### 穿山甲 (仅中国大陆渠道)

* 更新 AndroidManiftest.xml

```
     <!--必要权限-->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!--可选权限-->
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" tools:node="replace"/>
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
    <uses-permission android:name="android.permission.GET_TASKS"/>

    <!--可选，穿山甲提供“获取地理位置权限”和“不给予地理位置权限，开发者传入地理位置参数”两种方式上报用户位置，两种方式均可不选，添加位置权限或参数将帮助投放定位广告-->
    <!--请注意：无论通过何种方式提供给穿山甲用户地理位置，均需向用户声明地理位置权限将应用于穿山甲广告投放，穿山甲不强制获取地理位置信息-->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

    <!-- 如果有视频相关的广告且使用textureView播放，请务必添加，否则黑屏 -->
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <!-- 穿山甲3400版本新增：建议添加“query_all_package”权限，穿山甲将通过此权限在Android R系统上判定广告对应的应用是否在用户的app上安装，避免投放错误的广告，以此提高用户的广告体验。若添加此权限，需要在您的用户隐私文档中声明！ -->
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"/>

```

* Provider 定义

```
    <!--TT 改包名-->
    <provider
        android:name="com.bykv.vk.openvk.TTFileProvider"
        android:authorities="${applicationId}.TTFileProvider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/toutiao" />
    </provider>

    <!--TT 改包名-->
    <provider
        android:name="com.bykv.vk.openvk.multipro.TTMultiProvider"
        android:authorities="${applicationId}.TTMultiProvider"
        android:exported="false" />
```
* toutiao.xml 内容

```
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <!--为了适配所有路径可以设置 path = "." -->
    <external-path name="tt_external_root" path="." />
    <external-path name="tt_external_download" path="Download" />
    <external-files-path name="tt_external_files_download" path="Download" />
    <files-path name="tt_internal_file_download" path="Download" />
    <cache-path name="tt_internal_cache_download" path="Download" />
</paths>
```


* Jar包手动依赖

```
dependencies {

    //穿山甲
    implementation fileTree(include: ["*.jar","*.aar"], dir: 'libs/others-cn/toutiao')

 }
```
### OneWay  (仅中国大陆渠道)

* 更新 AndroidManiftest.xml

		<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
		<uses-permission android:name="android.permission.INTERNET" />
		<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
		<uses-permission android:name="android.permission.READ_PHONE_STATE" />
		<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
		<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />

* Activity 定义

		<activity
    		android:name="mobi.oneway.export.AdShowActivity"
    		android:configChanges="fontScale|keyboard|keyboardHidden|locale|mnc|mcc|navigation|orientation|screenLayout|screenSize|smallestScreenSize|uiMode|touchscreen"
    		android:hardwareAccelerated="true"
    		android:theme="@android:style/Theme.NoTitleBar.Fullscreen" />

* Provider 定义

		<provider
    		android:name="mobi.oneway.export.OWProvider"
    		android:authorities="${applicationId}.OWProvider"
    		android:exported="false"
    		android:grantUriPermissions="true">
    		<meta-data
        		android:name="android.support.FILE_PROVIDER_PATHS"
        		android:resource="@xml/ow_file_paths" />
		</provider>


* ow_file_paths.xml 内容

		<?xml version="1.0" encoding="utf-8"?>
		<resources>
    		<paths>
        		<!-- 
            		主要用于在 7.0 以上系统上调起安装。 apk 下载目录会依次尝试获取
            		系统下载目录(sdcard/Download) -> App 外部目录(files, cache) -> App 内部目录 (files, cache)
         		-->
        		<cache-path name="cache" path="" />
        		<external-cache-path name="exCache" path="" />
        		<external-files-path name="exFiles" path="" />
        		<external-path name="sdcard" path="" />
        		<files-path name="files" path="" />
    		</paths>
		</resources>



* Jar包手动依赖

		dependencies {

    		//OneWay
    		implementation fileTree(include: ["*.jar","*.aar"], dir: 'libs/others-cn/OneWay')
			//重要 2.4.5 使用 1.0.9版本!!
    		implementation 'mobi.oneway.common:core:1.0.9'

 		}

### Mintegral 渠道依赖（仅中国大陆渠道）


* 更新 AndroidManiftest.xml

```
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <!-- 如果中国大陆流量版本SDK ，以下两条权限必须加上 -->
    <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
    <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>

    <uses-library android:name="org.apache.http.legacy" android:required="false"/>

```

* Provider 定义

```
    //中国大陆流量版本必需，海外版本可以不添加。
    <provider
            android:name="com.mbridge.msdk.foundation.tools.MBFileProvider"
            android:authorities="${applicationId}.mbFileProvider"
            android:exported="false"
            android:grantUriPermissions="true" >
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/mb_provider_paths" />
    </provider>
```

* mb_provider_paths.xml 内容

```
    <?xml version="1.0" encoding="utf-8"?>
    <paths xmlns:android="http://schemas.android.com/apk/res/android">
     <external-path name="external_files" path="."/>
    </paths>
```

* Mintegral Maven自动依赖

```
dependencies {

    // 请将下方代码添加到项目的build.gradle中
    implementation 'com.mbridge.msdk.china:videojs:15.5.02'
    implementation 'com.mbridge.msdk.china:mbjscommon:15.5.02'
    implementation 'com.mbridge.msdk.china:playercommon:15.5.02'
    implementation 'com.mbridge.msdk.china:reward:15.5.02'
    implementation 'com.mbridge.msdk.china:videocommon:15.5.02'
    implementation 'com.mbridge.msdk.china:chinasame:15.5.02'
    implementation 'com.mbridge.msdk.china:interstitialvideo:15.5.02'
    implementation 'com.mbridge.msdk.china:interstitial:15.5.02'
    implementation 'com.mbridge.msdk.china:mbsplash:15.5.02'

    //mintegral provider 依赖androidx
    implementation 'androidx.legacy:legacy-support-v4:1.0.0'

 }
```

* Mintegral Jar及AAR 手动依赖

```
dependencies {

    //mintegral
    implementation fileTree(include: ["*.jar","*.aar"], dir: 'libs/others-cn/Mintegral-cn')

 }

```
* Mintegral 混淆配置

        -keepattributes Signature
        -keepattributes *Annotation*
        -keep class com.mbridge.** {*; }
        -keep interface com.mbridge.** {*; }
        -keep interface androidx.** { *; }
        -keep class androidx.** { *; }
        -keep public class * extends androidx.** { *; }
        -dontwarn com.mbridge.**
        -keep class **.R$* { public static final int mbridge*; }


### 快手广告 (仅中国大陆渠道)

* 更新 AndroidManiftest.xml

        <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
        <uses-permission android:name="android.permission.INTERNET" />
        <uses-permission android:name="android.permission.READ_PHONE_STATE" />
        <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
        <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
        <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
        <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
        <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
        <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
        <!--SDK内⾃定义的权限，与下载相关，aar中已经申请-->
        <permission
            android:name="${applicationId}.permission.KW_SDK_BROADCAST"
            android:protectionLevel="signature" />
        <uses-permission android:name="${applicationId}.permission.KW_SDK_BROADCAST"/>


* 如果您的应⽤启⽤了资源混淆或资源缩减，您需要保留SDK的资源，SDK的资源名都是以ksad_开头的。您可以在资源混淆配置⽂件添加如下配置：

		<?xml version="1.0" encoding="utf-8"?>
        <resources xmlns:tools="http://schemas.android.com/tools"
            tools:keep="@layout/ksad_*,@id/ksad_*,@style/ksad_*,
            @drawable/ksad_*,@string/ksad_*,@color/ksad_*,@attr/ksad_*,@dimen/ksad_*"/>

* Jar包手动依赖

		dependencies {

    		// 快⼿SDK aar包，请将提供的aar包拷⻉到libs⽬录下，添加依赖。根据接⼊版本修改SDK包名
    		implementation fileTree(include: ["*.jar","*.aar"], dir: 'libs/others-cn/KuaiShou')

            //建议使⽤的26以上的support库版本，建议使⽤28最新的即可。
            implementation "com.android.support:appcompat-v7:28.0.0"
            implementation "com.android.support:recyclerview-v7:28.0.0"

            // AndroidX依赖和上面Supprot库依赖，二选一
            implementation "androidx.appcompat:appcompat:$version"
            implementation "androidx.recyclerview:recyclerview:$version"
 		}

* 混淆配置

        -keep class com.kwad.sdk.** { *;}
        -keep class com.ksad.download.** { *;}
        -keep class com.kwai.filedownloader.** { *;}


### Tapjoy (仅中国大陆渠道)

* 更新 AndroidManiftest.xml

		<uses-permission android:name="android.permission.INTERNET"/>
        <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
        <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>

* Activity 定义

		<activity
            android:name="com.tapjoy.TJAdUnitActivity"
            android:configChanges="orientation|keyboardHidden|screenSize"
            android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen"
            android:hardwareAccelerated="true" />

        <activity
            android:name="com.tapjoy.TJContentActivity"
            android:configChanges="orientation|keyboardHidden|screenSize"
            android:theme="@android:style/Theme.Translucent.NoTitleBar" />


* Jar包手动依赖

	    dependencies {
            
            //Tapjoy
            implementation fileTree(include: ["*.jar","*.aar"], dir: 'libs/others-cn/Tapjoy')

            implementation 'com.google.android.gms:play-services-basement:17.0.0'
            implementation 'com.google.android.gms:play-services-ads-identifier:17.0.0'
 		}

* 作为Google Play服务器集成的一部分, 你需要添加如下：

        <meta-data
            android:name="com.google.android.gms.version"
            android:value="@integer/google_play_services_version"/>
       

* Tapjoy 混淆配置

        -keep class com.tapjoy.** { *; }
        -keep class com.moat.** { *; }
        -keepattributes JavascriptInterface
        -keepattributes *Annotation*
        -keep class * extends java.util.ListResourceBundle {
            protected Object[][] getContents();
        }
        -keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
            public static final *** NULL;
        }
        -keepnames @com.google.android.gms.common.annotation.KeepName class *
        -keepclassmembernames class * {
            @com.google.android.gms.common.annotation.KeepName *;
        }
        -keepnames class * implements android.os.Parcelable {
            public static final ** CREATOR;
        }
        -keep class com.google.android.gms.ads.identifier.** { *; }
        -dontwarn com.tapjoy.**  

### Vungle (仅中国大陆渠道)

* vungle Maven自动依赖

```
dependencies {

    //vungle
    implementation 'com.vungle:publisher-sdk-android:6.9.1'
    implementation "androidx.core:core:1.3.2"
    implementation "androidx.localbroadcastmanager:localbroadcastmanager:1.0.0"

 }

```
### unityAds (仅中国大陆渠道)

* unityAds 拷贝others-cn/UnityAds/unity-ads-*.aar 到libs目录，手动增加依赖

```
dependencies {

    //unityAds请将提供的aar包拷⻉到libs⽬录下，添加依赖。
    implementation fileTree(include: ["*.jar","*.aar"], dir: 'libs/others-cn/UnityAds')

 }

```



## 海外渠道集成

### SigMob

***更新 AndroidManifest.xml权限声明***

```
<manifest>

    <!-- SDK所需要权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

     <!-- 可选 -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

</manifest>

```

***广告展示Activity 声明***
```
<manifest>

    <application>

        <!--广告展示Activity -->
        <activity
            android:name="com.wind.sdk.base.common.AdActivity"
            android:configChanges="keyboard|keyboardHidden|orientation|screenSize"
            android:theme="@style/sig_transparent_style" />

    </application>

</manifest>
```


* maven 自动适配器依赖

```
dependencies {

    //rewardVideoAdapter
    implementation fileTree(include: ['sigmob-*.aar'], dir: 'libs/adapterLibs')

 }

```

### 穿山甲 海外渠道依赖

* provider 定义

```

  <provider

    android:name="com.bytedance.sdk.openadsdk.multipro.TTMultiProvider"
    android:authorities="${applicationId}.TTMultiProvider"
    android:exported="false" />


```

* 拷贝open_ad_sdk_*.aar 到libs目录，增加依赖

```
dependencies {

    implementation fileTree(dir: 'libs', include: ['open_ad_sdk_*.aar'])
 }

```


### Admob 渠道依赖

* 请根据自己申请admob的APPLICATION_ID进行修改

```

  <application>
   <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="ca-app-pub-3940256099942544~3347511713"/>

  </application>

```


* Admob Maven自动依赖

```
dependencies {

    //admob
    implementation "com.google.android.gms:play-services-ads:19.7.0"
    implementation 'com.google.android.ads.consent:consent-library:1.0.8'
 }

```


### applovin 渠道依赖

* applovin Maven自动依赖


```
dependencies {

    //applovin
    implementation "com.applovin:applovin-sdk:9.12.0"

 }

```

### Tapjoy 渠道依赖

* 更新 AndroidManiftest.xml

		<uses-permission android:name="android.permission.INTERNET"/>
        <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
        <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>

* Activity 定义

		<activity
            android:name="com.tapjoy.TJAdUnitActivity"
            android:configChanges="orientation|keyboardHidden|screenSize"
            android:theme="@android:style/Theme.Translucent.NoTitleBar.Fullscreen"
            android:hardwareAccelerated="true" />

        <activity
            android:name="com.tapjoy.TJContentActivity"
            android:configChanges="orientation|keyboardHidden|screenSize"
            android:theme="@android:style/Theme.Translucent.NoTitleBar" />


* 拷贝tapjoyconnectlibrary-*.aar 到libs目录，增加依赖

		dependencies {

    	    //Tapjoy
            implementation (name: 'tapjoyconnectlibrary-12.7.0', ext: 'jar')
            
            implementation 'com.google.android.gms:play-services-basement:17.0.0'
            implementation 'com.google.android.gms:play-services-ads-identifier:17.0.0'

 		}


* 作为Google Play服务器集成的一部分, 你需要添加如下：

        <meta-data
            android:name="com.google.android.gms.version"
            android:value="@integer/google_play_services_version"/>

* Tapjoy 混淆配置

        -keep class com.tapjoy.** { *; }
        -keep class com.moat.** { *; }
        -keepattributes JavascriptInterface
        -keepattributes *Annotation*
        -keep class * extends java.util.ListResourceBundle {
            protected Object[][] getContents();
        }
        -keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
            public static final *** NULL;
        }
        -keepnames @com.google.android.gms.common.annotation.KeepName class *
        -keepclassmembernames class * {
            @com.google.android.gms.common.annotation.KeepName *;
        }
        -keepnames class * implements android.os.Parcelable {
            public static final ** CREATOR;
        }
        -keep class com.google.android.gms.ads.identifier.** { *; }
        -dontwarn com.tapjoy.**      

### vungle 渠道依赖
* vungle Maven自动依赖


```
dependencies {

    //vungle
    implementation 'com.vungle:publisher-sdk-android:6.9.1'
    implementation "androidx.core:core:1.3.2"
    implementation "androidx.localbroadcastmanager:localbroadcastmanager:1.0.0"

    // Recommended for SDK to be able to get Android Advertising ID
    implementation 'com.google.android.gms:play-services-basement:17.4.0'
    implementation 'com.google.android.gms:play-services-ads-identifier:17.0.0'

 }

```

### facebook 渠道依赖

* facebook  maven自动依赖

```
dependencies {

    //facebook
    implementation "com.facebook.android:audience-network-sdk:5.8.0"


 }

```


### unityAds 渠道依赖

* unityAds 拷贝others/UnityAds/unity-ads-*.aar 到libs目录，手动增加依赖

```
dependencies {

    //unityAds
    implementation(name: 'unity-ads-3.7.1', ext: 'aar')

 }

```

### ironsource 渠道依赖

* ironsource Maven自动依赖


```
dependencies {

    //ironsource
    implementation "com.ironsource.sdk:mediationsdk:7.1.6.1"

 }

```

### Mintegral 渠道依赖

* 更新 AndroidManiftest.xml

```
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

```

* Mintegral Maven自动依赖

```
dependencies {

    // 请将下方代码添加到项目的build.gradle中
    implementation 'com.mbridge.msdk.oversea:videojs:15.5.01'
    implementation 'com.mbridge.msdk.oversea:mbjscommon:15.5.01'
    implementation 'com.mbridge.msdk.oversea:playercommon:15.5.01'
    implementation 'com.mbridge.msdk.oversea:reward:15.5.01'
    implementation 'com.mbridge.msdk.oversea:videocommon:15.5.01'
    implementation 'com.mbridge.msdk.oversea:same:15.5.01'
    implementation 'com.mbridge.msdk.oversea:interstitialvideo:15.5.01'
    implementation 'com.mbridge.msdk.oversea:interstitial:15.5.01'
    implementation 'com.mbridge.msdk.oversea:mbsplash:15.5.01'

 }

```

* Mintegral 混淆配置

        -keepattributes Signature
        -keepattributes *Annotation*
        -keep class com.mbridge.** {*; }
        -keep interface com.mbridge.** {*; }
        -keep interface androidx.** { *; }
        -keep class androidx.** { *; }
        -keep public class * extends androidx.** { *; }
        -dontwarn com.mbridge.**
        -keep class **.R$* { public static final int mbridge*; }


### 海外接入全局设置信息(GDPR授权支持,COPPA授权支持)
> 海外接入如果为欧盟区域请在初始化前请通过接口setUserGDPRConsentStatus设置GDPR相关授权状态。
> 如若涉及儿童隐私保护相关协议，请通过接口setIsAgeRestrictedUser设置是否为受限制用户。


####  GDPR授权支持（仅针对海外市场）

```
        WindAds ads = WindAds.sharedAds();


        /*  欧盟 GDPR 支持
        **  WindConsentStatus 值说明:
        **     UNKNOW("0"),  //未知,默认值，根据服务器判断是否在欧盟区，若在欧盟区则判断为拒绝GDPR授权
        **     ACCEPT("1"),  //用户同意GDPR授权
        **     DENIED("2");  //用户拒绝GDPR授权
        */

        ads.setUserGDPRConsentStatus(WindConsentStatus.ACCEPT);

```
####  COPPA授权支持（仅针对海外市场）

```
        WindAds ads = WindAds.sharedAds();

        ads.setUserGDPRConsentStatus(WindConsentStatus.ACCEPT);

        /*   COPPA 支持
        *    WindAgeRestictedUserStatus
        *            WindAgeRestrictedStatusUNKNOWN 未知，默认值
        *            WindAgeRestrictedStatusNO 不限制
        *            WindAgeRestrictedStatusYES 有限制
        *    setUserAge 设置用户年龄
        */
        ads.setIsAgeRestrictedUser(true);
        ads.setUserAge(18);

```

###  SDK 初始化启动


```
    WindAds ads = WindAds.sharedAds();

    //useMediation:true代表使用聚合服务;false:代表单接SigMob
    // start SDK Init with Options（聚合,必须传入actvity，否则聚合Ironsource,UnityAds,Mintegral,平台无法加载广告）
    ads.startWithOptions(activity, new WindAdOptions(YOU_APP_ID, YOU_APP_KEY,USE_MEDIATION));//是否使用聚合服务
```


## 集成常见问题

### targetSdkVersion 28以上 http支持

```
<manifest>
<application android:usesCleartextTraffic="true">
</manifest>

或者
</manifest>
<application android:networkSecurityConfig="@xml/network_security_config">
</manifest>

network_security_config.xml 文件配置

<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true"/>
</network-security-config>

```

### 多进程支持说明
> 广告AdActivity 增加 android:multiprocess="true"，此方案每次开启子进程后你需要重新对广告SDK进行初始化，主进程加载广告状态在次进程无效，需要重新加载，设置回调。


### 系统installProvider失败或者开启MultiDex导致的Not find Class
> 参考链接 <https://developer.android.com/studio/build/multidex>

```
    android {
        buildTypes {
            release {
                multiDexKeepProguard file('multidex-config.pro')
                ...
            }
        }
    }
```

### 方法数65k问题解决

> 参考链接 <https://developer.android.com/studio/build/multidex>


####   build.gradle defaultConfig 开启multiDexEnabled

```
    defaultConfig {

        multiDexEnabled true
    }

```

####   build.gradle dependencies 增加 multidex

```

    dependencies {

         //AndroidX
         def multidex_version = "2.0.1"
         implementation 'androidx.multidex:multidex:$multidex_version'

         //非AndroidX
         implementation 'com.android.support:multidex:1.0.3'
    }

```

####   修改 MyAppcation 继承 MultiDexApplication

```

public class MyAppcation extends MultiDexApplication {

  override fun attachBaseContext(base: Context) {
            super.attachBaseContext(base)
            MultiDex.install(this) //Multi 安装
        }
}

```


### AndroidX与Android support 冲突

* 在项目根目录下文件gradle.properties，添加以下代码

```

android.enableJetifier=true
android.useAndroidX=true

```

### 聚合Ironsource,UnityAds,Mintegral 无法初始化

> 如果聚合配置了Ironsource,UnityAds,Mintegral，必须使用loadAd(activity, adrequest) 这个接口，且第一个参数必须必须传入
Activity对象


### 常见错误码及流程常见问题说明

* Q: user id 是什么值，是否可以传null？

> A: userId是应用为注册用户分配的id与设备id无关，正确传入这个id可以更好的优化广告效果，提高媒体的收入，如果没有可以传入null
* Q: 广告请求为什么收到广告无填充（200000)？
> A: 具体原因根据一下情况检查:
   > 1. 开屏广告请检查应用是否为竖屏应用，我们暂时不对横屏应用填充开屏广告.
   > 2. 已经上线则可能是目前广告填充率不足，建议多尝试。
   > 3. 提供设备ID，APPID 及广告位参数，联系我们的商务人员或者技术人员。

* Q: 广告请求为什么收到请求的app已经关闭广告服务（500420)？
> A: 具体原因根据一下情况检查:
   > 1. 未上线应用可以通过添加测试设备IMEI/GAID/OAID拉去测试广告.
   > 2. 检查获取IMEI权限是否授权，我们是依赖应用授权，SDK不会主动授权，具体授权代码可参考Demo源码
   > 3. 已上线应用请检查应用状态或者广告位状态

* Q: 广告请求为什么收到请求参数缺少设备信息（500424/500422)？
> A: 检查获取IMEI权限是否授权，我们是依赖应用授权，SDK不会主动授权，具体授权代码可参考Demo源码

* Q: 广告请求为什么收到错误的广告位信息（500432)？
> A: 我们广告位与广告类型和APPID 必须对应，请检查appid 或者请求的广告类型是否有广告位类型匹配

* Q: 广告请求为什么收到设备的操作系统类型，与请求的app的系统类型不匹配（500435)？
> A: APPID 只对应相应的系统平台，不能混合使用。

* Q: 广告请求为什么收到请求的app不存在（500473)？
> A: 请检查appid 是否填写错误。

* Q: 广告请求为什么收到app未设置聚合策略（500700/500701)？
> A: 请在媒体变现平台应用管理- 渠道列表开启广告渠道

* Q: 广告请求为什么收到广告请求出错（600101)？
> A: 此情况是API渠道无填充，但原因比较复杂，服务器返回的错误在SDK无法对应错误码，需要先根据appid，广告位，设备id查询阿里云日志，查询请求日志。

* Q: 广告请求为什么收到未找到该渠道的适配器（600102)？
> A: 请添加混淆配置，集成文档有对应说明

* Q: 广告请求为什么收到文件下载错误（600104)？
> A: 一般是网络问题，特殊情况可能是下载链接，或者Video Md5（如果有） 与下载的md5 不匹配


* Q: 广告请求为什么收到文件下载错误（610005)？
> A: Endcard（MD5）或者Video Md5（如果有） 与下载的md5不匹配

* Q: 广告请求为什么收到文件下载错误（600104)？
> A: 一般是网络问题，特殊情况可能是下载链接失效。Video Md5（如果有） 与下载的md5不匹配

* Q: 点击游戏下载，没有反应？
> A: 一般是provider没配置，需要引入Android-support-v4，极端情况下是游戏过大，磁盘没有下载空间。

* Q: Android SDK是否支持多进程？
> A: 我们支持多进程，具体使用参考集成说明文档关于多进程说明。

* Q: 点击播放广告没有反应或者收到播放失败？
> A: 请按照集成说明文档中ADActivity 添加说明

* Q: Android 5.0以下部分机器视频无法播放？
> A: 此情况可能是手机编码器不支持H264 baseline 解码器导致。

* Q: 部分广告无法播放？
> A: 请检查是否添加了对Http的支持，具体支持方式参考集成说明文档。

* Q: 我们SDK集成后的大小有多少？
> A: 集成后的安装包大小增加300KB左右，Jar包大小为800多KB

* Q: 我们SDK是否支持GDPR？
> A: 我们支持GDPR，具体使用请参考集成说明文档关于GDPR说明。

* Q: Android SDK 是否支持Android Q(10)？
> A: Android 10 无法获取IMEI，需要设备已经集成GMS服务或者开发者集成 OAID SDK。

* Q: 请求视频广告，isReady检查返回False/无法播放广告？
> A: isReady检查返回False/无法播放广告必须收到loadSuccess之后

* Q: 请求播放后，无法再播放广告？
> A: 我们SDK每次播放必须有对应的load请求，一次请求广告，不能播放两次。

* Q: 广告请求加载时长多久？
> A: 这个根据网络情况，一般是3-5秒完成，网络比较差可能会比较慢或者加载超时，建议合理增加播放与预加载调用间隔。

* Q: 开屏广告没有播放方法？
> A: 开屏广告不需要播放方法，广告请求会自动播放。

* Q: 开屏广告fetchTime 一般是多少？
> A: 一般是3-5秒，建议5秒。

* Q: Android 10 国内如何填充广告？
> A: 建议集成MSA联盟的OAID生成SDK， 具体集成见 http://www.msa-alliance.cn/col.jsp?id=120。

* Q: AndroidX与Android Support 兼容问题？
> A: 移除Android Support 依赖库， 修改项目 gradle.properties 文件 添加 android.enableJetifier=true  和 android.useAndroidX=true

* Q: SDK 是否支持AndroidX？
> A: 支持，国内版本 AndroidX 的Priovder name 定义: SigmobXFileProivder，国内版本 Android Support 的Priovder name 定义: SigmobFileProivder


## 错误码

> 相关错误信息可参考此翻译表。

| Error Code | Error Message | 备注 |
| --- | --- | --- |
| 500402  | 该地区无sigmob广告渠道服务 |
| 500420  | 请求的app已经关闭广告服务 |
| 500422 | 请求参数缺少设备信息 |
| 500424 | 缺少设备id相关信息 |
| 500428 | 缺少广告为信息 |
| 500430 | 错误的广告位信息 |
| 500432 | 广告位不存在，或者appid与广告位不匹配 |
| 500433 | 广告位不存在或是已关闭 |
| 500435 | 设备的操作系统类型，与请求的app的系统类型不匹配 |
| 500436 | 广告单元id与请求的广告类型不匹配 |
| 500473 | 请求的app不存在 |
| 500700 | app未设置聚合策略 |
| 500701 | app未开通任何广告渠道 |
| 200000 | 无广告填充 |
| 600100 | 网络出错 |
| 600101 | 请求出错 | 该code会包含多种错误场景，具体原因会在userInfo中以Json形式进行更细致的描述(code&message) |
| 600102 | 未找到该渠道的适配器 |
| 600103 | 配置的策略为空 |
| 600104 | 文件下载错误 |
| 600105 | 下载广告超时 |
| 600106 | User GDPR Consent Status is denied |
| 600107 | 插入数据库失败 |
| 600108 | protoBuf协议解析出错 |
| 600900 | SDK未初始化 |
| 600901 | 广告位为空 |
| 600902 | 策略请求失败 |
| 600903 | 安装失败 |
| 610002 | 激励视频播放出错 |
| 610003 | 激励视频广告未准备好 |
| 610004 | server下发的广告信息缺失关键信息 |
| 610005 | 下载的文件校验md5出错 |
| 610006 | 激励视频播接口检查出错（广告过期或者未ready) |
| 610006 | 激励视频播接口检查出错（广告过期或者未ready) |
| 620001 | 开屏广告加载超时 |
| 620002 | 开屏广告不支持当前方向 |
| 620900 | 开屏广告被阻拦 AD BLOCK |

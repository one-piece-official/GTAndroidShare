package com.wind.common;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

public class MyApplication extends MultiDexApplication {


    @Override
    public void onCreate() {
        super.onCreate();

//        CrashReport.initCrashReport(getApplicationContext(), "4c41e5eed0", true);//4c41e5eed0//4ee13aff7b
//        initSDK();
    }

//    private void initSDK() {
//        WindAds ads = WindAds.sharedAds();
//
//        //enable or disable debug log
//
//        SharedPreferences sharedPreferences = getSharedPreferences("setting", 0);
//        String appId = sharedPreferences.getString(CONF_APPID, APP_ID);
//        String appKey = sharedPreferences.getString(CONF_APPKEY, "");
//
//        ads.setIsAgeRestrictedUser(WindAgeRestrictedUserStatus.NO);
//        ads.setUserAge(18);
//        boolean isUseMediation = sharedPreferences.getBoolean(USE_MEDIATION, false);
//        WindAdOptions options = new WindAdOptions(appId, appKey, isUseMediation);
//        ads.startWithOptions(this, options,
//                new WindAdConfig.Builder().
//                        customController(new WindCustomController() {
//                            @Override
//                            public boolean isCanUseLocation() {
//                                return super.isCanUseLocation();
//                            }
//
//                            @Override
//                            public Location getLocation() {
//                                return getAppLocation();
//                            }
//
//                            @Override
//                            public boolean isCanUsePhoneState() {
//                                return false;
//                            }
//
//                            @Override
//                            public String getDevImei() {
//                                return "1234567890";
//                            }
//
//                            @Override
//                            public String getDevOaid() {
//                                return super.getDevOaid();
//                            }
//                        }).build());
//
////        WindAds.sharedAds().getWindAdConfig().getCustomController();
//    }


    // Called before we send every request.
    private Location getAppLocation() {
        Location lastLocation = null;

        try {
            if (this.checkCallingOrSelfPermission("android.permission.ACCESS_FINE_LOCATION") == PackageManager.PERMISSION_GRANTED
                    || this.checkCallingOrSelfPermission("android.permission.ACCESS_COARSE_LOCATION") == PackageManager.PERMISSION_GRANTED) {
                // Get lat, long failFrom any GPS information that might be currently
                // available
                LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

                for (String provider_name : lm.getProviders(true)) {
                    Location l = lm.getLastKnownLocation(provider_name);
                    if (l == null) {
                        continue;
                    }

                    if (lastLocation == null) {
                        lastLocation = l;
                    } else {
                        if (l.getTime() > 0 && lastLocation.getTime() > 0) {
                            if (l.getTime() > lastLocation.getTime()) {
                                lastLocation = l;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lastLocation;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        MultiDex.install(this);

    }
}

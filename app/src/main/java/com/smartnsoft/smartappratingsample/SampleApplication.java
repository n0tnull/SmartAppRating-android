package com.smartnsoft.smartappratingsample;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.smartnsoft.smartapprating.RemoteConfigFactory;
import com.smartnsoft.smartapprating.SmartAppRatingManager;
import com.smartnsoft.smartapprating.SmartAppRatingManager.Builder;
import com.smartnsoft.smartapprating.bo.Configuration;

/**
 * @author Adrien Vitti
 * @since 2018.01.31
 */

public final class SampleApplication
    extends Application
{

  private static SmartAppRatingManager ratingManager;

  public static SmartAppRatingManager getRatingManager()
  {
    return ratingManager;
  }

  @Override
  public void onCreate()
  {
    super.onCreate();

    ratingManager = new Builder(this)
        .setIsInDevelopmentMode(BuildConfig.DEBUG)
        .setApplicationId(BuildConfig.APPLICATION_ID)
        .setRatePopupActivity(AnimatedSmartAppRatingActivity.class)
        .setApplicationVersionName(BuildConfig.VERSION_NAME)
        .setFallbackConfiguration(new Configuration())
//        .setFactory(new JsonConfigFactory("https://next.json-generator.com/", "api/json/get/4yBX9X0CN"))
        .setFactory(new RemoteConfigFactory())
        .build();

    SmartAppRatingManager.setUncaughtExceptionHandler(this, Thread.getDefaultUncaughtExceptionHandler());
    final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
    SmartAppRatingManager.increaseNumberOfSession(sharedPreferences);

    //    smartAppRatingManager.fetchConfigurationAndTryToDisplayPopup();
    //    smartAppRatingManager.fetchConfiguration();
    //    smartAppRatingManager.showRatePopup();
  }
}

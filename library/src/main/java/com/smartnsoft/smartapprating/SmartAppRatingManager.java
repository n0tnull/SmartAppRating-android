package com.smartnsoft.smartapprating;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.smartnsoft.smartapprating.bo.Configuration;
import com.smartnsoft.smartapprating.ws.SmartAppRatingServices;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * @author Adrien Vitti
 * @since 2018.01.29
 */
@SuppressWarnings("unused")
public final class SmartAppRatingManager
{

  public static class Builder
  {

    private boolean isInDevelopmentMode;

    private String baseURL;

    private String configurationFilePath;

    private File cacheDirectory = null;

    @NonNull
    private Context context;

    @Nullable
    private Class<? extends SmartAppRatingActivity> ratePopupActivity;

    private int cacheSize;

    private String applicationId;

    private String applicationVersionName;

    public Builder(@NonNull Context context)
    {
      this.context = context;
    }

    public Builder setIsInDevelopmentMode(boolean isInDevelopmentMode)
    {
      this.isInDevelopmentMode = isInDevelopmentMode;
      return this;
    }

    public Builder setConfigurationFileURL(@NonNull final String baseURL, @NonNull final String configurationFilePath)
    {
      this.baseURL = baseURL;
      this.configurationFilePath = configurationFilePath;
      return this;
    }

    public Builder setRatePopupActivity(@NonNull Class<? extends SmartAppRatingActivity> ratePopupActivity)
    {
      this.ratePopupActivity = ratePopupActivity;
      return this;
    }

    public Builder setCachePolicy(@NonNull File cacheDirectory, @IntRange(from = 1024 * 1024) int cacheSize)
    {
      this.cacheDirectory = cacheDirectory;
      this.cacheSize = cacheSize;
      return this;
    }

    public Builder setApplicationId(@NonNull String applicationId)
    {
      this.applicationId = applicationId;
      return this;
    }

    public Builder setApplicationVersionName(@NonNull String applicationVersionName)
    {
      this.applicationVersionName = applicationVersionName;
      return this;
    }

    public SmartAppRatingManager build()
    {
      if (TextUtils.isEmpty(applicationId))
      {
        throw new IllegalStateException("Unable to create the app rating manager because the application ID was not set");
      }
      else if (TextUtils.isEmpty(applicationVersionName))
      {
        throw new IllegalStateException("Unable to create the app rating manager because the application ID was not set");
      }

      final SmartAppRatingManager smartAppRatingManager = new SmartAppRatingManager(context, applicationId, applicationVersionName, baseURL, configurationFilePath, cacheDirectory, cacheSize);
      smartAppRatingManager.isInDevelopmentMode = isInDevelopmentMode;
      if (ratePopupActivity != null)
      {
        smartAppRatingManager.setRatingPopupActivityClass(ratePopupActivity);
      }
      return smartAppRatingManager;
    }
  }

  public static void setRateLaterTimestamp(@NonNull SharedPreferences preferences, long updateLaterTimestamp)
  {
    preferences.edit().putLong(SmartAppRatingManager.LAST_RATE_POPUP_CLICK_ON_LATER_TIMESTAMP_PREFERENCE_KEY, updateLaterTimestamp).apply();
  }

  private static long getLastCrashTimestamp(@NonNull SharedPreferences preferences)
  {
    return preferences.getLong(SmartAppRatingManager.LAST_CRASH_TIMESTAMP_PREFERENCE_KEY, -1);
  }

  private static long getRateLaterTimestamp(@NonNull SharedPreferences preferences)
  {
    return preferences.getLong(SmartAppRatingManager.LAST_RATE_POPUP_CLICK_ON_LATER_TIMESTAMP_PREFERENCE_KEY, -1);
  }

  public static void increaseNumberOfSession(@NonNull SharedPreferences preferences)
  {
    setNumberOfSession(preferences, getNumberOfSession(preferences) + 1);
  }

  public static void setNumberOfSession(@NonNull SharedPreferences preferences, long newNumberOfSession)
  {
    preferences.edit().putLong(SmartAppRatingManager.NUMBER_OF_SESSION_PREFERENCE_KEY, newNumberOfSession).apply();
  }

  private static long getNumberOfSession(@NonNull SharedPreferences preferences)
  {
    return preferences.getLong(SmartAppRatingManager.NUMBER_OF_SESSION_PREFERENCE_KEY, 0);
  }

  private static final String LAST_RATE_POPUP_CLICK_ON_LATER_TIMESTAMP_PREFERENCE_KEY = "smartAppRating_lastRateAppPopupClickOnLaterTimestamp";

  private static final String LAST_CRASH_TIMESTAMP_PREFERENCE_KEY = "smartAppRating_lastCrashTimestamp";

  private static final String NUMBER_OF_SESSION_PREFERENCE_KEY = "smartAppRating_numberOfSession";

  private static final long DAY_IN_MILLISECONDS = 24 * 60 * 60 * 1000;

  private static final String TAG = "SmartAppRatingManager";

  private final Context applicationContext;

  private final String configurationFilePath;

  @NonNull
  private final String applicationId;

  @NonNull
  private final String applicationVersionName;

  private boolean isInDevelopmentMode;

  private Configuration configuration;

  private final SmartAppRatingServices smartAppRatingServices;

  private Class<? extends SmartAppRatingActivity> ratingPopupActivityClass = SmartAppRatingActivity.class;

  SmartAppRatingManager(@NonNull Context context,
      @NonNull final String applicationId, @NonNull final String applicationVersionName, @NonNull final String baseURL,
      @NonNull final String configurationFilePath, @Nullable File cacheDirectory,
      int cacheSize)
  {
    this.applicationContext = context.getApplicationContext();
    this.applicationId = applicationId;
    this.applicationVersionName = applicationVersionName;
    this.configurationFilePath = configurationFilePath;
    this.smartAppRatingServices = SmartAppRatingServices.get(baseURL, cacheDirectory, cacheSize);
  }

  public static void setUncaughtExceptionHandler(final Context context,
      @NonNull final UncaughtExceptionHandler defaultHandler)
  {
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
    {
      @Override
      public void uncaughtException(Thread thread, Throwable throwable)
      {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putLong(SmartAppRatingManager.LAST_CRASH_TIMESTAMP_PREFERENCE_KEY, System.currentTimeMillis()).apply();
        defaultHandler.uncaughtException(thread, throwable);
      }
    });
  }

  void setRatingPopupActivityClass(Class<? extends SmartAppRatingActivity> ratingPopupActivityClass)
  {
    this.ratingPopupActivityClass = ratingPopupActivityClass;
  }

  public void fetchConfigurationAndTryToDisplayPopup()
  {
    if (isInDevelopmentMode)
    {
      Log.d(TAG, "fetching configuration...");
    }
    this.smartAppRatingServices.getConfiguration(configurationFilePath, new Callback<Configuration>()
    {

      @Override
      public void onResponse(@NonNull Call<Configuration> call, @NonNull Response<Configuration> response)
      {
        if (response.isSuccessful())
        {
          configuration = response.body();
          showRatePopup();
        }
        else
        {
          if (isInDevelopmentMode)
          {
            Log.w(TAG, "Failed to retrieve configuration file : HTTP error code = " + response.code());
          }
        }
      }

      @Override
      public void onFailure(@NonNull Call<Configuration> call, @NonNull Throwable t)
      {
        if (isInDevelopmentMode)
        {
          Log.w(TAG, "Failed to retrieve configuration file", t);
        }
      }
    });
  }

  public void fetchConfiguration()
  {
    if (isInDevelopmentMode)
    {
      Log.d(TAG, "fetching configuration...");
    }
    this.smartAppRatingServices.getConfiguration(configurationFilePath, new Callback<Configuration>()
    {

      @Override
      public void onResponse(@NonNull Call<Configuration> call, @NonNull Response<Configuration> response)
      {
        if (response.isSuccessful())
        {
          configuration = response.body();
        }
        else
        {
          if (isInDevelopmentMode)
          {
            Log.w(TAG, "Failed to retrieve configuration file : HTTP error code = " + response.code());
          }
        }
      }

      @Override
      public void onFailure(@NonNull Call<Configuration> call, @NonNull Throwable t)
      {
        if (isInDevelopmentMode)
        {
          Log.w(TAG, "Failed to retrieve configuration file", t);
        }
      }
    });
  }

  public void showRatePopup()
  {
    final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);
    if (configuration != null
        && configuration.isRateAppDisabled == false
        && (configuration.minimumTimeGapAfterACrashInDays > 0 && getLastCrashTimestamp(sharedPreferences) + (configuration.minimumTimeGapAfterACrashInDays * SmartAppRatingManager.DAY_IN_MILLISECONDS) < System.currentTimeMillis())
        && (configuration.minimumTimeGapBeforeAskingAgainInDays > 0 && getRateLaterTimestamp(sharedPreferences) + (configuration.minimumTimeGapBeforeAskingAgainInDays * SmartAppRatingManager.DAY_IN_MILLISECONDS) < System.currentTimeMillis())
        && (configuration.numberOfSessionBeforeAskingToRate > 0 && configuration.numberOfSessionBeforeAskingToRate <= SmartAppRatingManager.getNumberOfSession(sharedPreferences))
        )
    {
      if (isInDevelopmentMode)
      {
        Log.d(TAG, "Try to display the rating popup");
      }
      configuration.versionName = applicationVersionName;
      configuration.applicationID = applicationId;
      final Intent intent = new Intent(applicationContext, ratingPopupActivityClass);
      intent.putExtra(SmartAppRatingActivity.CONFIGURATION_EXTRA, configuration);
      applicationContext.startActivity(intent);
    }
  }

}

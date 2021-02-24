package com.parishod.watomatic.model.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.parishod.watomatic.R;
import com.parishod.watomatic.model.App;
import com.parishod.watomatic.model.utils.Constants;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PreferencesManager {
    private final String KEY_SERVICE_ENABLED = "pref_service_enabled";
    private final String KEY_GROUP_REPLY_ENABLED = "pref_group_reply_enabled";
    private final String KEY_AUTO_REPLY_THROTTLE_TIME_MS = "pref_auto_reply_throttle_time_ms";
    private final String KEY_SELECTED_APPS_ARR = "pref_selected_apps_arr";
    private static PreferencesManager _instance;
    private SharedPreferences _sharedPrefs;
    private Context thisAppContext;

    private PreferencesManager(Context context) {
        thisAppContext = context;
        _sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        init();
    }

    public static PreferencesManager getPreferencesInstance(Context context){
        if(_instance == null){
            _instance = new PreferencesManager(context.getApplicationContext());
        }
        return _instance;
    }

    /**
     * Execute this code when the singleton is first created. All the tasks that needs to be done
     * when the instance is first created goes here. For example, set specific keys based on new install
     * or app upgrade, etc.
     */
    private void init () {
        // For new installs, enable all the supported apps
        if (Constants.BETA_FACEBOOK_SUPPORT_ENABLED) {
            boolean newInstall = !_sharedPrefs.contains(KEY_SERVICE_ENABLED)
                    && !_sharedPrefs.contains(KEY_SELECTED_APPS_ARR);
            if (newInstall) {
                setAppsAsEnabled(Constants.SUPPORTED_APPS);
            }
        }
    }

    public boolean isServiceEnabled(){
        return _sharedPrefs.getBoolean(KEY_SERVICE_ENABLED,false);
    }

    public void setServicePref(boolean enabled){
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putBoolean(KEY_SERVICE_ENABLED, enabled);
        editor.apply();
    }

    public boolean isGroupReplyEnabled(){
        return _sharedPrefs.getBoolean(KEY_GROUP_REPLY_ENABLED,false);
    }

    public void setGroupReplyPref(boolean enabled){
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putBoolean(KEY_GROUP_REPLY_ENABLED, enabled);
        editor.apply();
    }

    public long getAutoReplyDelay(){
        return _sharedPrefs.getLong(KEY_AUTO_REPLY_THROTTLE_TIME_MS,0);
    }

    public void setAutoReplyDelay(long delay){
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putLong(KEY_AUTO_REPLY_THROTTLE_TIME_MS, delay);
        editor.apply();
    }

    public Set<String> getEnabledApps(){
        String enabledAppsJsonStr = _sharedPrefs.getString(KEY_SELECTED_APPS_ARR, null);

        // Users upgrading from v1.7 and before
        // For upgrading users, preserve functionality by enabling only WhatsApp
        //   (remove this when time most users would have updated. May be in 3 weeks after deploying this?)
        if (enabledAppsJsonStr == null) {
            enabledAppsJsonStr = setAppsAsEnabled(Collections.singleton(new App("WhatsApp", "com.whatsapp")));
        }

        Type type = new TypeToken<Set<String>>(){}.getType();
        return new Gson().fromJson(enabledAppsJsonStr, type);
    }

    public boolean isAppEnabled (App thisApp) {
        return getEnabledApps().contains(thisApp.getPackageName());
    }

    private String serializeAndSetEnabledPackageList (Collection<String> packageList) {
        String jsonStr = new Gson().toJson(packageList);
        SharedPreferences.Editor editor = _sharedPrefs.edit();
        editor.putString(KEY_SELECTED_APPS_ARR, jsonStr);
        editor.apply();
        return jsonStr;
    }

    public String setAppsAsEnabled (Collection<App> apps) {
        Set<String> packageNames = new HashSet<>();
        for (App app: apps) {
            packageNames.add(app.getPackageName());
        }
        return serializeAndSetEnabledPackageList(packageNames);
    }

    public String saveEnabledApps(App app, boolean isSelected){
        Set<String> enabledPackages = getEnabledApps();
        if(!isSelected) {
            //remove the given platform
            enabledPackages.remove(app.getPackageName());
        }else{
            //add the given platform
            enabledPackages.add(app.getPackageName());
        }
        return serializeAndSetEnabledPackageList(enabledPackages);
    }
}

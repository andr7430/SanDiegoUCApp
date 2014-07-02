package com.esri.UC;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.util.Log;


import com.esri.android.geotrigger.GeotriggerApiClient;
import com.esri.android.geotrigger.GeotriggerApiListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends PreferenceActivity {
    /**
     * Determines whether to always show the simplified settings UI, where
     * settings are presented in a single list. When false, settings are shown
     * as a master/detail two-pane view on tablets. When true, a single pane is
     * shown on tablets.
     */
    private static final boolean ALWAYS_SIMPLE_PREFS = false;

    private static enum ALL_POI{poi_Architecture, poi_Beach, poi_Cafe, poi_Esri_Events, poi_Historic_Site, poi_Infrastructure, poi_Market, poi_Museum, poi_Neighborhood, poi_Outdoors, poi_Park, poi_Restaurant, poi_Sculpture, poi_Shopping_and_Dining, poi_Store};


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupSimplePreferencesScreen();
    }

    /**
     * Shows the simplified settings UI if the device configuration if the
     * device configuration dictates that a simplified, single-pane UI should be
     * shown.
     */
    private void setupSimplePreferencesScreen() {
        if (!isSimplePreferences(this)) {
            return;
        }

        // In the simplified UI, fragments are not used at all and we instead
        // use the older PreferenceActivity APIs.

        // Add 'general' preferences.
        addPreferencesFromResource(R.xml.pref_general);

        // Add 'data and sync' preferences, and a corresponding header.
        PreferenceCategory fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_poi);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_poi);

        // Add 'notifications' preferences, and a corresponding header.
        fakeHeader = new PreferenceCategory(this);
        fakeHeader.setTitle(R.string.pref_header_notifications);
        getPreferenceScreen().addPreference(fakeHeader);
        addPreferencesFromResource(R.xml.pref_notification);



        // Bind the summaries of EditText/List/Dialog/Ringtone preferences to
        // their values. When their values change, their summaries are updated
        // to reflect the new value, per the Android Design guidelines.
        bindPreferenceSummaryToValue(findPreference("example_text"));
        //bindPreferenceSummaryToValue(findPreference("example_list"));
        bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));

        //bind the trigger status
        bindTriggerToValue(findPreference("poi_Architecture"));
        bindTriggerToValue(findPreference("poi_Beach"));
        bindTriggerToValue(findPreference("poi_Cafe"));
        bindTriggerToValue(findPreference("poi_Esri_Events"));
        bindTriggerToValue(findPreference("poi_Historic_Site"));
        bindTriggerToValue(findPreference("poi_Infrastructure"));
        bindTriggerToValue(findPreference("poi_Market"));
        bindTriggerToValue(findPreference("poi_Museum"));
        bindTriggerToValue(findPreference("poi_Neighborhood"));
        bindTriggerToValue(findPreference("poi_Outdoors"));
        bindTriggerToValue(findPreference("poi_Park"));
        bindTriggerToValue(findPreference("poi_Restaurant"));
        bindTriggerToValue(findPreference("poi_Sculpture"));
        bindTriggerToValue(findPreference("poi_Shopping_and_Dining"));
        bindTriggerToValue(findPreference("poi_Store"));
    }

    /** {@inheritDoc} */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this) && !isSimplePreferences(this);
    }

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Determines whether the simplified settings UI should be shown. This is
     * true if this is forced via {@link #ALWAYS_SIMPLE_PREFS}, or the device
     * doesn't have newer APIs like {@link PreferenceFragment}, or the device
     * doesn't have an extra-large screen. In these cases, a single-pane
     * "simplified" settings UI should be shown.
     */
    private static boolean isSimplePreferences(Context context) {
        return true;
//        return ALWAYS_SIMPLE_PREFS
//                || Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB
//                || !isXLargeTablet(context);
    }

//    /** {@inheritDoc} */
//    @Override
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public void onBuildHeaders(List<Header> target) {
//        if (!isSimplePreferences(this)) {
//            loadHeadersFromResource(R.xml.pref_headers, target);
//        }
//    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
                if (TextUtils.isEmpty(stringValue)) {
                    // Empty values correspond to 'silent' (no ringtone).
                    preference.setSummary(R.string.pref_ringtone_silent);

                } else {
                    Ringtone ringtone = RingtoneManager.getRingtone(
                            preference.getContext(), Uri.parse(stringValue));

                    if (ringtone == null) {
                        // Clear the summary if there was a lookup error.
                        preference.setSummary(null);
                    } else {
                        // Set the summary to reflect the new ringtone display
                        // name.
                        String name = ringtone.getTitle(preference.getContext());
                        preference.setSummary(name);
                    }
                }

            }else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }



    private Preference.OnPreferenceChangeListener sBindTriggerToValueListener = new Preference.OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            Log.d(preference.getKey().toString(), newValue.toString());
            ALL_POI tempval = ALL_POI.valueOf(preference.getKey());
            switch (tempval){
                case poi_Architecture:
                    if(newValue.equals(true)){
                        addTag("Architecture");

                    }else{
                        removeTag("Architecture");
                    }
                    break;
                case poi_Beach:
                    if(newValue.equals(true)){
                        addTag("Beach");

                    }else{
                        removeTag("Beach");
                    }
                    break;
                case poi_Cafe:
                    if(newValue.equals(true)){
                        addTag("Cafe");

                    }else{
                        removeTag("Cafe");
                    }
                    break;
                case poi_Esri_Events:
                    if(newValue.equals(true)){
                        addTag("esrievent");

                    }else{
                        removeTag("esrievent");
                    }
                    break;
                case poi_Historic_Site:
                    if(newValue.equals(true)){
                        addTag("Historic Site");

                    }else{
                        removeTag("Historic Site");
                    }
                    break;
                case poi_Infrastructure:
                    if(newValue.equals(true)){
                        addTag("Infrastructure");

                    }else{
                        removeTag("Infrastructure");
                    }
                    break;
                case poi_Market:
                    if(newValue.equals(true)){
                        addTag("Market");

                    }else{
                        removeTag("Market");
                    }
                    break;
                case poi_Museum:
                    if(newValue.equals(true)){
                        addTag("Museum");

                    }else{
                        removeTag("Museum");
                    }
                    break;
                case poi_Neighborhood:
                    if(newValue.equals(true)){
                        addTag("Neighborhood");

                    }else{
                        removeTag("Neighborhood");
                    }
                    break;
                case poi_Outdoors:
                    if(newValue.equals(true)){
                        addTag("Outdoors");

                    }else{
                        removeTag("Outdoors");
                    }
                    break;
                case poi_Park:
                    if(newValue.equals(true)){
                        addTag("Park");

                    }else{
                        removeTag("Park");
                    }
                    break;
                case poi_Restaurant:
                    if(newValue.equals(true)){
                        addTag("Restaurant");

                    }else{
                        removeTag("Restaurant");
                    }
                    break;
                case poi_Sculpture:
                    if(newValue.equals(true)){
                        addTag("Sculpture");

                    }else{
                        removeTag("Sculpture");
                    }
                    break;
                case poi_Shopping_and_Dining:
                    if(newValue.equals(true)){
                        addTag("Shopping & Dining");

                    }else{
                        removeTag("Shopping & Dining");
                    }
                    break;
                case poi_Store:
                    if(newValue.equals(true)){
                        addTag("Store");

                    }else{
                        removeTag("Store");
                    }
                    break;
            }


            return true;
        }
    };

    private void bindTriggerToValue(Preference preference){
        preference.setOnPreferenceChangeListener(sBindTriggerToValueListener);
        sBindTriggerToValueListener.onPreferenceChange(preference,  PreferenceManager
                .getDefaultSharedPreferences(preference.getContext())
                .getBoolean(preference.getKey(), false));
    }


    public void addTag(String tagname){
        JSONObject params = new JSONObject();
        try {
            params.put("addTags", tagname);
        } catch (JSONException e) {
            Log.e("Addtag", "Error creating device update parameters.", e);
        }

        GeotriggerApiClient.runRequest(this, "device/update", params, new GeotriggerApiListener() {
            @Override
            public void onSuccess(JSONObject data) {
                Log.d("Addtag", "Device updated: " + data.toString());
            }

            @Override
            public void onFailure(Throwable error) {
                Log.d("Addtag", "Failed to update device.", error);
            }
        });
    }
    public void removeTag(String tagname){
        JSONObject params = new JSONObject();
        try {
            params.put("removeTags", tagname);

        } catch (JSONException e) {
            Log.e("Removetag", "Error creating device update parameters.", e);
        }

        GeotriggerApiClient.runRequest(this, "device/update", params, new GeotriggerApiListener() {
            @Override
            public void onSuccess(JSONObject data) {
                Log.d("Removetag", "Device updated: " + data.toString());
            }

            @Override
            public void onFailure(Throwable error) {
                Log.d("Removetag", "Failed to update device.", error);
            }
        });
    }
//
//    /**
//     * This fragment shows general preferences only. It is used when the
//     * activity is showing a two-pane settings UI.
//     */
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public static class GeneralPreferenceFragment extends PreferenceFragment {
//        @Override
//        public void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.pref_general);
//
//            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
//            // to their values. When their values change, their summaries are
//            // updated to reflect the new value, per the Android Design
//            // guidelines.
//            bindPreferenceSummaryToValue(findPreference("example_text"));
//            //bindPreferenceSummaryToValue(findPreference("example_list"));
//        }
//    }
//
//
//    /**
//     * This fragment shows data and sync preferences only. It is used when the
//     * activity is showing a two-pane settings UI.
//     */
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public static class POIPreferenceFragment extends PreferenceFragment {
//        @Override
//        public void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.pref_poi);
//
//            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
//            // to their values. When their values change, their summaries are
//            // updated to reflect the new value, per the Android Design
//            // guidelines.
//            //bind the trigger status
//            bindTriggerToValue(findPreference("poi_Architecture"));
//            bindTriggerToValue(findPreference("poi_Beach"));
//            bindTriggerToValue(findPreference("poi_Cafe"));
//            bindTriggerToValue(findPreference("poi_Esri_Events"));
//            bindTriggerToValue(findPreference("poi_Historic_Site"));
//            bindTriggerToValue(findPreference("poi_Infrastructure"));
//            bindTriggerToValue(findPreference("poi_Market"));
//            bindTriggerToValue(findPreference("poi_Museum"));
//            bindTriggerToValue(findPreference("poi_Neighborhood"));
//            bindTriggerToValue(findPreference("poi_Outdoors"));
//            bindTriggerToValue(findPreference("poi_Park"));
//            bindTriggerToValue(findPreference("poi_Restaurant"));
//            bindTriggerToValue(findPreference("poi_Sculpture"));
//            bindTriggerToValue(findPreference("poi_Shopping_and_Dining"));
//            bindTriggerToValue(findPreference("poi_Store"));
//        }
//    }
//
//    /**
//     * This fragment shows notification preferences only. It is used when the
//     * activity is showing a two-pane settings UI.
//     */
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public static class NotificationPreferenceFragment extends PreferenceFragment {
//        @Override
//        public void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.pref_notification);
//
//            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
//            // to their values. When their values change, their summaries are
//            // updated to reflect the new value, per the Android Design
//            // guidelines.
//            bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
//        }
//    }

//    /**
//     * This fragment shows data and sync preferences only. It is used when the
//     * activity is showing a two-pane settings UI.
//     */
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
//    public static class DataSyncPreferenceFragment extends PreferenceFragment {
//        @Override
//        public void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            addPreferencesFromResource(R.xml.pref_data_sync);
//
//            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
//            // to their values. When their values change, their summaries are
//            // updated to reflect the new value, per the Android Design
//            // guidelines.
//            bindPreferenceSummaryToValue(findPreference("sync_frequency"));
//        }
//    }

}

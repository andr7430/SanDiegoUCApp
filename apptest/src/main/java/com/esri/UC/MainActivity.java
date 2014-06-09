package com.esri.UC;

import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.esri.android.geotrigger.GeotriggerApiClient;
import com.esri.android.geotrigger.GeotriggerApiListener;
import com.esri.android.geotrigger.GeotriggerBroadcastReceiver;
import com.esri.android.geotrigger.GeotriggerService;
import com.esri.android.geotrigger.TriggerBuilder;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;

import org.apache.http.StatusLine;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class MainActivity extends ActionBarActivity implements
        GeotriggerBroadcastReceiver.LocationUpdateListener,
        GeotriggerBroadcastReceiver.ReadyListener {
    private static final String TAG = "GeotriggerActivity";

    // Create a new application at https://developers.arcgis.com/en/applications
    private static final String AGO_CLIENT_ID = "g7L7BthrcX9vkxwG";

    // The project number from https://cloud.google.com/console
    private static final String GCM_SENDER_ID = "540929246562";

    // A list of initial tags to apply to the device.
    // Triggers created on the server for this application, with at least one of these same tags,
    // will be active for the device.
    private static final String[] TAGS = new String[] {"some_tag", "another_tag"};

    // The GeotriggerBroadcastReceiver receives intents from the
    // GeotriggerService, calling any listeners implemented in your class.
    private GeotriggerBroadcastReceiver mGeotriggerBroadcastReceiver;

    private boolean mShouldCreateTrigger;
    private boolean mShouldSendNotification;
    MapView mMapView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGeotriggerBroadcastReceiver = new GeotriggerBroadcastReceiver();
        mShouldCreateTrigger = false;
        mShouldSendNotification = true;
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.addLayer(new ArcGISTiledMapServiceLayer(
                "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer"));
    }

    @Override
    public void onStart() {
        super.onStart();

        GeotriggerHelper.startGeotriggerService(this, AGO_CLIENT_ID, GCM_SENDER_ID, TAGS,
                GeotriggerService.TRACKING_PROFILE_ADAPTIVE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the receiver. Activity will no longer respond to
        // GeotriggerService intents. Tracking and push notification handling
        // will continue in the background.
        unregisterReceiver(mGeotriggerBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the receiver. The default intent filter listens for all
        // intents that the receiver can handle. If you need to handle events
        // while the app is in the background, you must register the receiver
        // in the manifest.
        registerReceiver(mGeotriggerBroadcastReceiver,
                GeotriggerBroadcastReceiver.getDefaultIntentFilter());
    }

    @Override
    public void onReady() {
        // Called when the device has registered with ArcGIS Online and is ready
        // to make requests to the Geotrigger Service API.
        Toast.makeText(this, "GeotriggerService ready!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "GeotriggerService ready!");
    }

    @Override
    public void onLocationUpdate(Location loc, boolean isOnDemand) {
        // Called with the GeotriggerService obtains a new location update from
        // Android's native location services. The isOnDemand parameter lets you
        // determine if this location update was a result of calling
        // GeotriggerService.requestOnDemandUpdate()
        Toast.makeText(this, "Location Update Received!"+ GeotriggerService.getDeviceId(this),
                Toast.LENGTH_SHORT).show();
        Log.d(TAG, String.format("Location update received: (%f, %f)",
                loc.getLatitude(), loc.getLongitude())+ GeotriggerService.getDeviceId(this));

        if(mShouldSendNotification){
            mShouldSendNotification = false;


            //Making a Get Request
            Map<String, String> arg = new HashMap<String, String>();
            arg.put("f", "json");

            NetUtils.getJson(this,"http://sampleserver6.arcgisonline.com/arcgis/rest/services?f=json",null ,null, new NetUtils.JsonRequestListener() {
                @Override
                public void onSuccess(JSONObject json) {
                    Log.i(TAG, "reqeust/json success: " + json);
                }

                @Override
                public void onError(JSONObject json, StatusLine status) {
                    Log.i(TAG, "reqeust/json error: " + json);
                }

                @Override
                public void onFailure(Throwable error) {
                    Log.i(TAG, "reqeust/json failure: " + error);
                }
            });

            //Make a POST Request
            JSONObject params1 = new JSONObject();
            try {
                params1.put("where", "1=1");
                params1.put("geometryType", "esriGeometryEnvelope");
                params1.put("f", "json");
            } catch (JSONException e) {
                e.printStackTrace();
            }


            NetUtils.jsonPost(this,"http://sampleserver6.arcgisonline.com/arcgis/rest/services/911CallsHotspot/MapServer/1/query",params1,null, new NetUtils.JsonRequestListener() {
                @Override
                public void onSuccess(JSONObject json) {
                    Log.i(TAG, "reqeust/json success: " + json);
                }

                @Override
                public void onError(JSONObject json, StatusLine status) {
                    Log.i(TAG, "reqeust/json error: " + json);
                }

                @Override
                public void onFailure(Throwable error) {
                    Log.i(TAG, "reqeust/json failure: " + error);
                }
            });




            //Create params for test push notification
            JSONObject params = new JSONObject();
            try {
                params.put("text", "Push notifications are working!");
                params.put("url", "http://developers.arcgis.com");
            } catch (JSONException e) {
                Log.e(TAG, "Error creating device/notify params", e);
            }

            //Make Test Push Notification
            GeotriggerApiClient.runRequest(this, "device/notify", params, new GeotriggerApiListener() {
                @Override
                public void onSuccess(JSONObject json) {
                    Log.i(TAG, "device/notify success: " + json);
                }

                @Override
                public void onFailure(Throwable error) {
                    Log.e(TAG, "device/notify failure", error);
                }
            });
        }

        // Create the trigger if we haven't done so already.
        if (mShouldCreateTrigger) {
            // Set create trigger flag here so that we don't create multiple
            // triggers if we get a few initial updates in rapid succession.
            mShouldCreateTrigger = false;

            // The TriggerBuilder helps build JSON parameters for use with the
            // 'trigger/create' API route.
            JSONObject params = new TriggerBuilder()
                    .setTags(TAGS[0]) // make sure to add at least one of the tags we have on the device to this trigger
                    .setGeo(loc, 100)
                    .setDirection(TriggerBuilder.DIRECTION_LEAVE)
                    .setNotificationText("You left the trigger!")
                    .build();

            // Send the request to the Geotrigger API.
            GeotriggerApiClient.runRequest(this, "trigger/create", params,
                    new GeotriggerApiListener() {
                        @Override
                        public void onSuccess(JSONObject data) {
                            Toast.makeText(MainActivity.this, "Trigger created!",
                                    Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "Trigger Created");
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            Log.d(TAG, "Error creating trigger!", e);
                            // It didn't work, so we need to try again
                            mShouldCreateTrigger = true;
                        }
                    }
            );
        }
    }
}
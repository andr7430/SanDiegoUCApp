package com.esri.UC;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.esri.android.geotrigger.GeotriggerApiClient;
import com.esri.android.geotrigger.GeotriggerApiListener;
import com.esri.android.geotrigger.GeotriggerBroadcastReceiver;
import com.esri.android.geotrigger.GeotriggerService;
import com.esri.android.geotrigger.TriggerBuilder;
import com.esri.android.map.Layer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.LocationService;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnLongPressListener;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.map.popup.Popup;
import com.esri.android.map.popup.PopupContainer;
import com.esri.android.toolkit.map.MapViewHelper;
import com.esri.android.toolkit.map.PopupCreateListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureSet;
import com.esri.core.symbol.SimpleMarkerSymbol;

import org.apache.http.StatusLine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.esri.core.tasks.ags.query.Query;
import com.google.gson.Gson;


public class MainActivity extends ActionBarActivity implements
        GeotriggerBroadcastReceiver.LocationUpdateListener,
        GeotriggerBroadcastReceiver.ReadyListener {
    private static final String TAG = "GeotriggerActivity";

    // Create a new application at https://developers.arcgis.com/en/applications
    private static final String AGO_CLIENT_ID = "eVCAwCf3YFLqnOfs";

    // The project number from https://cloud.google.com/console
    private static final String GCM_SENDER_ID = "540929246562";

    // A list of initial tags to apply to the device.
    // Triggers created on the server for this application, with at least one of these same tags,
    // will be active for the device.
    private static final String[] TAGS = new String[]{"init_tag"};

    // The GeotriggerBroadcastReceiver receives intents from the
    // GeotriggerService, calling any listeners implemented in your class.
    private GeotriggerBroadcastReceiver mGeotriggerBroadcastReceiver;

    private boolean mShouldCreateTrigger;
    private boolean mShouldSendNotification;
    MapView mMapView;

    //for the popup
    private PopupContainer popupContainer;
    private PopupDialog popupDialog;
    private ProgressDialog progressDialog;
    private AtomicInteger count;
    int initcount=0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Bundle extras  = getIntent().getExtras();


        mGeotriggerBroadcastReceiver = new GeotriggerBroadcastReceiver();
        mShouldCreateTrigger = false;
        mShouldSendNotification = true;
        mMapView = (MapView) findViewById(R.id.map);
//        mMapView.addLayer(new ArcGISTiledMapServiceLayer(
//                "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer"));
//
//        mMapView.addLayer(new ArcGISFeatureLayer("http://services.arcgis.com/Wl7Y1m92PbjtJs5n/arcgis/rest/services/UC_1/FeatureServer/0", ArcGISFeatureLayer.MODE.ONDEMAND));
//        mMapView.addLayer(new ArcGISFeatureLayer("http://services.arcgis.com/Wl7Y1m92PbjtJs5n/arcgis/rest/services/UC_1/FeatureServer/1", ArcGISFeatureLayer.MODE.ONDEMAND));
//        mMapView.addLayer(new ArcGISFeatureLayer("http://services.arcgis.com/Wl7Y1m92PbjtJs5n/arcgis/rest/services/UC_1/FeatureServer/2", ArcGISFeatureLayer.MODE.ONDEMAND));
//        mMapView.addLayer(new ArcGISFeatureLayer("http://services.arcgis.com/Wl7Y1m92PbjtJs5n/arcgis/rest/services/UC_1/FeatureServer/3", ArcGISFeatureLayer.MODE.ONDEMAND));

        //mMapView = new MapView(this, "http://www.arcgis.com/home/item.html?id=afc701cded49434b91afbd975d59569c",);
        //setContentView(mMapView);




        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
            @Override
            public void onStatusChanged(Object o, STATUS status) {

                initcount++;
                Log.d("statuschanged", o.toString()+" "+initcount);
//                if ((status == STATUS.INITIALIZED) && (o == mMapView) ) {
//                    onNewIntent(getIntent());
//                }
                if(initcount==6){
                    onNewIntent(getIntent());
                }
            }
        });

        // Tap on the map and show popups for selected features.
        mMapView.setOnSingleTapListener(new OnSingleTapListener() {
            private static final long serialVersionUID = 1L;

            public void onSingleTap(float x, float y) {
                if (mMapView.isLoaded()) {
                    // Instantiate a PopupContainer
                    popupContainer = new PopupContainer(mMapView);
                    int id = popupContainer.hashCode();
                    popupDialog = null;
                    // Display spinner.
                    if (progressDialog == null || !progressDialog.isShowing())
                        progressDialog = ProgressDialog.show(mMapView.getContext(), "", "Querying...");

                    // Loop through each layer in the webmap
                    int tolerance = 20;
                    Envelope env = new Envelope(mMapView.toMapPoint(x, y), 20 * mMapView.getResolution(), 20 * mMapView.getResolution());
                    Layer[] layers = mMapView.getLayers();
                    count = new AtomicInteger();
                    for (Layer layer : layers) {
                        // If the layer has not been initialized or is invisible, do nothing.
                        if (!layer.isInitialized() || !layer.isVisible())
                            continue;

                        if (layer instanceof ArcGISFeatureLayer) {
                            Log.d("querying","a featurelayer");
                            // Query feature layer and display popups
                            ArcGISFeatureLayer featureLayer = (ArcGISFeatureLayer) layer;
                            if (featureLayer.getPopupInfo() != null) {
                                // Query feature layer which is associated with a popup definition.
                                count.incrementAndGet();
                                new RunQueryFeatureLayerTask(x, y, tolerance, id).execute(featureLayer);
                            }
                        }
                    }
                }
            }
        });

    }

    @Override
    protected void onNewIntent(Intent intent) {
        //super.onNewIntent(intent);

            Bundle extras = intent.getExtras();
        if(extras!=null) {
            if (extras.containsKey("NewNotification")) {
                Log.d("GOTNOTIFICATION", extras.get("data").toString());

                try {
                    //extras.get("data").toString().to
                    //JSONObject alljson = ((JSONObject) extras.get("data").toString());

                    //Gson gson = new Gson();
                    String stringjson = extras.get("data").toString();
                    Log.e("stringjson",stringjson);
                    JSONObject alljson = new JSONObject(stringjson);




                    String title = alljson.getString("TITLE");
                    final int objectid = alljson.getInt("OBJECTID");

                    Log.e("jsons", title+" "+objectid);


                    ArrayList<Layer> layers = new ArrayList<Layer>();
                    layers.add(new ArcGISFeatureLayer("http://services.arcgis.com/Wl7Y1m92PbjtJs5n/arcgis/rest/services/UC_1/FeatureServer/0", ArcGISFeatureLayer.MODE.ONDEMAND));
                    layers.add(new ArcGISFeatureLayer("http://services.arcgis.com/Wl7Y1m92PbjtJs5n/arcgis/rest/services/UC_1/FeatureServer/1", ArcGISFeatureLayer.MODE.ONDEMAND));
                    layers.add(new ArcGISFeatureLayer("http://services.arcgis.com/Wl7Y1m92PbjtJs5n/arcgis/rest/services/UC_1/FeatureServer/2", ArcGISFeatureLayer.MODE.ONDEMAND));
                    layers.add(new ArcGISFeatureLayer("http://services.arcgis.com/Wl7Y1m92PbjtJs5n/arcgis/rest/services/UC_1/FeatureServer/3", ArcGISFeatureLayer.MODE.ONDEMAND));
                    Layer[] layers1 = mMapView.getLayers();

                    count = new AtomicInteger();
                    // If the layer has not been initialized or is invisible, do nothing.
//                        if (!layer.isInitialized() || !layer.isVisible())
//                            continue;
                    for (final Layer layer : layers1)
                        if (layer instanceof ArcGISFeatureLayer) {
                            if (!layer.isInitialized() || !layer.isVisible())
                                continue;

                            Query query = new Query();
                            String querystring = "OBJECTID="+objectid+" AND TITLE='"+title+"'";
                            Log.d("querystring", querystring);
                            query.setWhere(querystring);

                            ((ArcGISFeatureLayer) layer).queryFeatures(query, new CallbackListener<FeatureSet>() {
                                @Override
                                public void onCallback(FeatureSet featureSet) {
                                    Log.e("length", "length: "+featureSet.getGraphics().length);
                                    if(featureSet!=null && featureSet.getGraphics().length>0) {
                                        popupContainer = new PopupContainer(mMapView);
                                        int id = popupContainer.hashCode();
                                        popupDialog = null;

                                        Feature fr = featureSet.getGraphics()[0];
                                        ArrayList<Feature> features = new ArrayList<Feature>();
                                        features.add(fr);

                                        Feature[] featarray = features.toArray(new Feature[0]);

                                        for (Feature fr2 : featarray) {
                                            Popup popup = layer.createPopup(mMapView, 0, fr2);
                                            popupContainer.addPopup(popup);
                                        }

                                        createPopupViews(featarray, id);
                                    }

                                }

                                @Override
                                public void onError(Throwable throwable) {
                                    Log.e("ERROR", throwable.getMessage());

                                }
                            });
//                            Feature feat1 = ((ArcGISFeatureLayer) layer).getGraphic(objectid);
//                            if (feat1 != null) {
//                                Object obj = feat1.getAttributeValue("TITLE");
//                                Log.d("title", feat1.getAttributeValue("TITLE").toString());
//                                if (((ArcGISFeatureLayer) layer).getGraphic(objectid).getAttributeValue("TITLE").toString().equalsIgnoreCase(title)) {
//                                    Log.d("query", "a match");
//                                    // Instantiate a PopupContainer
//                                    popupContainer = new PopupContainer(mMapView);
//                                    int id = popupContainer.hashCode();
//                                    popupDialog = null;
//
//                                    Feature fr = ((ArcGISFeatureLayer) layer).getGraphic(objectid);
//                                    ArrayList<Feature> features = new ArrayList<Feature>();
//                                    features.add(fr);
//
//
//                                    Popup popup = ((ArcGISFeatureLayer) layer).createPopup(mMapView, 0, fr);
//                                    popupContainer.addPopup(popup);
//                                    createPopupViews(features.toArray(new Feature[0]), id);
//
//                                } else {
//                                    Log.d("query", "no match");
//                                }
//                            }
                        }

                } catch (JSONException e) {
                    Log.e("query", e.getMessage());
                    e.printStackTrace();
                }



            } else {
                Log.d("GOTNOTIFICATION", "NO");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_locate:
                try {
                    locateMe();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return true;
                }
            case R.id.action_settings:
                switchToSettings();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();

        GeotriggerHelper.startGeotriggerService(this, AGO_CLIENT_ID, GCM_SENDER_ID, TAGS,
                GeotriggerService.TRACKING_PROFILE_FINE);
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


//
//        Log.e("trymessage", "try to get push notification");
//        Bundle bundle = getIntent().getExtras();
//        if (bundle != null) {
//            Log.e("trymessage", "got a push notification");
//            String notificationData = bundle.getString("com.parse.Data");
//            if (notificationData != null) {
//                Log.d("detectPushNotificationMessage", "notificationData =" + notificationData);
//                try {
//                    locateMe();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                //handlePushNotificationMessage(notificationData);
//            }
//
//        }else{
//            Log.e("trymessage", "did not get a push notification");
//        }
    }

    @Override
    public void onReady() {
        // Called when the device has registered with ArcGIS Online and is ready
        // to make requests to the Geotrigger Service API.
        //Toast.makeText(this, "GeotriggerService ready!", Toast.LENGTH_SHORT).show();


        Log.d(TAG, "GeotriggerService ready!");

        //check if it is the first time the app is launched, if it is do some tagging
        SharedPreferences settings = getSharedPreferences("MyPrefsFile", 0);
        if (settings.getBoolean("myfirsttime", true)) {
            addTag("Architecture");
            addTag("Beach");
            addTag("Cafe");
            addTag("esrievent");
            addTag("Historic Site");
            addTag("Infrastructure");
            addTag("Market");
            addTag("Museum");
            addTag("Neighborhood");
            addTag("Outdoors");
            addTag("Park");
            addTag("Restaurant");
            addTag("Sculpture");
            addTag("Shopping & Dining");
            addTag("Store");

            settings.edit().putBoolean("myfirsttime", false).commit();
        }
    }


    public void addTag(String tagname) {
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

    @Override
    public void onLocationUpdate(Location loc, boolean isOnDemand) {
        // Called with the GeotriggerService obtains a new location update from
        // Android's native location services. The isOnDemand parameter lets you
        // determine if this location update was a result of calling
        // GeotriggerService.requestOnDemandUpdate()
//
//        Toast.makeText(this, "Location Update Received!" + GeotriggerService.getDeviceId(this),
//                Toast.LENGTH_SHORT).show();
//        Log.d(TAG, String.format("Location update received: (%f, %f)",
//                loc.getLatitude(), loc.getLongitude()) + GeotriggerService.getDeviceId(this));

        if (mShouldSendNotification) {
            mShouldSendNotification = false;


//            //Making a Get Request
//            Map<String, String> arg = new HashMap<String, String>();
//            arg.put("f", "json");
//
//            NetUtils.getJson(this,"http://sampleserver6.arcgisonline.com/arcgis/rest/services?f=json",null ,null, new NetUtils.JsonRequestListener() {
//                @Override
//                public void onSuccess(JSONObject json) {
//                    Log.i(TAG, "reqeust/json success: " + json);
//                }
//
//                @Override
//                public void onError(JSONObject json, StatusLine status) {
//                    Log.i(TAG, "reqeust/json error: " + json);
//                }
//
//                @Override
//                public void onFailure(Throwable error) {
//                    Log.i(TAG, "reqeust/json failure: " + error);
//                }
//            });
//
//            //Make a POST Request
//            JSONObject params1 = new JSONObject();
//            try {
//                params1.put("where", "1=1");
//                params1.put("geometryType", "esriGeometryEnvelope");
//                params1.put("f", "json");
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//
//
//            NetUtils.jsonPost(this,"http://sampleserver6.arcgisonline.com/arcgis/rest/services/911CallsHotspot/MapServer/1/query",params1,null, new NetUtils.JsonRequestListener() {
//                @Override
//                public void onSuccess(JSONObject json) {
//                    Log.i(TAG, "reqeust/json success: " + json);
//                }
//
//                @Override
//                public void onError(JSONObject json, StatusLine status) {
//                    Log.i(TAG, "reqeust/json error: " + json);
//                }
//
//                @Override
//                public void onFailure(Throwable error) {
//                    Log.i(TAG, "reqeust/json failure: " + error);
//                }
//            });


//            //Create params for test push notification
//            JSONObject params = new JSONObject();
//            try {
//                params.put("text", "Push notifications are working!");
//                //params.put("url", "http://developers.arcgis.com");
//            } catch (JSONException e) {
//                Log.e(TAG, "Error creating device/notify params", e);
//            }
//
//            //Make Test Push Notification
//            GeotriggerApiClient.runRequest(this, "device/notify", params, new GeotriggerApiListener() {
//                @Override
//                public void onSuccess(JSONObject json) {
//                    Log.i(TAG, "device/notify success: " + json);
//                }
//
//                @Override
//                public void onFailure(Throwable error) {
//                    Log.e(TAG, "device/notify failure", error);
//                }
//            });
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

    public void locateMe() throws Exception {
        LocationDisplayManager locationDisplayManager = mMapView.getLocationDisplayManager();
        //locationDisplayManager.setDefaultSymbol(new SimpleMarkerSymbol(Color.BLUE, 15, SimpleMarkerSymbol.STYLE.CIRCLE));
        locationDisplayManager.setShowLocation(true);
        locationDisplayManager.start();
        locationDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);

    }

    public void switchToSettings() {
        Intent myIntent = new Intent(MainActivity.this, SettingsActivity.class);
        //myIntent.putExtra("key", value); //Optional parameters
        MainActivity.this.startActivity(myIntent);
    }



    private void createPopupViews(Feature[] features, final int id) {
        if ( id != popupContainer.hashCode() ) {
            if (progressDialog != null && progressDialog.isShowing() && count.intValue() == 0)
                progressDialog.dismiss();

            return;
        }

        if (popupDialog == null) {
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            // Create a dialog for the popups and display it.
            popupDialog = new PopupDialog(mMapView.getContext(), popupContainer);
            popupDialog.show();
        }
    }

    private class IdQuery extends AsyncTask<ArcGISFeatureLayer, Void, Feature[]> {
        private int objectid;
        private String title;

        public IdQuery(int objectid, String title){
            super();
            this.objectid = objectid;
            this.title = title;
        }
        @Override
        protected Feature[] doInBackground(ArcGISFeatureLayer... params) {
            return new Feature[0];
        }
    }

    // Query feature layer by hit test
    private class RunQueryFeatureLayerTask extends AsyncTask<ArcGISFeatureLayer, Void, Feature[]> {

        private int tolerance;
        private float x;
        private float y;
        private ArcGISFeatureLayer featureLayer;
        private int id;

        public RunQueryFeatureLayerTask(float x, float y, int tolerance, int id) {
            super();
            this.x = x;
            this.y = y;
            this.tolerance = tolerance;
            this.id = id;
        }

        @Override
        protected Feature[] doInBackground(ArcGISFeatureLayer... params) {
            for (ArcGISFeatureLayer featureLayer : params) {
                this.featureLayer = featureLayer;
                // Retrieve feature ids near the point.
                int[] ids = featureLayer.getGraphicIDs(x, y, tolerance);
                if (ids != null && ids.length > 0) {
                    ArrayList<Feature> features = new ArrayList<Feature>();
                    for (int id : ids) {
                        // Obtain feature based on the id.
                        Feature f = featureLayer.getGraphic(id);
                        if (f == null)
                            continue;
                        features.add(f);
                    }
                    // Return an array of features near the point.
                    return features.toArray(new Feature[0]);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Feature[] features) {
            Log.d("postexecute","runpostexecute");
            count.decrementAndGet();
            if (features == null || features.length == 0) {
                Log.d("postexecute","no features");
                if (progressDialog != null && progressDialog.isShowing() && count.intValue() == 0)
                    progressDialog.dismiss();

                return;
            }
            // Check if the requested PopupContainer id is the same as the current PopupContainer.
            // Otherwise, abandon the obsoleted query result.
            if (id != popupContainer.hashCode()) {
                Log.d("postexecute","something wrong with popup");
                if (progressDialog != null && progressDialog.isShowing() && count.intValue() == 0)
                    progressDialog.dismiss();

                return;
            }

            for (Feature fr : features) {
                Log.d("postexecute","createfeatures"+fr.getId());

                Popup popup = featureLayer.createPopup(mMapView, 0, fr);
                popupContainer.addPopup(popup);
            }
            createPopupViews(features, id);
        }

    }



    // A customize full screen dialog.
    private class PopupDialog extends Dialog {
        private PopupContainer popupContainer;

        public PopupDialog(Context context, PopupContainer popupContainer) {
            super(context, android.R.style.Theme);
            this.popupContainer = popupContainer;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            LinearLayout layout = new LinearLayout(getContext());
            layout.addView(popupContainer.getPopupContainerView(), android.widget.LinearLayout.LayoutParams.FILL_PARENT, android.widget.LinearLayout.LayoutParams.FILL_PARENT);
            setContentView(layout, params);
        }

    }
}


//    @Override
//    public void onPushMessage(Bundle bundle) {
//        try {
//            Log.d("MessageListener", "received message");
//            Log.d("MessageListener", bundle.get("text").toString());
//            locateMe();
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

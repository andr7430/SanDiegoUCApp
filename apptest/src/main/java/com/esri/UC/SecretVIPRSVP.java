package com.esri.UC;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.StatusLine;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.w3c.dom.Text;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.Duration;


public class SecretVIPRSVP extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secret_viprsvp);
        onNewIntent(getIntent());

        Button cancelbutton = (Button) findViewById(R.id.secretCancel);
        final Button rsvpbutton = (Button) findViewById(R.id.secretVIPRSVP);

        cancelbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToMain();
            }
        });

        rsvpbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Map<String, String> arg = new HashMap<String, String>();
                arg.put("f", "json");

                NetUtils.getJson(getApplicationContext(),"http://sampleserver6.arcgisonline.com/arcgis/rest/services?f=json",null ,null, new NetUtils.JsonRequestListener() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        Log.i("GetRequest", "request/json success: " + json);
                        //Toast.makeText(getBaseContext(), "You have Successfully RSVP'd!", Toast.LENGTH_LONG);
                        //switchToMain();
                        new RSVPd().show(getFragmentManager(),"RSVP");
                        //rsvpbutton.setClickable(false);
                        rsvpbutton.setEnabled(false);
                    }

                    @Override
                    public void onError(JSONObject json, StatusLine status) {
                        Log.i("GetRequest", "request/json error: " + json);
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        Log.i("GetRequest", "request/json failure: " + error);
                    }
                });

//                            //Make a POST Request
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
//            NetUtils.jsonPost(getApplicationContext(),"http://sampleserver6.arcgisonline.com/arcgis/rest/services/911CallsHotspot/MapServer/1/query",params1,null, new NetUtils.JsonRequestListener() {
//                @Override
//                public void onSuccess(JSONObject json) {
//                    Log.i("PostRequest", "reqeust/json success: " + json);
//                    Toast.makeText(getApplicationContext(),"RSVP Succeeded", Toast.LENGTH_SHORT);
//                    switchToMain();
//                }
//
//                @Override
//                public void onError(JSONObject json, StatusLine status) {
//                    Log.i("PostRequest", "reqeust/json error: " + json);
//                }
//
//                @Override
//                public void onFailure(Throwable error) {
//                    Log.i("PostRequest", "reqeust/json failure: " + error);
//                }
//            });


            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        //super.onNewIntent(intent);

        Bundle extras = intent.getExtras();
        if (extras.containsKey("NewNotification")) {
            Log.d("GOTNOTIFICATION", extras.get("data").toString());

            String title="";
            int objectid=0;
            String imageurl="";
            String category="";
            String desc1="";
            String desc2="";
            String desc3="";
            String desc4="";
            String desc5="";
            String hours="";
            String address="";
            String website="";
            String text=extras.get("text").toString();

            try {
                String stringjson = extras.get("data").toString();
                Log.e("stringjson", stringjson);
                JSONObject alljson = new JSONObject(stringjson);

                title = alljson.getString("TITLE");
                objectid = alljson.getInt("OBJECTID");
                imageurl = alljson.getString("IMAGE_URL");
                category = alljson.getString("Type");
                desc1 = alljson.getString("Desc1");
                desc2 = alljson.getString("Desc2");
                desc3 = alljson.getString("Desc3");
                desc4 = alljson.getString("Desc4");
                desc5 = alljson.getString("Desc5");
                hours = alljson.getString("Hours");
                address = alljson.getString("Address");
                website = alljson.getString("Website");

            } catch (JSONException e) {
                e.printStackTrace();
            }


            TextView nameHolder = (TextView) findViewById(R.id.nameHolder);
            nameHolder.setText(title);
            TextView textHolder = (TextView) findViewById(R.id.textHolder);
            textHolder.setText(text);
            TextView addressHolder = (TextView) findViewById(R.id.addressHolder);
            addressHolder.setText(address);
            TextView descriptionHolder = (TextView) findViewById(R.id.descriptionHolder);
            descriptionHolder.setText(desc1+desc2+desc3+desc4+desc5);
            TextView websiteHolder = (TextView) findViewById(R.id.websiteHolder);
            websiteHolder.setText(website);
            TextView hoursHolder = (TextView) findViewById(R.id.hoursHolder);
            hoursHolder.setText(hours);
            TextView categoryHolder = (TextView) findViewById(R.id.categoryHolder);
            categoryHolder.setText(category);

            // show The Image
            new DownloadImageTask((ImageView) findViewById(R.id.imageView))
                    .execute(imageurl);
        }
    }


    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.secret_viprsv, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            switchToSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void switchToSettings() {
        Intent myIntent = new Intent(SecretVIPRSVP.this, SettingsActivity.class);
        //myIntent.putExtra("key", value); //Optional parameters
        SecretVIPRSVP.this.startActivity(myIntent);
    }

    public void switchToMain() {
        Intent myIntent = new Intent(SecretVIPRSVP.this, MainActivity.class);
        //myIntent.putExtra("key", value); //Optional parameters
        SecretVIPRSVP.this.startActivity(myIntent);
    }

    public static class RSVPd extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the Builder class for convenient dialog construction
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("You have Successfully RSVP'd")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // FIRE ZE MISSILES!

                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

}


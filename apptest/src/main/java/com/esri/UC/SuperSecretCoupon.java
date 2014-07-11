package com.esri.UC;

import android.app.Activity;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;


public class SuperSecretCoupon extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_super_secret_coupon);
        onNewIntent(getIntent());

        Button cancelbutton = (Button) findViewById(R.id.secretCancel);
        cancelbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchToMain();
            }
        });
    }
    @Override
    protected void onNewIntent(Intent intent) {
        //super.onNewIntent(intent);

        Bundle extras = intent.getExtras();
        if (extras.containsKey("NewCoupon")) {

            String message="";
            String coupon="";

            try {
                String stringjson = extras.get("data").toString();
                Log.e("stringjson", stringjson);
                JSONObject alljson = new JSONObject(stringjson);

                message = alljson.getString("Message");
                coupon = alljson.getString("Coupon");

            }catch (JSONException e){
                e.printStackTrace();
            }
            // show The Image
            new DownloadImageTask((ImageView) findViewById(R.id.couponimage))
                    .execute(coupon);
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
        getMenuInflater().inflate(R.menu.super_secret_coupon, menu);
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

    public void switchToMain() {
        Intent myIntent = new Intent(SuperSecretCoupon.this, MainActivity.class);
        SuperSecretCoupon.this.startActivity(myIntent);
    }

    public void switchToSettings() {
        Intent myIntent = new Intent(SuperSecretCoupon.this, SettingsActivity.class);
        SuperSecretCoupon.this.startActivity(myIntent);
    }
}

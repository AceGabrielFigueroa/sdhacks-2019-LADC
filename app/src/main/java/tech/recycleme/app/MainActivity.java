package tech.recycleme.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity {
    static final int REQUEST_TAKE_PHOTO = 1;
    private File file;
    private TextView mTextViewResult; //hold http get response

    /*
    @PURPOSE: Creates the associated image file to save to the app.
    @RESULT:  Returns a file where the image is stored. Returns null if failed.
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,      /* prefix */
                ".jpg",      /* suffix */
                storageDir          /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }

    /*
    @PURPOSE: Uses the camera API to take a picture of barcode.
    @RESULT: Returns a file where the image is stored. Returns null if failed.
     */
    private File dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // TODO: Do error for picture capture.
                // Error occurred while creating the File
                //...
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);

                return photoFile;
            }
        }

        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);

        /* Navigstion Setup */
        BottomNavigationView navigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        navigationView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new InfoFragment()).commit();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener onNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment selectedFragment = null;

            switch (item.getItemId()) {
                case R.id.info:
                    selectedFragment = new InfoFragment();
                    break;

                case R.id.camera:
                    file =  dispatchTakePictureIntent();

                    selectedFragment = new InfoFragment();

                    break;
                case R.id.directions:
                                                        selectedFragment = new DirectionFragment();
                    break;
            }

            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
            return true;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK)
        {
            Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            BarcodeDetector detector =
                    new BarcodeDetector.Builder(getApplicationContext())
                            .setBarcodeFormats(0)
                            .build();

            Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
            SparseArray<Barcode> barcodes = detector.detect(frame);

            Barcode thisCode = barcodes.valueAt(0);

            /* Build Api Call */
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url("https://sd-hacks-257117.appspot.com/getRecyclable/" +
                            thisCode.rawValue)
                    .build();


            /* Call */
            client.newCall(request).enqueue(new Callback() {
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String myResponse = response.body().string();
                        JSONObject jsonResponse;

                        try {
                            jsonResponse = new JSONArray(myResponse).getJSONObject(0);

                            Log.d("PARSED", "DOING");
                        } catch (JSONException err) {

                        }

                        InfoFragment.value = "AAAAAA";
                        Log.d("PARSED", myResponse);
                    }
                }

            });

        }
    }


        /*
        // Detector
        Button btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ImageView myImageView = (ImageView) findViewById(R.id.imgview);

                Bitmap myBitmap = BitmapFactory.decodeFile(currentPhotoPath);
                myImageView.setImageBitmap(myBitmap);

                // Setup Barcode detector
                TextView txtView = (TextView) findViewById(R.id.text_view_result);

                BarcodeDetector detector =
                        new BarcodeDetector.Builder(getApplicationContext())
                                .setBarcodeFormats(0)
                                .build();

                if (!detector.isOperational()) {
                    txtView.setText("Cannot do it lmao");
                    return;
                }

                // Detect barcode
                Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
                SparseArray<Barcode> barcodes = detector.detect(frame);

                // Decode
                Barcode thisCode = barcodes.valueAt(0);
                //txtView.setText(thisCode.rawValue);

                //init http get/post request
                mTextViewResult = findViewById(R.id.text_view_result);
                OkHttpClient client = new OkHttpClient();

                String url = "https://api.edamam.com/api/food-database/parser?upc=" +
                        thisCode.rawValue +
                        "&app_id=ef713db9&app_key=6db7c214c7e5be5f9e131a0314d4ccdc";


                Request request = new Request.Builder()
                        .url(url)
                        .build();

                client.newCall(request).enqueue(new Callback() {
                    //@Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                    }

                    //@Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            final String myResponse = response.body().string();
                            JSONObject jsonResponse = null;
                            JSONObject imgObject = null;
                            JSONArray hints = null;
                            String imgUrl = null;

                            try {
                                Log.d("Parsing", myResponse);
                                jsonResponse = new JSONObject(myResponse);
                            } catch (JSONException err) {
                               Log.e("JSON PARSE", "Couldn't read JSON");
                            }

                            if (jsonResponse != null && jsonResponse.has("hints")) {
                                try {
                                    hints = jsonResponse.getJSONArray("hints");
                                    Log.d("HINTS", hints.toString());
                                } catch (JSONException err) {
                                    System.out.println("Couldn't get hints");
                                }
                            }

                            if (hints != null && hints.length() != 0) {
                                try {
                                    imgObject = hints.getJSONObject(0);
                                    Log.d("IMAGE OBJECT", imgObject.toString());

                                } catch (JSONException err) {
                                    System.out.println("Couldn't get index");
                                }
                            }

                            if (imgObject != null && imgObject.has("food")) {
                                try {
                                    JSONObject foodObj = imgObject.getJSONObject("food");
                                    imgUrl = foodObj.get("image").toString();
                                    Log.d("IMAGE URL", imgUrl);

                                } catch (JSONException err) {
                                    Log.w("PARSE", "Image not found!");
                                }
                            } else {
                                // generic food image if none exists
                                imgUrl = "https://downtowncl.org/wp-content/uploads/2016/08/1977_Food-Drink-Generic-Logo.jpg";
                            }
                            final String finalUrl = new String(imgUrl);
                            final Bitmap bm = BitmapFactory.decodeStream(
                                new URL(finalUrl).openConnection().getInputStream());


                            //make a request for the image
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTextViewResult.setText(finalUrl);
                                    myImageView.setImageBitmap(bm);

                                }
                            });
                        }
                    }
                });
            }
        });*/
}

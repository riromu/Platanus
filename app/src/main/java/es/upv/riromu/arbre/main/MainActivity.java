package es.upv.riromu.arbre.main;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import es.upv.riromu.arbre.R;
import es.upv.riromu.arbre.crop.CropImageActivity;
import es.upv.riromu.arbre.gps.GPSTracking;
import es.upv.riromu.arbre.image.ImageTreatment;
import es.upv.riromu.arbre.image.ImageWrapper;
import es.upv.riromu.arbre.iucomponents.RangeSeekBar;
import es.upv.riromu.arbre.util.Util;

/* Licensed under the Apache License, Version 2.0 (the "License");
        * you may not use this file except in compliance with the License.
        * You may obtain a copy of the License at
        *
        *      http://www.apache.org/licenses/LICENSE-2.0
        *
        * Unless required by applicable law or agreed to in writing, software
        * distributed under the License is distributed on an "AS IS" BASIS,
        * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        * See the License for the specific language governing permissions and
        * limitations under the License.
        */
public class MainActivity extends FragmentActivity{
    static final int PICK_IMAGE = 1;
    static final int CROP_IMAGE = 2;
    static final int CAPTURE_IMAGE = 3;
    static final int TREAT_IMAGE = 4;
    static final int SHOW_HISTOGRAM = 5;
    static final int COLOUR_R = 1;
    static final int COLOUR_G = 2;
    static final int COLOUR_B = 3;
    static final int MIN_TH = 50;
    static final int MAX_TH = 130;
    static final int MAX_SIZE = 300;
    private static final String TAG = "MainActivity";
    public static final String RETURN_DATA = "return-data";
    private static final String ACCESSKEY = "ebtabril15";

  private static final String URL_UPLOAD = "http://192.168.1.134:8080/Platanus/Upload";

        /*IMAGES VARIABLES*/

    private Uri image_uri, croppedimage_uri;
    private Bitmap image, croppedimage, treatedimage, histogram;
    private String histograma,mascara;
    private int colours[] = {255, 255, 255, 255};
    private String captura = String.valueOf(System.currentTimeMillis() / 1000L);
    private int compressRatio;
    private GPSTracking gpstracker;
    /*GPS VARIABLES*/
    private LocationManager locationManager = null;
    private LocationListener locationListener = null;
    private String latitude, longitude;
    private boolean[] state = new boolean[6];
    private String upload;
    private int minrange,maxrange;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Arrays.fill(state, false);
        Arrays.fill(colours, 255);
        compressRatio = 100;
        // state[TREAT_IMAGE]=false;
        // image = BitmapFactory.decodeResource(getResources(), R.drawable.platanus_hispanica);
        Log.i(TAG, "Trying to load OpenCV library");
        if (!OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback)) {
            Log.e(TAG, "Cannot connect to OpenCV Manager");
        }
        setContentView(R.layout.intro_activity);
        ImageView imv = (ImageView) findViewById(R.id.image_intro);
        imv.setImageDrawable(getResources().getDrawable(R.drawable.platanus_hispanica));
        // create RangeSeekBar as Integer range between 0 and 180
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String rangepreference=(String)sharedPref.getString("rangepreference","H");

        RangeSeekBar<Integer> seekBar=null;

        if(rangepreference.equals("H"))
            seekBar = new RangeSeekBar<Integer>(Integer.valueOf(sharedPref.getString("minH_preference","60")), Integer.valueOf(sharedPref.getString("maxH_preference","130")), this);
        if(rangepreference.equals("S"))
            seekBar = new RangeSeekBar<Integer>(Integer.valueOf(sharedPref.getString("minS_preference","0")), Integer.valueOf(sharedPref.getString("maxS_preference","255")), this);
        if(rangepreference.equals("V"))
            seekBar = new RangeSeekBar<Integer>(Integer.valueOf(sharedPref.getString("minV_preference","0")), Integer.valueOf(sharedPref.getString("maxV_preference","255")), this);

        //RangeSeekBar<Integer> seekBar = new RangeSeekBar<Integer>(MIN_TH, MAX_TH, this);
        seekBar.setId(R.id.rangeseekbar);
        seekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Integer>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
                rangeSeekBarChanged(minValue, maxValue);
            }

        });
        setVisibility();
        // add RangeSeekBar to pre-defined layout
        ViewGroup layout = (ViewGroup) findViewById(R.id.seekbarholder);
        layout.addView(seekBar);
      //locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        FileOutputStream fos;

        if (image != null) {
            try {
                String fileName = "temp_image.jpg";
                String fileURL = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() +
                        "/" + fileName;

                fos = new FileOutputStream(fileURL);
                image.compress(Bitmap.CompressFormat.JPEG, compressRatio, fos);
                outState.putBoolean("image", true);
                image.recycle();
                System.gc();
            } catch (Exception e) {
                Log.e(TAG, "Error " + e.getMessage());
            }
        }
        if (state[CROP_IMAGE]) {
            try {
                String fileName = "temp_cropped.jpg";
                String fileURL = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() +
                        "/" + fileName;

                fos = new FileOutputStream(fileURL);
                croppedimage.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                outState.putBoolean("croppedimage", true);
                fos.close();
                croppedimage.recycle();
                System.gc();

            } catch (Exception e) {
                Log.e(TAG, "Error " + e.getMessage());
            }
        }
        if (state[TREAT_IMAGE]) {
            try {
                String fileName = "temp_treated.jpg";
                String fileURL = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() +
                        "/" + fileName;
                fos = new FileOutputStream(fileURL);

                ImageView imv = (ImageView) findViewById(R.id.image_intro);
                ((BitmapDrawable) imv.getDrawable()).getBitmap().compress(Bitmap.CompressFormat.JPEG, 100, fos);
                outState.putBoolean("treatedimage", true);
                fos.close();


            } catch (Exception e) {
                Log.e(TAG, "Error " + e.getMessage());
            }
        }

        if (image_uri != null) {
            outState.putString("image_uri", image_uri.getPath());
        }
        outState.putIntArray("colours", colours);
        outState.putBoolean("cropping", state[CROP_IMAGE]);
        outState.putBoolean("treated", state[TREAT_IMAGE]);

        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.containsKey("cropping")) {
            state[CROP_IMAGE] = savedInstanceState.getBoolean("cropping");
        }
        if (savedInstanceState.containsKey("treated")) {
            state[TREAT_IMAGE] = savedInstanceState.getBoolean("treated");
        }

        if (savedInstanceState.containsKey("image_uri")) {
            image_uri = Uri.parse(savedInstanceState.getString("image_uri"));
        }

        if (savedInstanceState.getBoolean("image")) {
            String fileName = "temp_image.jpg";
            String fileURL = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() +
                    "/" + fileName;
            File file = new File(fileURL);
            try {
                InputStream imageStream = getContentResolver().openInputStream(Uri.fromFile(file));
                image = Util.decodeScaledBitmapFromFile(file.getPath(), MAX_SIZE, MAX_SIZE);
                //  image = BitmapFactory.decodeStream(imageStream);

            } catch (FileNotFoundException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        if (state[CROP_IMAGE]) {
            String fileName = "temp_cropped.jpg";
            String fileURL = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() +
                    "/" + fileName;
            File file = new File(fileURL);
            try {
                InputStream imageStream = getContentResolver().openInputStream(Uri.fromFile(file));
                croppedimage = BitmapFactory.decodeStream(imageStream);
                imageStream.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        if (state[TREAT_IMAGE]) {
            String fileName = "temp_treated.jpg";
            String fileURL = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath() +
                    "/" + fileName;
            File file = new File(fileURL);
            try {
                InputStream imageStream = getContentResolver().openInputStream(Uri.fromFile(file));
                ImageView imv = (ImageView) findViewById(R.id.image_intro);
                imv.setImageBitmap(BitmapFactory.decodeStream(imageStream));
                imageStream.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        ImageView imv = (ImageView) findViewById(R.id.image_intro);
        colours = savedInstanceState.getIntArray("colours");
        if ((!state[TREAT_IMAGE]) && state[CROP_IMAGE]) imv.setImageBitmap(croppedimage);
        else {

            imv.setImageBitmap(image);
        }
        TextView imc = (TextView) findViewById(R.id.textView);
        imc.setBackgroundColor(Color.rgb(colours[COLOUR_R], colours[COLOUR_G], colours[COLOUR_B]));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_intro, menu);
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            android.app.Fragment fragment= new SettingsFragment();

            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, fragment, "first")
                    .addToBackStack("first")
                    .commit();
        }
        return true;
    }

    private void rangeSeekBarChanged(int minValue, int maxValue) {
        {

            TextView imc = (TextView) findViewById(R.id.textView);
            imc.setVisibility(View.VISIBLE);
            ImageButton cropButton = (ImageButton) findViewById(R.id.button_crop);
            cropButton.setVisibility(View.GONE);
            ImageButton sendButton = (ImageButton) findViewById(R.id.button_send);
            sendButton.setVisibility(View.VISIBLE);
            ImageButton histButton = (ImageButton) findViewById(R.id.button_histogram);
            histButton.setVisibility(View.VISIBLE);
            ImageButton revertButton = (ImageButton) findViewById(R.id.button_revert);
            revertButton.setVisibility(View.VISIBLE);
            ImageView imagePalette = (ImageView) findViewById(R.id.palette);
            imagePalette.setVisibility(View.GONE);
            ImageWrapper imw = new ImageWrapper();
            if (image_uri == null)
                image = Util.decodeSampledBitmapFromResource(getResources(), R.drawable.platanus_hispanica, MAX_SIZE, MAX_SIZE);
            if (image_uri != null)
                image = Util.decodeSampledBitmapFromUri(image_uri, MAX_SIZE, MAX_SIZE,this);
            // handle changed range values
            try {
                Log.i(TAG, "User selected new range values: MIN=" + minValue + ", MAX=" + maxValue);
                ImageView imv = (ImageView) findViewById(R.id.image_intro);
                if (state[CROP_IMAGE]) imw.setBitmap(croppedimage);
                else imw.setBitmap(image);
                imw.setMinV(minValue);
                imw.setMaxV(maxValue);

                ProcessImageTask pit = new ProcessImageTask();
                imw = (ImageWrapper) pit.execute(imw).get();
                treatedimage = imw.getBitmap();
                imv.setImageBitmap(treatedimage);
                histogram = imw.getHistogram();
                histograma=imw.getHist();
                mascara=imw.getMask();
                imc.setBackgroundColor(Color.rgb(imw.getR().intValue(), imw.getG().intValue(), imw.getB().intValue()));
                state[TREAT_IMAGE] = true;

            } catch (Exception e) {
                Log.e(TAG, "Error " + e.getMessage());

            }
        }
    }

    /******************************************/
    //    PICK_IMAGE CALL

    /******************************************/

    public void pickImage(View view) {
        // Do something in response to button click

        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, PICK_IMAGE);

    }
    /******************************************/
    //    CROP_IMAGE CALL

    /******************************************/
    public void cropImage(View view) {
        Intent intent = new Intent(this, CropImageActivity.class);
        ImageView imv = (ImageView) findViewById(R.id.image_intro);
        try {
            image = ((BitmapDrawable) imv.getDrawable()).getBitmap();
            croppedimage_uri = Uri.fromFile(createImageFile());

        } catch (IOException e) {
            e.printStackTrace();
        }


        //Convert to byte array
        if (getImageUri() != null) {
            intent.putExtra("image", getImageUri().toString());//tostring for passing it
            intent.putExtra("imageSave", croppedimage_uri.toString());//tostring for passing it
            intent.putExtra(RETURN_DATA, true);
            //     intent.putExtra(RETURN_DATA_AS_BITMAP, true);
        } else {
            intent.putExtra("image", "default");
        }
        intent.putExtra(CropImageActivity.SCALE, true);
        intent.putExtra(CropImageActivity.ASPECT_X, 3);
        intent.putExtra(CropImageActivity.ASPECT_Y, 2);
        try {

            startActivityForResult(intent, CROP_IMAGE);
        } catch (Exception e) {
            Log.e(TAG, "Error calling crop" + e.getMessage());
        }
    }

    /******************************************/
    //    CAPTURE IMAGE CALL

    /******************************************/
    public void captureImage(View view) {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // Ensure that there's a camera activity to handle the intent
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    //
                    image_uri = Uri.fromFile(photoFile);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.i(TAG, "Error");
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
                    takePictureIntent.putExtra(MediaStore.EXTRA_SCREEN_ORIENTATION, getResources().getConfiguration().orientation);
                    //    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,uriSavedImage);
                    startActivityForResult(takePictureIntent, CAPTURE_IMAGE);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error" + e.getMessage());
        }
    }

    /******************************************/
    //    SEND DATA CALL

    /******************************************/

    public void revert(View view) throws IOException {
        updateVisibility();
    }


    public void showHistogram(View view) throws IOException {

        state[SHOW_HISTOGRAM] = true;

        // TextView imc = (TextView) findViewById(R.id.textView);
        //  imc.setBackgroundColor(Color.rgb(colours[COLOUR_R], colours[COLOUR_G], colours[COLOUR_B]));
        RangeSeekBar<Integer> rangeSeekBar = (RangeSeekBar<Integer>) findViewById(R.id.rangeseekbar);
        rangeSeekBar.setVisibility(View.GONE);
        ImageButton histButton = (ImageButton) findViewById(R.id.button_histogram);
        histButton.setVisibility(View.GONE);
        ImageButton revertButton = (ImageButton) findViewById(R.id.button_revert);
        revertButton.setVisibility(View.VISIBLE);
        ImageView imagePalette = (ImageView) findViewById(R.id.palette);
        imagePalette.setVisibility(View.GONE);
        ImageView imv = (ImageView) findViewById(R.id.image_intro);
        imv.setImageBitmap(histogram);




    }
    /******************************************/
    //    SEND DATA CALLfc

    /******************************************/
    public void sendData(View view) {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        try {

            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
            String formattedDate = df.format(c.getTime());
            String result = "";


            if (image_uri != null) {
                UploadImageTask task1 = new UploadImageTask(this, new Callback() {
                    public void run(Object result) {
                        upload = result.toString();
                        //do something here with the result
                    }
                });

                if (state[PICK_IMAGE]) {
                    File f = new File(Util.getRealPathFromURI(this,image_uri));
                    task1.execute(f, 1);
                } else {
                    File f = new File(image_uri.getPath());
                    task1.execute(f, 1);
                }



                UploadImageTask task2 = new UploadImageTask(this, new Callback() {
                    public void run(Object result) {
                        //do something here with the result
                        upload = result.toString();
                    }
                });

                File f = createJSONFile(histograma);
                task2.execute(f, 2);

                UploadImageTask task3 = new UploadImageTask(this, new Callback() {
                    public void run(Object result) {
                        //do something here with the result
                        upload = result.toString();

                    }
                });
                ImageView imv = (ImageView) findViewById(R.id.image_intro);
                Bitmap bm = ((BitmapDrawable) imv.getDrawable()).getBitmap();
              f = createImageFile(bm);
                task3.execute(f, 3);



                UploadImageTask task4 = new UploadImageTask(this, new Callback() {
                    public void run(Object result) {
                        //do something here with the result
                        upload = result.toString();

                    }
                });

                f = createJSONFile(mascara);
                task4.execute(f, 4);



                UploadImageTask task5 = new UploadImageTask(this, new Callback() {
                    public void run(Object result) {
                        //do something here with the result
                        upload = result.toString();

                    }
                });

                f = createImageFile(histogram);
                task5.execute(f, 5);
                resetVariables();
            }



        } catch (Exception e) {
            Log.e(TAG, "Error" + e.getMessage());
        }

    }

    /***************************************************************/
    /* ON ACTIVITY RESULT                                          */

    /***************************************************************/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent returnedIntent) {
        super.onActivityResult(requestCode, resultCode, returnedIntent);

        switch (requestCode) {
            case PICK_IMAGE:
                if (resultCode == RESULT_OK) {


                    resetVisibility(PICK_IMAGE);
                    try {

                        state[PICK_IMAGE] = true;
                        state[CROP_IMAGE] = false;
                        state[TREAT_IMAGE] = false;
                        captura = String.valueOf(System.currentTimeMillis() / 1000L);
                        ImageView imv = (ImageView) findViewById(R.id.image_intro);
                        image_uri = returnedIntent.getData();
                        InputStream is = getContentResolver().openInputStream(returnedIntent.getData());
                        imv.setImageBitmap(BitmapFactory.decodeStream(is));
                        is.close();
                        RangeSeekBar<Integer> seekBar = (RangeSeekBar<Integer>) findViewById(R.id.rangeseekbar);
                        seekBar.setVisibility(View.GONE);


                    } catch (IOException e) {
                        Log.e(TAG, "Error reading from gallery" + e.getMessage());
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading from gallery" + e.getMessage());
                    }
                }
                break;
            case CAPTURE_IMAGE:
                try {
                    if (resultCode == RESULT_OK) {
                        resetVisibility(CAPTURE_IMAGE);
                        captura = String.valueOf(System.currentTimeMillis() / 1000L);
                        state[PICK_IMAGE] = false;
                        state[CROP_IMAGE] = false;
                        state[TREAT_IMAGE] = false;
                        TextView imc = (TextView) findViewById(R.id.textView);
                        imc.setBackgroundColor(Color.rgb(255, 255, 255));
                        RangeSeekBar<Integer> seekBar = (RangeSeekBar<Integer>) findViewById(R.id.rangeseekbar);
                        seekBar.setVisibility(View.GONE);
                        if (returnedIntent != null) {
                            setPic();
                            Bundle extras = returnedIntent.getExtras();
                            setSelectedImage(returnedIntent.getData());
                        } else {

                            ImageView imv = (ImageView) findViewById(R.id.image_intro);
                            image = Util.decodeSampledBitmapFromUri(image_uri, MAX_SIZE, MAX_SIZE,this);
                            imv.setImageBitmap(image);

                            gpstracker=new GPSTracking(this);
                            if(gpstracker.canGetLocation())
                            {
                             latitude=String.valueOf(gpstracker.getLatitude());
                                longitude=String.valueOf(gpstracker.getLongitude());
                            }
                            else
                            {
                                gpstracker.showSettingsAlert();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error " + e.getMessage());
                }

                break;
            case CROP_IMAGE:
                try {

                    if (resultCode == RESULT_OK) {
                        state[CROP_IMAGE] = true;
                        state[TREAT_IMAGE] = false;
                        RangeSeekBar<Integer> seekBar = (RangeSeekBar<Integer>) findViewById(R.id.rangeseekbar);
                        seekBar.setVisibility(View.VISIBLE);
                        ImageTreatment imt = null;
                        Bundle extras = returnedIntent.getExtras();
                        if (extras != null) {
                            croppedimage_uri = Uri.parse(extras.getString(RETURN_DATA));
                            // croppedimage=extras.getParcelable(RETURN_DATA_AS_BITMAP);
                            if (croppedimage_uri != null) {
                                InputStream is = getContentResolver().openInputStream(croppedimage_uri);
                                croppedimage = BitmapFactory.decodeStream(is);
                                is.close();
                            } else {
                                image = BitmapFactory.decodeResource(getResources(), R.drawable.platanus_hispanica);
                            }
                            ImageView imv = (ImageView) findViewById(R.id.image_intro);
                            imv.setImageBitmap(croppedimage);
                            colours[COLOUR_R] = (int) imt.getRedColor();
                            colours[COLOUR_B] = (int) imt.getBlueColor();
                            colours[COLOUR_G] = (int) imt.getGreenColor();

                        }
                    }
                } catch (OutOfMemoryError e) {
                    Log.e(TAG, "Error " + e.getMessage());

                } catch (IOException e) {
                    Log.e(TAG, "Error " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Error " + e.getMessage());
                }
        }

    }

    public void setSelectedImage(Uri s) throws IOException {
        image_uri = s;
        image = Util.decodeSampledBitmapFromUri(image_uri, MAX_SIZE, MAX_SIZE,this);
        croppedimage = Util.decodeSampledBitmapFromUri(image_uri, MAX_SIZE,MAX_SIZE,this);
    }

    public Uri getImageUri() {
        return image_uri;
    }


    /**
     * CreateImageFile
     */
    private File createJSONFile(String JSON) throws IOException {
        // Create an image file name
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String imageFileName = "PLATANUS" + timeStamp;


        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File file = File.createTempFile(
                imageFileName,  /* prefix */
                ".txt",         /* suffix */
                storageDir      /* directory */
        );
        FileWriter filewriter = new FileWriter(file);
        filewriter.write(JSON);
        filewriter.flush();
        filewriter.close();
        // Save a file: path for use with ACTION_VIEW intents
        return file;
    }
    /**
     * CreateImageFile
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String imageFileName = "PLATANUS" + timeStamp;


        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }


    private File createImageFile(Bitmap bm) throws IOException {
        // Create an image file name
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000L);
        String imageFileName = "PLATANUS" + timeStamp;


        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        OutputStream os;
        try {
            os = new FileOutputStream(image);
            bm.compress(Bitmap.CompressFormat.JPEG, 100, os);
            os.flush();
            os.close();
        } catch (Exception e) {
            Log.e(TAG, "Error writing bitmap", e);
        }
        return image;
    }


    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, image_uri);
        sendBroadcast(mediaScanIntent);
    }

    private void setPic() {
        // Get the dimensions of the View
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        // Get the dimensions of the bitmap
        ImageView imv = (ImageView) findViewById(R.id.image_intro);
        int targetW = imv.getMaxWidth();
        int targetH = imv.getMaxHeight();
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        Bitmap bitmap = loadBitmap(image_uri.getPath());
        int photoW = bitmap.getWidth();
        int photoH = bitmap.getHeight();
        imv.setImageBitmap(bitmap);
        galleryAddPic();
    }


    private Bitmap loadBitmap(String url) {
        Bitmap bm = null;
        InputStream is = null;
        BufferedInputStream bis = null;
        try {
            URLConnection conn = new URL(url).openConnection();
            conn.connect();
            is = conn.getInputStream();
            bis = new BufferedInputStream(is, 8192);
            bm = BitmapFactory.decodeStream(bis);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bm;
    }

    /*----Method to Check GPS is enable or disable ----- */
    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext()
                .getContentResolver();
        boolean gpsStatus =locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        return gpsStatus;
    }






    /******************************************************/
        /*PROCESS IMAGE TASK                                  */
    private class ProcessImageTask extends AsyncTask<Object, Integer, Object> {
        public ProcessImageTask() {
        }

        @Override
        protected Object doInBackground(Object... params) {
            ImageWrapper imw = (ImageWrapper) params[0];
            //Convert to byte arr
            ImageTreatment imt = new ImageTreatment(imw.getBitmap());
            imt.performImageSmoothing();
            imt.generateHSV();
            imt.performDoubleThresholding(
                    imw.getMinH(),
                    imw.getMaxH(),
                    imw.getMinS(),
                    imw.getMaxS(),
                    imw.getMinV(),
                    imw.getMaxV());
            imt.performImageFusion();
            imt.drawContours();
            imw.setHistogram(imt.calchsvHistogram());
            imw.setHist(imt.calchsvHistogramJSON());
            imw.setMask(imt.getmaskJSON());
            imw.setBitmap(imt.getBitmap());
            imw.setR(imt.getRedColor());
            imw.setB(imt.getBlueColor());
            imw.setB(imt.getBlueColor());
            imw.setG(imt.getGreenColor());
            return imw;

        }
    }

    /******************************************************/
        /* UPLOAD IMAGE TASK                                  */

    private class UploadImageTask extends AsyncTask<Object, Integer, String> {

        private Context context;
        private String msg = "";
        private boolean running = true;
        private ProgressDialog dialog;
        Callback callback;


        public UploadImageTask(Activity activity, Callback callback) {
            this.context = activity;
            dialog = new ProgressDialog(context);
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(true);

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            upload = "";
            dialog.setMessage("Uploading picture, please wait...");
            dialog.show();
        }

        @Override
        protected String doInBackground(Object... params) {
            // TODO Auto-generated method stub
            String result = "";
            try {
                result = doUploadinBackground((File) params[0], (Integer) params[1]);
            } catch (Exception e) {
                Log.d(TAG, "ERROR " + e.getMessage());
            }
            return result;
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //    callback.run(result);

            croppedimage = BitmapFactory.decodeResource(getResources(), R.drawable.platanus_hispanica);
            ImageView imv = (ImageView) findViewById(R.id.image_intro);
            imv.setImageBitmap(croppedimage);
            if (result.equals("success")) dialog.setMessage("Uploaded OK");
            else dialog.setMessage("Upload failure");
            setVisibility();
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

        /**
         * Method uploads the image using http multipart form data.
         * We are not using the default httpclient coming with android we are using the new from apache
         * they are placed in libs folder of the application
         *
         * @param file
         * @return
         * @throws Exception
         */


        String doUploadinBackground(File file, int numCaptura) throws Exception {
            String responseString = null;
            try {
                String url = addParamsToUrl(URL_UPLOAD, numCaptura, file.getName());
                HttpEntity mpe = null;
                HttpPost post = new HttpPost(url);
                String boundary = "-----" + System.currentTimeMillis();
                post.setHeader("ENCTYPE", "multipart/form-data");
                post.addHeader("Connection", "Keep-Alive");
                HttpClient client = new DefaultHttpClient();
                client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.STRICT);
                builder.setCharset(Charset.forName(HTTP.UTF_8));
                builder.addBinaryBody("file", file);
                mpe = builder.build();
                post.setEntity(mpe);
                HttpResponse response = client.execute(post);
                HttpEntity entity = response.getEntity();
                responseString = EntityUtils.toString(response.getEntity());
                client.getConnectionManager().shutdown();
                response.getStatusLine();  // CONSIDER  Detect server complaints
                entity.consumeContent();
            } catch (Exception e) {
                Log.e(TAG, "ERROR" + e.getMessage());
            }
            ;

            Log.e("httpPost", "Response status: " + responseString);
            if (responseString.equals("success")) {
                return "success";
            } else {
                return "failure";
            }
        }

        private String addParamsToUrl(String url, int numcaptura, String filename) throws UnknownHostException {
            if (!url.endsWith("?"))
                url += "?";
            List<NameValuePair> params = new LinkedList<NameValuePair>();
            params.add(new BasicNameValuePair("p", ACCESSKEY));
            params.add(new BasicNameValuePair("ip", Inet4Address.getLocalHost().toString()));
            String modelodispositivo = "OS VERSION " + System.getProperty("os.version") + "SDK " + android.os.Build.VERSION.SDK + "DEVICE " +
                    android.os.Build.DEVICE + "MODEL " + android.os.Build.MODEL + "PRODUCT " + android.os.Build.PRODUCT;
            params.add(new BasicNameValuePair("modelodispositivo", modelodispositivo));
            if (longitude != null && latitude != null) {
                params.add(new BasicNameValuePair("l", latitude + "," + longitude));
                params.add(new BasicNameValuePair("proveedor", LocationManager
                        .GPS_PROVIDER));
            }
            params.add(new BasicNameValuePair("e", "riromu@upv.es"));
            params.add(new BasicNameValuePair("captura", captura));
            params.add((new BasicNameValuePair("numcaptura", String.valueOf(numcaptura))));
            params.add((new BasicNameValuePair("filename", filename)));
            params.add((new BasicNameValuePair("version", "1")));
            params.add((new BasicNameValuePair("o", "")));
            if ((colours[COLOUR_R] == 255) && (colours[COLOUR_G] == 255) && (colours[COLOUR_B] == 255)) {
                params.add((new BasicNameValuePair("R", "00")));
                params.add((new BasicNameValuePair("G", "00")));
                params.add((new BasicNameValuePair("B", "00")));
            } else {
                params.add((new BasicNameValuePair("R", String.format("%02x", colours[COLOUR_R]))));
                params.add((new BasicNameValuePair("G", String.format("%02x", colours[COLOUR_G]))));
                params.add((new BasicNameValuePair("B", String.format("%02x", colours[COLOUR_B]))));

            }
            String paramString = URLEncodedUtils.format(params, "utf-8");
            url += paramString;
            return url;
        }
    }


    public interface Callback {

        void run(Object result);
    }


    private void resetVariables()

    {
        image=null;
        croppedimage=null;
        treatedimage=null;
        histogram=null;
        image_uri=null;
        croppedimage_uri=null;


    }

    private void resetVisibility(int code)

    {


        RangeSeekBar<Integer> seekBar = (RangeSeekBar<Integer>) findViewById(R.id.rangeseekbar);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String rangepreference=(String)sharedPref.getString("rangepreference", "H");
        if(rangepreference.equals("H"))
            seekBar = new RangeSeekBar<Integer>(Integer.valueOf(sharedPref.getString("minH_preference","60")), Integer.valueOf(sharedPref.getString("maxH_preference","130")), this);
        if(rangepreference.equals("S"))
            seekBar = new RangeSeekBar<Integer>(Integer.valueOf(sharedPref.getString("minS_preference","0")), Integer.valueOf(sharedPref.getString("maxS_preference","255")), this);
        if(rangepreference.equals("V"))
            seekBar = new RangeSeekBar<Integer>(Integer.valueOf(sharedPref.getString("minV_preference","0")), Integer.valueOf(sharedPref.getString("maxV_preference","255")), this);


        seekBar.setSelectedMinValue(MIN_TH);
        seekBar.setSelectedMaxValue(MAX_TH);

        TextView imc = (TextView) findViewById(R.id.textView);
        imc.setVisibility(View.GONE);
    if((code==CAPTURE_IMAGE)||(code==PICK_IMAGE)){
        ImageButton cropButton = (ImageButton) findViewById(R.id.button_crop);
        cropButton.setVisibility(View.VISIBLE);
    }
        ImageButton sendButton = (ImageButton) findViewById(R.id.button_send);
        sendButton.setVisibility(View.GONE);
        ImageButton histButton = (ImageButton) findViewById(R.id.button_histogram);
        histButton.setVisibility(View.GONE);
        ImageButton revertButton = (ImageButton) findViewById(R.id.button_revert);
        revertButton.setVisibility(View.GONE);
        ImageView imagePalette = (ImageView) findViewById(R.id.palette);
        imagePalette.setVisibility(View.GONE);
    }

    private void setVisibility() {


        TextView imc = (TextView) findViewById(R.id.textView);
        imc.setVisibility(View.GONE);
        ImageButton sendButton = (ImageButton) findViewById(R.id.button_send);
        sendButton.setVisibility(View.GONE);
        ImageButton histButton = (ImageButton) findViewById(R.id.button_histogram);
        histButton.setVisibility(View.GONE);
        ImageButton revertButton = (ImageButton) findViewById(R.id.button_revert);
        revertButton.setVisibility(View.GONE);
        ImageView imagePalette = (ImageView) findViewById(R.id.palette);
        imagePalette.setVisibility(View.GONE);
    }

    private void updateVisibility()

    {
        Boolean done = false;
        TextView imc = (TextView) findViewById(R.id.textView);
        //   imc.setBackgroundColor(Color.rgb(255, 255, 255));
        RangeSeekBar<Integer> rangeSeekBar = (RangeSeekBar<Integer>) findViewById(R.id.rangeseekbar);
        rangeSeekBar.setSelectedMinValue(MIN_TH);
        rangeSeekBar.setSelectedMaxValue(MAX_TH);

        ImageView imv = (ImageView) findViewById(R.id.image_intro);

        if (state[CROP_IMAGE]) {
            rangeSeekBar.setVisibility(View.VISIBLE);
        }
        if (!state[CROP_IMAGE]) {
            rangeSeekBar.setVisibility(View.GONE);
        }

        if (state[TREAT_IMAGE] && state[CROP_IMAGE] && (!state[SHOW_HISTOGRAM])) {
            state[TREAT_IMAGE] = false;
            imv.setImageBitmap(croppedimage);
            done = true;
        }

        if (state[TREAT_IMAGE] && (!state[CROP_IMAGE]) && (!state[SHOW_HISTOGRAM]) && (!done)) {
            state[TREAT_IMAGE] = false;

            imv.setImageBitmap(image);
            ImageButton cropButton = (ImageButton) findViewById(R.id.button_crop);
            cropButton.setVisibility(View.VISIBLE);
            imc.setVisibility(View.GONE);
            ImageButton sendButton = (ImageButton) findViewById(R.id.button_send);
            sendButton.setVisibility(View.GONE);
            ImageButton histButton = (ImageButton) findViewById(R.id.button_histogram);
            histButton.setVisibility(View.GONE);
            ImageButton revertButton = (ImageButton) findViewById(R.id.button_revert);
            revertButton.setVisibility(View.GONE);
            ImageView imagePalette = (ImageView) findViewById(R.id.palette);
            imagePalette.setVisibility(View.GONE);
            done = true;
        }
        if ((!state[TREAT_IMAGE]) && state[CROP_IMAGE] && (!state[SHOW_HISTOGRAM]) && (!done)) {
            state[CROP_IMAGE] = false;
            try {
                InputStream is = getContentResolver().openInputStream(image_uri);
                imv.setImageBitmap(BitmapFactory.decodeStream(is));
                is.close();
            } catch (FileNotFoundException e) {
                Log.e(TAG, "File not found" + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "File not found" + e.getMessage());
            }
            croppedimage.recycle();
            done = true;
        }
        if ((!state[TREAT_IMAGE]) && (!state[CROP_IMAGE]) && (!state[SHOW_HISTOGRAM]) && (!done)) {
            image.recycle();
            image_uri = null;
            imv.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.platanus_hispanica));
            done = true;
        }
        if ((state[SHOW_HISTOGRAM]) && (!done)) {
            state[SHOW_HISTOGRAM] = false;
            rangeSeekBar.setVisibility(View.VISIBLE);
            imv.setImageBitmap(treatedimage);
            imc.setVisibility(View.VISIBLE);
//                imc.setBackgroundColor(Color.rgb(colours[COLOUR_R], colours[COLOUR_G], colours[COLOUR_B]));

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {


            if (getFragmentManager().findFragmentByTag("first")!=null)
            {
                getFragmentManager().popBackStack();
                removeCurrentFragment();

                return false;

            }

            if (getFragmentManager().getBackStackEntryCount() == 0)
            {
                this.finish();
                return false;
            }
            else
            {
                getSupportFragmentManager().popBackStack();
                removeCurrentFragment();

                return false;
            }



        }

        return super.onKeyDown(keyCode, event);
    }


    public void removeCurrentFragment()
    {
        android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        Fragment currentFrag =  getSupportFragmentManager().findFragmentById(R.id.introlayout);


        String fragName = "NONE";

        if (currentFrag!=null)
            fragName = currentFrag.getClass().getSimpleName();


        if (currentFrag != null)
            transaction.remove(currentFrag);

        transaction.commit();

    }
    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);


            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);





        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
                savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);
            view.setBackgroundColor(getResources().getColor(android.R.color.white));

            return view;
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
        }
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
        {
            //IT NEVER GETS IN HERE!
            if (key.equals(""))
            {
                // Set summary to be the user-description for the selected value
                Preference exercisesPref = findPreference(key);
                exercisesPref.setSummary(sharedPreferences.getString(key, ""));
            }
        }
    }
}
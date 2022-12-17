package edu.upc.ac.jorge.GhostHunters;

import static android.hardware.Sensor.TYPE_ACCELEROMETER;
import static android.hardware.Sensor.TYPE_GYROSCOPE;
import static android.hardware.Sensor.TYPE_MAGNETIC_FIELD;

import static java.lang.String.format;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements SensorEventListener {
    public static final String EXTRA_MESSAGE = "edu.upc.ac.jorge.IMSensorLab.MESSAGE";

    private GLSurfaceView glView;   // Use GLSurfaceView
    //Sensor manager -  Allows us to check for all available sensors and request 'em
    private SensorManager sensorManager = null;
    private Sensor gyroscopeSensor = null; //Used for horizontal plane Game orientation
    private Sensor accelerometerSensor = null; //Used for vertical plane Game orientation
    private Sensor magneticfieldSensor = null; //Used for global orientation
    private Sensor stepDetectorSensor = null; //Fitness Sensor - count steps while walking //    //private Sensor stepCounterSensor = null; //Fitness Sensor - show total steps from phone reboo
    //Location handling - GPS & Network
    LocationManager locationManager;//LocationListener locationListener = new MyLocationListener(); //private Location currentBestLocation = null;
    Location locationGPS=null;// GPS_PROVIDER
    Location locationNet=null;// NETWORK_PROVIDER
    long GPSLocationTime = 0; long NetLocationTime = 0;
    private int numstepsActuales = 0;
    //camera vars
    private CameraCaptureSession myCameraCaptureSession;
    private CameraManager myCameraManager;
    private CameraDevice myCameraDevice;
    private TextureView myTextureView;
    private CaptureRequest.Builder myCaptureRequestBuilder;

    //audio vars
    private MediaPlayer MyBuuPlayer = null;
    private MediaPlayer MyNoPlayer = null;
    private MediaPlayer MyAuchPlayer = null;
    private MediaPlayer MyNewGhostPlayer = null;
    private MediaPlayer MyGameOverPlayer =null;
    private MediaPlayer MyAmbientPlayer = null;
    private final static int MAX_VOLUME = 100;

    //Ghost World locations and screen coords // quaternions for vectors in DEV coordinates
    private Quaternion GhostsDev[] = new Quaternion[5];  // Ghost positions in WORLD coordinates
    //GHOST_0 Reference in world coords, according to actual DEV coords
    float GhostDegreeAbovePlayer = 0; float GhostDegreeAroundPlayer = 0;
    float distanceToGhost0Ref = 10.f;
    //ghosts in display range info - x degree in WORLD coords
    boolean ghostsDisplayed[] = new boolean[5];
    int ghostdegreeX[] =  new int[5];
    //vars to use in sensors to get orientation - azimut, pitch and roll of phone
    float[] mGravity = new float[3];

    float[] mGeomagnetic = new float[3];
    float azimut = 0.f; float pitch = 0.f; float roll = 0.f;
    //coords in WORLD, around and above, distance is ignored.
    int rotxGhosts[] = new int[5];
    int rotyGhosts[] = new int[5];
    //simulate distance by display size
    int sizeGhosts[] = new int[5]; //adjust so it can be viewed properly
    //Display coords of each ghost
    Random rGenerator = new Random();
    //coords in DEV - display x-y
    int xghost1 = 0;    int xghost2 = 0;    int xghost3 = 0;    int xghost4 = 0;
    int yghost1 = 0;    int yghost2 = 0;    int yghost3 = 0;    int yghost4 = 0;
    int HitDamage = 10; //Dmg per freeze tick

    //game handling
    ImageView myImageHit;
    TextView tvGameOver;
    //public Timer GameLoopTimer;
    long startTime = 0;
    Handler timerHandler = new Handler();
    Handler timerHandler2 = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            GameLoopTask(minutes, seconds);
            //run this task once every second
            timerHandler.postDelayed(this, 1000);
        }
    };
    private boolean lastkilltimeok = true;
    Runnable timerRunnable2 = new Runnable() {
        @Override
        public void run() {
            //myImageHit.setAlpha(0.0f);
            //hitimageview.setVisibility(View.INVISIBLE);
            lastkilltimeok=true;
        }
    };

    //View helpers
    TextView tvgssensing = null;
    TextView tvgssensed = null;
    TextView textViewAzimut = null;
    TextView textViewPitch = null;
    TextView textViewRoll = null;
    TextView textViewGhostY = null;
    TextView textViewAccelX = null;
    TextView textViewAccelY = null;
    TextView textViewAccelZ = null;
    TextView textViewInfo = null;


    //NEW VARS FOR PDS PRESENTATION - 17.12.2022

    boolean bFilterEnabledX = true;
    boolean bFilterEnabledY = true;
    Button btFilterOnOffX = null;
    Button btFilterOnOffY = null;
    boolean bDriftGame = false;
    Button btStartGameFull;
    Button btStartGameDrift;
    float[] mFilerNoise = new float[3];

    //WARNING! No son realmente ImageView.. son GifImageView!! de pl.droidxxxx
    ImageView hitimageview = null;
    ImageView Ghost1 = null;
    ImageView Ghost2 = null;
    ImageView Ghost3 = null;
    ImageView Ghost4 = null;
    ImageView imageViewArrowLeft = null;
    ImageView imageViewArrowRight = null;
    ImageView imageViewLogo = null;

    private Quaternion Xini = new Quaternion(0f, 1f, 0f, 0f);  // X in DEV coordinates
    private Quaternion Yini = new Quaternion(0f, 0f, 0f, -1f);  // Y in DEV coordinates
    private Quaternion Zini = new Quaternion(0f, 0f, 1f, 0f);  // Z in DEV coordinates

    //Ghost World locations and screen coords // quaternions for vectors in DEV coordinates
    private Quaternion Ghost1Dev = new Quaternion(0f, 2f, 3f, 0f);  // Ghost positions in WORLD coordinates
    private Quaternion Ghost2Dev = new Quaternion(0f, 2f, 3f, 0f);  // Ghost2 in WORLD coordinates
    private Quaternion Ghost3Dev = new Quaternion(0f, 2f, 3f, 0f);  // Ghost2 in WORLD coordinates
    private Quaternion Ghost4Dev = new Quaternion(0f, 2f, 3f, 0f);  // Ghost2 in WORLD coordinates


    //Other
    private DisplayMetrics displayMetrics = null;
    private int SCREEN_height = 0;
    private int SCREEN_width = 0;
    private double timestamp = 0d;
    private double dTns = 1d;
    private double dT = 1d;
    private static final double NS2S = 1000000000.0d;  // nanosecs to secs


    /**
     * Called when the activity is first created.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //ask for permisions using RxPermissions
        //RxPermissions(this).request(Manifest.permission.ACTIVITY_RECOGNITION)
        //        .subscribe { isGranted ->
        //        Log.d("TAG", "Is ACTIVITY_RECOGNITION permission granted: $isGranted")
        //}


        //
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();

        //view helpers load
        if (tvgssensing == null) {
            tvgssensing = (TextView) findViewById(R.id.textViewGhostSense);
            tvgssensed = (TextView) findViewById(R.id.textViewGhosts);
            hitimageview = (ImageView) findViewById(R.id.hitcircle);
            imageViewArrowLeft = (ImageView) findViewById(R.id.imageViewArrorLeft);
            imageViewArrowRight = (ImageView) findViewById(R.id.imageViewArrowRight);
            imageViewLogo = (ImageView) findViewById(R.id.imageViewLogoGH);
            Ghost1 = (ImageView) findViewById(R.id.ghostimg1);
            Ghost2 = (ImageView) findViewById(R.id.ghostimg2);
            Ghost3 = (ImageView) findViewById(R.id.ghostimg3);
            Ghost4 = (ImageView) findViewById(R.id.ghostimg4);
            //imageHit effect handling
            myImageHit = (ImageView) findViewById(R.id.imageHit);
            myImageHit.setAlpha(0.9f);
            tvGameOver = (TextView) findViewById(R.id.textViewGameOver);
            //phone orientation show
            textViewAzimut = (TextView) findViewById(R.id.textViewAzimut);
            textViewPitch = (TextView) findViewById(R.id.textViewPitch);
            textViewRoll = (TextView) findViewById(R.id.textViewRoll);
            textViewGhostY = (TextView) findViewById(R.id.textViewGyroYD);
            textViewAccelX = (TextView) findViewById(R.id.textViewAccelX);
            textViewAccelY = (TextView) findViewById(R.id.textViewAccelY);
            textViewAccelZ = (TextView) findViewById(R.id.textViewAccelZ);
            textViewInfo =(TextView) findViewById(R.id.textViewInfo);
            btFilterOnOffX = (Button) findViewById(R.id.btFilterOnOffX);
            btFilterOnOffY = (Button) findViewById(R.id.btFilterOnOffY);
            btStartGameFull = (Button) findViewById(R.id.btStartGameFull);
            btStartGameDrift = (Button) findViewById(R.id.btStartGameDrift);
        }

        //camera init and show
        myCameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        openCamera();
        //set video clic listener
        myTextureView = findViewById(R.id.textureView);
        myTextureView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                return checkCameraTouch(v, event);
            }
        });

        //Load sounds
        if (MyAmbientPlayer == null) {
            MyAmbientPlayer = MediaPlayer.create(MainActivity.this, R.raw.ambient);
            MyNoPlayer = MediaPlayer.create(MainActivity.this, R.raw.zap);
            MyBuuPlayer = MediaPlayer.create(MainActivity.this, R.raw.buu);
            MyAuchPlayer = MediaPlayer.create(MainActivity.this, R.raw.auch);
            MyGameOverPlayer = MediaPlayer.create(MainActivity.this, R.raw.game_over);
            MyNewGhostPlayer = MediaPlayer.create(MainActivity.this, R.raw.appear);
            //set volumes
            int soundVolumeMax = 100; //Reference max volume
            int soundVolumeGeneral = 75; //Reference max volume
            int soundVolumeAmbient = 40; //50 is too much
            final float volumeAmbient = (float) (1 - (Math.log(MAX_VOLUME - soundVolumeAmbient) / Math.log(MAX_VOLUME)));
            final float volumeGeneral = (float) (1 - (Math.log(MAX_VOLUME - soundVolumeGeneral) / Math.log(MAX_VOLUME)));
            final float volumeMax = (float) (1 - (Math.log(MAX_VOLUME - soundVolumeMax) / Math.log(MAX_VOLUME)));
            MyAmbientPlayer.setVolume(volumeAmbient, volumeAmbient);
            MyBuuPlayer.setVolume(volumeAmbient, volumeGeneral);
            MyNoPlayer.setVolume(volumeAmbient, volumeGeneral);
            MyAuchPlayer.setVolume(volumeAmbient, volumeGeneral);

            MyNewGhostPlayer.setVolume(volumeAmbient, volumeMax);

            //set loops and start game ambient music
            MyAmbientPlayer.setLooping(true);
            //MyBuuPlayer.setLooping(false);MyNoPlayer.setLooping(false);MyAuchPlayer.setLooping(false);MyNewGhostPlayer.setLooping(false);
            MyAmbientPlayer.start();
        }
        //Gif handling - DEPRECATED --> NOW using implementation 'pl.droidsonroids.gif:android-gif-drawable:1.2.+'
        //To show gif animated ghosts and effects
        //ImageView imageView = findViewById(R.id.imageViewGhost1);
        /* from internet*/
        //Glide.with(this)
        //        .load("https://media.giphy.com/media/98uBZTzlXMhkk/giphy.gif")
        //        .into(imageView);
        /*from raw folder*/
        //Glide.with(this)
        //        .load(R.raw.giphy)
        //        .into(imageView);

        //Game loop
        //runs without a timer by reposting this handler at the end of the runnable

        //
        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        SCREEN_height = displayMetrics.heightPixels;
        SCREEN_width = displayMetrics.widthPixels;

        //init step counter sensor & others (gyro, etc...)
        initAppSensors();

    }

    public void switchFilterOnOffX(View view) {
        bFilterEnabledX = ! bFilterEnabledX;
        if(bFilterEnabledX){
            btFilterOnOffX.setText("F. ON");
        }else{
            btFilterOnOffX.setText("F. OFF");
        }
    }

    public void switchFilterOnOffY(View view) {
        bFilterEnabledY = ! bFilterEnabledY;
        if(bFilterEnabledY){
            btFilterOnOffY.setText("F. ON");
        }else{
            btFilterOnOffY.setText("F. OFF");
        }
    }

    public void checkforpermisions(View view) {
        //check for permisions
        int MyRequestCode = 456; //Application specific request code to match with a result reported to onRequestPermissionsResult. Should be >= 0.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            displayToast(10, 20, "Activity recognition granted OK", 2500);
        } else {
            // No explanation needed, we can request the permission.
            displayToast(10, 20, "Activity recognition NOT GRANTED!", 750);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, MyRequestCode);
            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }

        MyRequestCode = 457; //Application specific request code to match with a result reported to onRequestPermissionsResult. Should be >= 0.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            displayToast(10, 40, "Coarse location granted OK", 750);
        } else {
            // No explanation needed, we can request the permission.
            displayToast(10, 40, "Coarse location NOT GRANTED!", 2500);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MyRequestCode);
            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }

        MyRequestCode = 458; //Application specific request code to match with a result reported to onRequestPermissionsResult. Should be >= 0.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            displayToast(10, 40, "Fine location granted OK!", 2500);
        } else {
            // No explanation needed, we can request the permission.
            displayToast(10, 40, "Fine location NOT GRANTED!", 2500);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MyRequestCode);
            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }
    }

    private boolean paused=false;
    private void GameLoopTask(int minutes, int seconds) {
        if (GameOn && (life >0) && !paused) {
            //Perform ghost updates, lifes, sensor check... etc...
            //Input? - NO -> just  handled by events
            //Update? - YES -> Ghost appearing & life reduction
            int timeuntilnextghost = seconds % 5;
            tvgssensing.setText("Sensing ... " + (4 - timeuntilnextghost));

            if(bDriftGame){
                //show fixed ghost at zero position
                int numgaux=1;
                ghosts = numgaux;
                rotxGhosts[numgaux] = 50;//randomdegx; //0 a 360
                rotyGhosts[numgaux] = 10; //randomdegy; //0 a 80
                //simulate distance by display size
                sizeGhosts[numgaux] = 375; //rndsize; //300 a 450
                //CalculateCoords
                RecalculateGhostDisplayCoordsFromPhoneOrientation();
            }
            else if (timeuntilnextghost == 0) { //1 fantasma cada 5 segundos
                if (ghosts < 4) {
                    ghosts += 1;
                    setnewpositionforghost(ghosts);
                    MyNewGhostPlayer.start();
                } else {
                    life -= HitDamage;
                    MyAuchPlayer.start();
                    displayToast(40, 100, "YOU ARE FREEZING!!", 500);
                    scoreval -= 1;
                    float alphaval = (100.f-life) / 100.f;
                    myImageHit.setAlpha(alphaval);
                    myImageHit.setVisibility(View.VISIBLE);
                    if(life == 0){
                        imageViewArrowLeft.setVisibility(View.INVISIBLE);
                        imageViewArrowRight.setVisibility(View.INVISIBLE);
                        imageViewLogo.setVisibility(View.VISIBLE);
                        tvGameOver.setVisibility(View.VISIBLE);

                        if(bUseStartb) {
                            startbutton.setVisibility(View.VISIBLE);// . setText("Restart");
                            startbutton.setText("TRY AGAIN");
                        }
                        MyGameOverPlayer.start();
                    }
                }
            } else {
                //gestionado con runnable2
                //myImageHit.setAlpha(0.0f);
                //hitimageview.setVisibility(View.INVISIBLE);

            }

            //Gestionado con el update de sensores realmente...
            showghosts(ghosts);

            //Update Ghosts 3D screen position? -> NO -> ghost position update according to sensors handled by events
            //Update GPS pos and time
            UpdateGPS();
            UpdateTime(minutes, seconds);
            //Update
            //Update view
            UpdateScore();
            UpdateGhosts();
            UpdateLife();
            //UpdateSteps(); -> NO -> on sensor event
        }
    }

    private void setnewpositionforghost(int numg) {
        if(false){
            //OLD - JUST KEEPT FOR DEBUG AND OTHER... xghostZ are display pixel coords - calculated with world coords
            int minx = 20;  int maxx = 500;
            int randomx = rGenerator.nextInt((maxx - minx) + 1) + minx;
            int miny = 200; int maxy = 1000;
            int randomy = rGenerator.nextInt((maxy - miny) + 1) + miny;
            if (numg == 1) { xghost1 = randomx; yghost1 = randomy;}
            if (numg == 2) { xghost2 = randomx; yghost2 = randomy;}
            if (numg == 3) { xghost3 = randomx; yghost3 = randomy;}
            if (numg == 4) { xghost4 = randomx; yghost4 = randomy;}
        }
        //coords in WORLD, around and above, distance is ignored.
        int mindegx = 1;
        int maxdegx = 359; //all around player
        int randomdegx = rGenerator.nextInt((maxdegx - mindegx) + 1) + mindegx;
        //si va a aparecer en pantalla, evitarlo girándolo 180º
        float displaydeg = getGhostDegreeXInDisplayDegrees(randomdegx);
        if  (displaydeg > -40 && displaydeg<40){
            randomdegx += 180; if(randomdegx>360){randomdegx -= 360;}
        }
        int mindegy = 0; int maxdegy = 80; //from -40 to 40 degrees
        int randomdegy = rGenerator.nextInt((maxdegy - mindegy) + 1) + mindegy;
        randomdegy -= (maxdegy/2);
        final int mins = 300;
        int maxs = 450;
        final int rndsize = rGenerator.nextInt((maxs - mins) + 1) + mins;
        rotxGhosts[numg] = randomdegx;rotyGhosts[numg] = randomdegy;
        //simulate distance by display size
        sizeGhosts[numg] = rndsize;

        //CalculateCoords
        RecalculateGhostDisplayCoordsFromPhoneOrientation();
    }

    private float getGhostDegreeXInDisplayDegrees(float deg){
        //show ghost if ghost in range of view: +-45 degrees
        //Simulate view range - camera dependant... calibrated for POCO F2...
        float GhostDegreeAroundPlayerToDisplaySim = GhostDegreeAroundPlayer * 1.5f;
        float fRes = deg + GhostDegreeAroundPlayerToDisplaySim;// + ;
        if (fRes > 360) {fRes -= 360;}
        //now we have a range of 0 to 360... adjust to -180 to 180 degrees
        fRes -= 180;
        return fRes;
    }

    private float getGhostDegreeYInDisplayDegrees(float deg){
        //show ghost if ghost in range of view - in Y is always in range - just to make an easier and enjojable game
        float GhostDegreeAbovePlayerToDisplaySim = GhostDegreeAbovePlayer;
        float yRes = deg + (GhostDegreeAbovePlayerToDisplaySim);// + rotxGhosts[1];

        final int miny = 0; final int maxy = 10;
        final int randomy = rGenerator.nextInt((maxy - miny) + 1) + miny;

        //add randomy to avoid same random movement in for ghosts on Y due to Orientation Sensor
        //error - vibration -> instead of filtering, use it as ghost effect
        return yRes + (randomy/10);
    }

    private void RecalculateGhostDisplayCoordsFromPhoneOrientation() {
        //calc if each ghost is in view and it's position in the display
        float ghostworlddegreeX = getGhostDegreeXInDisplayDegrees(rotxGhosts[1]);
        float ghostworlddegreey = getGhostDegreeYInDisplayDegrees(rotyGhosts[1]);
        int maxdegapp=30;
        int centertodisp=40;
        ghostdegreeX[1] = (int) ghostworlddegreeX;
        if (((ghostworlddegreeX > maxdegapp) || (ghostworlddegreeX < -maxdegapp)) || (ghosts<1)) {
            //out of view
            xghost1 = SCREEN_width;
            yghost1 = SCREEN_height;
            ghostsDisplayed[1] = false;
        } else {
            //in view
            ghostsDisplayed[1] = true;
            xghost1 = (int) ((SCREEN_width / 100) * (centertodisp - ghostworlddegreeX));
            yghost1 = (int) ((SCREEN_height / 100) * (centertodisp - ghostworlddegreey));
        }

        ghostworlddegreeX = getGhostDegreeXInDisplayDegrees(rotxGhosts[2]);
        ghostworlddegreey = getGhostDegreeYInDisplayDegrees(rotyGhosts[2]);
        ghostdegreeX[2] = (int) ghostworlddegreeX;
        if (((ghostworlddegreeX > maxdegapp) || (ghostworlddegreeX < -maxdegapp)) || (ghosts<2)){
            //out of view
            ghostsDisplayed[2] = false;
            xghost2 = SCREEN_width;
            yghost2 = SCREEN_height;
        } else {
            //in view
            ghostsDisplayed[2] = true;
            xghost2 = (int) ((SCREEN_width / 100) * (centertodisp - ghostworlddegreeX));
            yghost2 = (int) ((SCREEN_height / 100) * (centertodisp - ghostworlddegreey));
        }

        ghostworlddegreeX = getGhostDegreeXInDisplayDegrees(rotxGhosts[3]);
        ghostworlddegreey = getGhostDegreeYInDisplayDegrees(rotyGhosts[3]);
        ghostdegreeX[3] = (int) ghostworlddegreeX;
        if (((ghostworlddegreeX > maxdegapp) || (ghostworlddegreeX < -maxdegapp)) || (ghosts<3)){
            //out of view
            ghostsDisplayed[3] = false;
            xghost3 = SCREEN_width;
            yghost3 = SCREEN_height;
        } else {
            //in view
            ghostsDisplayed[3] = true;
            xghost3 = (int) ((SCREEN_width / 100) * (centertodisp - ghostworlddegreeX));
            yghost3 = (int) ((SCREEN_height / 100) * (centertodisp - ghostworlddegreey));
        }

        ghostworlddegreeX = getGhostDegreeXInDisplayDegrees(rotxGhosts[4]);
        ghostworlddegreey = getGhostDegreeYInDisplayDegrees(rotyGhosts[4]);
        ghostdegreeX[4] = (int) ghostworlddegreeX;
        if ((ghostworlddegreeX > maxdegapp) || (ghostworlddegreeX < -maxdegapp) || (ghosts<4)) {
            //out of view
            ghostsDisplayed[4] = false;
            xghost4 = SCREEN_width;
            yghost4 = SCREEN_height;
        } else {
            //in view
            ghostsDisplayed[4] = true;
            xghost4 = (int) ((SCREEN_width / 100) * (centertodisp - ghostworlddegreeX));
            yghost4 = (int) ((SCREEN_height / 100) * (centertodisp - ghostworlddegreey));
        }

        //Debug to Status textView
        TextView textViewStat = findViewById(R.id.textViewStat);
        textViewStat.setText("1{" + GhostDegreeAroundPlayer + ";" + GhostDegreeAbovePlayer + "}");

        //set positions and size of each Ghost GifImageView
        Ghost1.setX(xghost1);
        Ghost1.setY(yghost1);
        Ghost1.getLayoutParams().width = sizeGhosts[1];
        Ghost1.getLayoutParams().height = sizeGhosts[1];
        Ghost2.setX(xghost2);
        Ghost2.setY(yghost2);
        Ghost2.getLayoutParams().width = sizeGhosts[2];
        Ghost2.getLayoutParams().height = sizeGhosts[2];
        Ghost3.setX(xghost3);
        Ghost3.setY(yghost3);
        Ghost3.getLayoutParams().width = sizeGhosts[3];
        Ghost3.getLayoutParams().height = sizeGhosts[3];
        Ghost4.setX(xghost4);
        Ghost4.setY(yghost4);
        Ghost4.getLayoutParams().width = sizeGhosts[4];
        Ghost4.getLayoutParams().height = sizeGhosts[4];

        //Gif image does not allow this transform... :(
        //Ghost1.setTranslationZ(0.5f);
        //Ghost2.setTranslationZ(0.5f);
        //Ghost3.setTranslationZ(0.5f);
        //Ghost4.setTranslationZ(0.5f);

        showghosts(ghosts);

        if(ghosts==0 || life==0){
            imageViewArrowLeft.setVisibility(View.INVISIBLE);
            imageViewArrowRight.setVisibility(View.INVISIBLE);
            imageViewLogo.setVisibility(View.VISIBLE);
        }
        else if (algunfantasmaalavista()) {
            imageViewArrowLeft.setVisibility(View.INVISIBLE);
            imageViewArrowRight.setVisibility(View.INVISIBLE);
            imageViewLogo.setVisibility(View.INVISIBLE);
        }
        else{
            imageViewLogo.setVisibility(View.INVISIBLE);
            int minghostdeg = 0;
            minghostdeg = ghostdegreeX[1];
            if(ghosts>1){
                if (Math.abs(minghostdeg) > Math.abs(ghostdegreeX[2])){
                    minghostdeg = ghostdegreeX[2];                }            }
            if(ghosts>2){
                if (Math.abs(minghostdeg) > Math.abs(ghostdegreeX[3])){
                    minghostdeg = ghostdegreeX[3];                }            }
            if(ghosts>3){
                if (Math.abs(minghostdeg) > Math.abs(ghostdegreeX[4])){
                    minghostdeg = ghostdegreeX[4];                }            }

            if(minghostdeg<0){
                imageViewArrowLeft.setVisibility(View.INVISIBLE);
                imageViewArrowRight.setVisibility(View.VISIBLE);
            }else{
                imageViewArrowLeft.setVisibility(View.VISIBLE);
                imageViewArrowRight.setVisibility(View.INVISIBLE);
            }
        }

        String gd = "";
        if(ghostsDisplayed[1]){gd += "[1];";}
        else{
            if(ghosts>0){gd += "[!];";}
            else{gd += "[_];";}}
        if(ghostsDisplayed[2]){gd += "[2];";}
        else{
            if(ghosts>1){gd += "[!];";}
            else{gd += "[_];";}}
        if(ghostsDisplayed[3]){gd += "[3];";}
        else{
            if(ghosts>2){gd += "[!];";}
            else{gd += "[_];";}}
        if(ghostsDisplayed[4]){gd += "[4];";}
        else{
            if(ghosts>3){gd += "[!];";}
            else{gd += "[_];";}}


        String GC = "{"+ghostdegreeX[1]+"};";
        GC += "{"+ghostdegreeX[2]+"};";
        GC += "{"+ghostdegreeX[3]+"};";
        GC += "{"+ghostdegreeX[4]+"};";
        GC ="";//remove for debug
        textViewInfo.setText(gd + " " + GC);

    }

    private void showghosts(int numghostsactuales) {
        Ghost1.setVisibility(View.INVISIBLE);
        Ghost2.setVisibility(View.INVISIBLE);
        Ghost3.setVisibility(View.INVISIBLE);
        Ghost4.setVisibility(View.INVISIBLE);
        //positions are updated with sensors update

        //RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100,200); // The desired size of the child
        //params.setMargins(50,50); // Position at 50,50
        //mRelativeLayout.addView( mViewToAdd, params);
        if (numghostsactuales > 0) {
            Ghost1.setVisibility(View.VISIBLE);
        }
        if (numghostsactuales > 1) {
            Ghost2.setVisibility(View.VISIBLE);
        }
        if (numghostsactuales > 2) {
            Ghost3.setVisibility(View.VISIBLE);
        }
        if (numghostsactuales > 3) {
            Ghost4.setVisibility(View.VISIBLE);
        }
        if (numghostsactuales > 3) {
            tvgssensed.setTextColor(Color.RED); //texto en rojo
        } else if (numghostsactuales == 0) {
            tvgssensed.setTextColor(Color.GREEN);
        } //texto en VERDE
        else {
            tvgssensed.setTextColor(Color.YELLOW);
        } //texto en amarillo
        //or Color.parseColor("#FFFFFF") //or Color.rgb(200,0,0) //or Color.Argb(0, 200,0,0) //or myTextView.setTextColor(0xAARRGGBB)
    }

    private void displayToast(int x, int y, String text, int delayType) {
        //private static final int LONG_DELAY = 3500; // 3.5 seconds
        //private static final int SHORT_DELAY = 2000; // 2 seconds
        if(delayType==1){
            Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
        }
    }


    private boolean algunfantasmaalavista() {
        if (ghosts < 1) {
            return false;
        }
        if (ghosts > 3 && ghostsDisplayed[4]) {
            return true;
        }
        if (ghosts > 2 && ghostsDisplayed[3]) {
            return true;
        }
        if (ghosts > 1 && ghostsDisplayed[2]) {
            return true;
        }
        if (ghosts > 0 && ghostsDisplayed[1]) {
            return true;
        }
        return false;
    }

    private boolean checkCameraTouch(View v, MotionEvent event) {
        if (GameOn) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (!GameOn){
                    StartGame();
                }
                else if (algunfantasmaalavista() && lastkilltimeok && (life > 0)) {
                    //SOLO PERMITIMOS HACER EL KILL SI LO ESTAMOS VIENDO
                    //NO HACE POCO QUE HEMOS MATADO UNO Y TENEMOS AÚN VIDA
                    int x = (int) event.getX();
                    int y = (int) event.getY();
                    if (false) { //debug
                        String text = "X axis is " + x + "and Y axis is " + y;
                        displayToast(x, y, text, 3000);
                    }
                    scoreval += 1;
                    //por como está hecha la lógica de control, matamos siempre al último creado
                    //basta con intercambiar coords del último por el que está visible.
                    //if (ghosts > 3 && ghostsDisplayed[4]) {//ok, kill this one
                    //} else if (ghosts > 2 && ghostsDisplayed[3]) { //
                    //intercambiaghostpos(3, ghosts);
                    //} else if (ghosts > 1 && ghostsDisplayed[2]) {//
                    // intercambiaghostpos(2, ghosts);
                    // } else if (ghosts > 0 && ghostsDisplayed[1]) {//
                    //    intercambiaghostpos(1, ghosts);
                    //}
                    //IMPROVEMENT: Matar el que está mas centrado en la vista del player [move to pos 4]
                    int minv = 999;
                    int actupdated = 4; //dejar como está
                    if(ghostsDisplayed[1]) {minv = Math.min(minv, ghostdegreeX[1]);
                        if(minv == ghostdegreeX[1]){actupdated=1;}}
                    if(ghostsDisplayed[2]) {minv = Math.min(minv, ghostdegreeX[2]);
                        if(minv == ghostdegreeX[2]){actupdated=2;}}
                    if(ghostsDisplayed[3]) {minv = Math.min(minv, ghostdegreeX[3]);
                        if(minv == ghostdegreeX[3]){actupdated=3;}}
                    if(ghostsDisplayed[4]) {minv = Math.min(minv, ghostdegreeX[4]);
                        if(minv == ghostdegreeX[4]){actupdated=4;}}
                    intercambiaghostpos(actupdated, ghosts);
                    //ahora podemos matar al último simplemente con:
                    ghosts -= 1;
                    RecalculateGhostDisplayCoordsFromPhoneOrientation();
                    MyNoPlayer.start();
                    //OUT.. MUY LENTO
                    //displayToast(x, y, "GHOST KILL!!", 10);
                    //update display of hit position & size
                    hitimageview.getLayoutParams().width = sizeGhosts[ghosts];
                    hitimageview.getLayoutParams().height = sizeGhosts[ghosts];

                    if (ghosts == 1) {
                        hitimageview.setX(Ghost1.getX());
                        hitimageview.setY(Ghost1.getY());                    }
                    if (ghosts == 2) {
                        hitimageview.setX(Ghost2.getX());
                        hitimageview.setY(Ghost2.getY());                    }
                    if (ghosts == 3) {
                        hitimageview.setX(Ghost3.getX());
                        hitimageview.setY(Ghost3.getY());                    }
                    if (ghosts == 4) {
                        hitimageview.setX(Ghost4.getX());
                        hitimageview.setY(Ghost4.getY());                    }

                    //disableo el setvisible del hitimageview.. no funciona bien y no hace falta.
                    //ya se ve cuando el fantasma desaparece.
                    //hitimageview.setVisibility(View.VISIBLE);

                    //disable hit view in 1 second if life>0
                    if(life>0) {
                        lastkilltimeok = false;
                        timerHandler2.postDelayed(timerRunnable2, 50);
                    }else{
                        if(bUseStartb) { startbutton.setVisibility(View.VISIBLE);}
                    }
                }
            }
        }

        return true;
    }

    private void intercambiaghostpos(int p1, int p2){
        int auxx = rotxGhosts[p1];
        int auxy = rotyGhosts[p1];
        int auxs = sizeGhosts[p1];
        rotxGhosts[p1]=rotxGhosts[p2];
        rotyGhosts[p1]=rotyGhosts[p2];
        sizeGhosts[p1]=sizeGhosts[p2];
        rotxGhosts[p2] = auxx;
        rotyGhosts[p2] = auxy;
        sizeGhosts[p2] = auxs;
    }
    public void sendMessageNoFilter(View view) {
        Intent intent = new Intent(this, DisplayAbsOrientation.class);
        // QdisplayAbsOrientation for using quaternions
        String message = "nofilter";
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void sendMessageFilter(View view) {
        Intent intent = new Intent(this, DisplayAbsOrientation.class);
        // QdisplayAbsOrientation for using quaternions
        String message = "filter";
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }


    public void sendMessageRelNoG(View view) {
        Intent intent = new Intent(this, QDisplayRelativeOrientation.class);
        String message = "nogravity";
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void sendMessageLaunchGame(View view) {
        Intent intent = new Intent(this, CarlosGame.class);
        String message = "filterauto"; //allow auto update with histeresis
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void initAppSensors(){
        //get sensors
        if(sensorManager==null) {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        }
        if (sensorManager != null) {
            accelerometerSensor = sensorManager.getDefaultSensor(TYPE_ACCELEROMETER);
        }
        if(accelerometerSensor!=null){
            sensorManager.registerListener((SensorEventListener) this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }else{
            displayToast(10, 50, "Accelerometer sensor not available!", 3000);
        }

        if (sensorManager != null) {
            gyroscopeSensor = sensorManager.getDefaultSensor(TYPE_GYROSCOPE);
        }
        if(gyroscopeSensor!=null){
            sensorManager.registerListener((SensorEventListener) this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
        }else{
            displayToast(10, 50, "Gyroscope sensor not available!", 3000);
        }

        if (sensorManager != null) {
            magneticfieldSensor = sensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD);
        }
        if(magneticfieldSensor!=null){
            sensorManager.registerListener((SensorEventListener) this, magneticfieldSensor, SensorManager.SENSOR_DELAY_GAME);
        }else{
            displayToast(10, 50, "Magneticfield sensor not available!", 3000);
        }

        /*if(stepCounterSensor==null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }
        if(stepCounterSensor!=null){
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_GAME);
        }else{
            displayToast(10, 50, "StepCount sensor not available!", 3000);
        }*/

        if(stepDetectorSensor==null) {
            stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        }
        if(stepDetectorSensor!=null){
            sensorManager.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_GAME);
        }else{
            displayToast(10, 50, "StepDetect sensor not available!", 3000);
        }


        //SENSOR_DELAY_FASTEST: 0us, SENSOR_DELAY_UI: 1us, SENSOR_DELAY_GAME: 2us, SENSOR_DELAY_NORMAL: 3us

        //Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        //if (stepCounterSensor != null) {
        //    sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        //} else {
        //    Toast.makeText(this, "Count sensor not available!", Toast.LENGTH_LONG).show();
        //}
    }

    public void checkSensors(View view){
        //check available sensors and types

        //get sensors
        if(sensorManager==null) {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        }

        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        String a = "Sensors: " + sensorList.size();
        displayToast(5,5, a, 3000);
        int i=0;
        String x="";
        for (Sensor sensor : sensorList) {
            //check this sensor...
            i+=20;
            x += sensor.getName()+";";
        }
        displayToast(5, 5, x, 3000);
    }
    //camera functions

    public void asfasfasf(View view) {
        Intent intent = new Intent(this, QDisplayRelativeOrientation.class);
        String message = "nogravity";
        intent.putExtra(EXTRA_MESSAGE, message);
        startActivity(intent);
    }

    public void StartCameraAndResetGameFull(View view) {
        StartCameraAndResetGame(view, 0);
    }
    public void StartCameraAndResetGameDrift(View view) {
        StartCameraAndResetGame(view, 1);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void StartCameraAndResetGame(View view, int DriftOnly) {
        //show camera and start the game
        SurfaceTexture mySurfaceTexture = myTextureView.getSurfaceTexture();
        Surface mySurface = new Surface(mySurfaceTexture);

        btStartGameFull.setVisibility(View.INVISIBLE);
        btStartGameDrift.setVisibility(View.INVISIBLE);

        if(DriftOnly == 1){
            bDriftGame = true;
        }else{
            bDriftGame = false;
        }

        try {
            myCaptureRequestBuilder = myCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            myCaptureRequestBuilder.addTarget(mySurface);
            myCameraDevice.createCaptureSession(Arrays.asList(mySurface), new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            myCameraCaptureSession = session;
                            myCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                            try {
                                myCameraCaptureSession.setRepeatingRequest(myCaptureRequestBuilder.build(), null, null);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        //INIT LOCATION SERVICE
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //
        StartGame();

    }


    //camera functions start
    private CameraDevice.StateCallback myStateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            myCameraDevice = camera;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            myCameraDevice.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            myCameraDevice.close();
            myCameraDevice = null;

        }
    };

    private void openCamera() {
        try {
            String myCameraID = myCameraManager.getCameraIdList()[0];
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            myCameraManager.openCamera(myCameraID, myStateCallBack, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Game control
    private double scoreval;
    private int ghosts = 0;
    private int life = 0;
    private int steps = 0;
    private boolean GameOn = false;

    private boolean bUseStartb = false;//use screen touch or button
    Button startbutton = null;

    private void StartGame() {
        scoreval = 0;
        ghosts = 0;
        life = 100;
        steps = 0;
        numstepsActuales = 0;
        UpdateScore();
        UpdateGhosts();
        UpdateLife();
        UpdateSteps(0);
        if(startbutton == null){startbutton = (Button) findViewById(R.id.button);}
        startbutton.setVisibility(View.INVISIBLE);// . setText("Restart");
        Button B2 = (Button) findViewById(R.id.button2);
        B2.setVisibility(View.INVISIBLE);
        Button B3 = (Button) findViewById(R.id.button3);
        B3.setVisibility(View.INVISIBLE);

        myImageHit.setVisibility(View.INVISIBLE);
        tvGameOver.setVisibility(View.INVISIBLE);
        tvGameOver.setText("GAME OVER");
        GameOn = true;
        //launch timer handler
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);
    }

    private void UpdateScore() {
        TextView tv1 = (TextView) findViewById(R.id.textViewScore);
        tv1.setText("Score: " + scoreval);
    }

    private void UpdateGhosts() {
        tvgssensed.setText(ghosts + " Ghosts sensed");
    }

    private void UpdateLife() {
        TextView tv1 = (TextView) findViewById(R.id.textViewLife);
        tv1.setText("Life: " + life);
    }

    int startsteps=0;
    private void UpdateSteps(int stepcount) {
        if(stepcount==0){startsteps=0;}
        else{ if(startsteps == 0){startsteps = stepcount;} }
        steps = stepcount;// - startsteps;
        TextView tv1 = (TextView) findViewById(R.id.textViewSteps);
        tv1.setText("Pasos: " + steps);
    }

    private void UpdateTime(int minutes, int seconds) {
        TextView tv1 = (TextView) findViewById(R.id.textViewTime);
        String minS = "" + minutes;
        if (minutes < 10) {
            minS = "0" + minS;
        }
        ;
        String secS = "" + seconds;
        if (seconds < 10) {
            secS = "0" + secS;
        }
        ;
        tv1.setText("Time: " + minS + ":" + secS);
    }

    private void UpdateGPS() {
        TextView tv1 = (TextView) findViewById(R.id.textViewGPS);
        Location myloc = getLastBestLocation();
        if(myloc != null) {
            double Lon = myloc.getLongitude();
            double Lat = myloc.getLatitude();
            tv1.setText("GPS N." + Lat + ";E." + Lon);
        }else{
            tv1.setText("GPS N. ??" +";E. ??");
        }
    }
    /**
     * @return the last know best location
     */
    static final int TWO_MINUTES = 1000 * 60 * 2;
    private Location getLastBestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;//DONE;
        }
        locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (null != locationGPS) { GPSLocationTime = locationGPS.getTime(); }
        if (null != locationNet) { NetLocationTime = locationNet.getTime();}
        if ( 0 < GPSLocationTime - NetLocationTime ) { return locationGPS;}
        else {return locationNet;}

        // Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent ==null) {return;}
        //if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) { count.setText(String.valueOf(sensorEvent.values[0]));
        //    UpdateSteps((int) sensorEvent.values[0]);}
        if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            numstepsActuales+=1;
            UpdateSteps(numstepsActuales);
        }
        // USE OF GYROSCOPE to control position
        if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float[] gyr = new float[3]; float[] v = new float[3]; double theta = 0d;
            copyarrayto(sensorEvent.values, gyr);


            double gyrRotationVelocity = rotatingvel(gyr, v); // obtain rotation velocity and axis
            //https://developer.android.com/reference/android/hardware/SensorEvent#values
            //SensorEvent.values[1]; ->
            // evaluate time evolution
            if (timestamp != 0) {
                dTns = sensorEvent.timestamp - timestamp;dT = dTns / NS2S;
                theta = gyrRotationVelocity * dT;
            }
            timestamp = sensorEvent.timestamp;
            // introduce rotation of device
            Xini.update(Xini.rotate((float) -theta, v));
            Yini.update(Yini.rotate((float) -theta, v));
            Zini.update(Zini.rotate((float) -theta, v));


            if(bFilterEnabledX){

            }
            else{
                double vDriftMeanNoise = 0.001;
                //add simulated drift to left or right
                //Xini.x += vNoise;
                //Xini.y += vNoise;
                Xini.z += vDriftMeanNoise;
            }


            //We Just need to use the YAxis info to rotate ghosts
            //show value for debug and validation
            //TextView tAxisY = (TextView) findViewById(R.id.textViewYAxisDeg);
            //Gyro info and Ghost 0,0 WORLD position calculation against DEV coords
            //Projection against device coordinates only! not world X,Y,Z (not needed for this game)
            //We could just add distance to ghost to reference world coords. right now is a constant:
            distanceToGhost0Ref = 10.f;//10 meters for example.

            TextView textViewGyroX = (TextView) findViewById(R.id.textViewGyroX);
            TextView textViewGyroY = (TextView) findViewById(R.id.textViewGyroY);
            TextView textViewGyroZ = (TextView) findViewById(R.id.textViewGyroZ);
            TextView textViewGhostX = (TextView) findViewById(R.id.textViewGyroXD);

            //Game is meant played with movile phone in vertical position
            // we will use Xini coords to recalculate ghosts position around player
            // and  above/under player
            String x = format("%.2f", (Xini.x/1f));
            String y = format("%.2f", (Xini.y/1f));
            String z = format("%.2f", (Xini.z/1f));
            textViewGyroX.setText("º: " + x);//
            textViewGyroY.setText("º: " + y);//
            textViewGyroZ.setText("º: " + z);//

            //Just use Xini.x and Xini.z to calculate ghost position around player
            if(Xini.x<0){ //degrees from 90 to 270
                GhostDegreeAroundPlayer = 180.f + ((Xini.z) * 90.0f);
            }else{ //degrees from -90 to 90
                GhostDegreeAroundPlayer = 0.f + ((-Xini.z) * 90.0f);
            }
            GhostDegreeAroundPlayer+=90.f;

            //ignore decimals, just angle from 0 to 360º
            int degreesGhostX = (int) GhostDegreeAroundPlayer;
            textViewGhostX.setText("" + degreesGhostX + "º" );
            textViewGhostX.setTextColor(Color.MAGENTA);
        }


        //with magnetic and accelerometer sensors
        // USE OF CALCULATED ORIENTATION  to control position
        //simplificando un poco el código
        float linear_acceleration[] = new float[3];
        switch(sensorEvent.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                //mGravity = sensorEvent.values.clone();
                //accelerometervalues = sensorEvent.values.clone();
                //AX.setText(accelerometervalues[0] + "");//AY.setText(accelerometervalues[1] + "");//AZ.setText(accelerometervalues[2] + "");
                float alpha = 0.8f;

                if(bFilterEnabledY){
                    alpha = 0.8f;
                    mFilerNoise[0] = 0.0f;
                    mFilerNoise[1] = 0.0f;
                    mFilerNoise[2] = 0.0f;
                }
                else{
                    alpha = 0.0f;
                    //add simulated drift to left or right
                    mFilerNoise[0] = 0.1f;
                    mFilerNoise[1] = 0.1f;
                    mFilerNoise[2] = 0.1f;
                }

                // filtrar para hacer el ajuste Y mas suave (low-pass filter t / (t + dT))
                mGravity[0] = alpha * mGravity[0] + (1 - alpha) * sensorEvent.values[0];
                mGravity[1] = alpha * mGravity[1] + (1 - alpha) * sensorEvent.values[1];
                mGravity[2] = alpha * mGravity[2] + (1 - alpha) * sensorEvent.values[2];
                // Recalcular acc eliminando la gravedad (hfilter)
                linear_acceleration[0] = mFilerNoise[0] + sensorEvent.values[0] - mGravity[0];
                linear_acceleration[1] = mFilerNoise[1] + sensorEvent.values[1] - mGravity[1];
                linear_acceleration[2] = mFilerNoise[2] + sensorEvent.values[2] - mGravity[2];
                textViewAccelX.setText(format("%.2f", linear_acceleration[0]));
                textViewAccelY.setText(format("%.2f", linear_acceleration[1]));
                textViewAccelZ.setText(format("%.2f", linear_acceleration[2]));
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mGeomagnetic = sensorEvent.values.clone();
                break;
        }
        //RECALCULAR posiciones de referencia del movil
        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                azimut = orientation[0]; // orientation contains: azimut, pitch and roll
                pitch = orientation[1]; // orientation contains: azimut, pitch and roll
                roll = orientation[2]; // orientation contains: azimut, pitch and roll
            }
            //Now Just use the PITCH to calculate ghost position ABOVE Player ?
            GhostDegreeAbovePlayer = pitch;
            String a = format("%.2f", (azimut/1f));
            String p = format("%.2f", (pitch/1f));
            String r = format("%.2f", (roll/1f));
            //NO, JUST USE GRAVITY OVER Z AXIS - PITCH SOLO VA BIEN CON MOVIL EN PLANO
            //[filtro paso bajo aplicado]
            GhostDegreeAbovePlayer = mGravity[2];
            a = format("%.2f", (mGravity[0]/1f));
            p = format("%.2f", (mGravity[1]/1f));
            r = format("%.2f", (mGravity[2]/1f));
            textViewAzimut.setText("º: " + a);//
            textViewPitch.setText("º: " + p);//
            textViewRoll.setText("º: " + r);//
            //9.8 = el cambio de 0 a 9.8 son 90 grados aprox (ignoramos altitud, etc...)
            //Aunque podriamos ajustar con la geolocalizacion,
            //para el juego es completamente innecesario
            GhostDegreeAbovePlayer = GhostDegreeAbovePlayer * 90.0f / 9.8f;
            int degreesGhostY = (int) GhostDegreeAbovePlayer;
            textViewGhostY.setText("" + -degreesGhostY + "º" );
            textViewGhostY.setTextColor(Color.MAGENTA);
            //
        }

        //ajustar posiciones de display de los fanstasmas si se ven ahora
        RecalculateGhostDisplayCoordsFromPhoneOrientation();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    private void copyarrayto(float x[], float y[]) {
        int i;
        for (i = 0; i <= 2; i++) {
            y[i] = (float) x[i];
        }
    }

    private float rotatingvel(float[] w, float[] vv) {
        int i;
        float rotationVelocity = (float) Math.sqrt(w[0] * w[0] + w[1] * w[1] + w[2] * w[2]);

        if (rotationVelocity != 0.0d)
            for (i = 0; i <= 2; i++)
                vv[i] = w[i] / (float) rotationVelocity;
        else
            for (i = 0; i <= 2; i++)
                vv[i] = 0f;

        return rotationVelocity;
    }



    //android handling resume - pause
    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the needed sensors
        //registersensors();
        paused=false;
        MyAmbientPlayer.start();
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        paused=true;
        //unregistersensors();
        MyAmbientPlayer.pause();
    }

    private String symbolof(float v){
        if(v<0){return "";}
        else{return "+";}
    }
}



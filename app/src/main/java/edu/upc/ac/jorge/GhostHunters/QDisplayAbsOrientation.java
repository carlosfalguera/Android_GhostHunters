package edu.upc.ac.jorge.GhostHunters;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class QDisplayAbsOrientation extends AppCompatActivity implements SensorEventListener{
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private int sig_mag=1;

    private float[] mag=new float[3];   // readings magnetometer
    private float[] acc=new float[3];   // readings accelerometer

    private Quaternion B = new Quaternion(0f,0f,0f,0f);     //  MAGNETIC READING in DEV coordinates

    private  Quaternion E = new Quaternion(0f,1f,0f,0f);    // EAST (Y) in DEV coordinates
    private Quaternion N = new Quaternion(0f,0f,0f,-1f);     // NORTH (Y) in DEV coordinates
    private Quaternion Zaxis = new Quaternion(0f,0f,1f,0f); // Vertical axis in DEV coordinates

    private float Evector[]={1f,0f,0f};
    private float Nvector[]={0f,1f,0f};
    private float Zvector[]={0f,0f,1f};


    // external points
    private int time=0;                // time for movinig points
    private int time_vel=0;
    private float[] point = {0f,10f,0f};  // point in EARTH coordinates (E,N,Z)
    private float[] point_1 = {0f,10f,0f};  // point in EARTH coordinates (E,N,Z) at time t-1
    private float[] point_dev = {0f,-100f,0f};  // point in DEV coordinates (X,Y,Z)

    private float[] vel = {0.0f,0.0f,0.0f};  // vel in EARTH coordinates
    private float[] vel_1 = {0.0f,0.0f,0.0f};  // vel in EARTH coordinates
    private Quaternion P = new Quaternion(0f,0f,0f,0f);         // POINT in DEV coordinates
    private float[] point2 = {0f,10f,0.5f};  // point 2 in EARTH coordinates
    private Quaternion P2 = new Quaternion(0f,0f,0f,0f);         // POINT in DEV coordinates
    Random r = new Random();

    // For filtering purposes
    private String message;
    private float alpha=0.9f;             // coefficient of low-pass filter
    private float beta=0.9f;
    private boolean filterActivate = true;
    private String check = "Inicial";


    private float[] acc_1={0f,0f,0f};
    private float[] mag_1={0f,0f,0f};

    private DrawView drawView;  // screen CANVAS


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        //gyro=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        //setContentView(R.layout.activity_no_filter);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();

        message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        if (message.equals("nofilter")){
            filterActivate=false;
        }
        if (message.equals("filter")){
            filterActivate=true;
        }


        // Drawing
        drawView = new DrawView(this);
        setContentView(drawView);


        // Capture the layout's TextView and set the string as its text
        //TextView textView = findViewById(R.id.textView);
        //textView.setText(message);
    }


    //@Override
    /*public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
    */


    @Override
    public void onSensorChanged(SensorEvent event) {
        int i;
        float mod=0; // to compute mod of vectors

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            copyarrayto(event.values,acc);
            if(filterActivate) {
                lowpassfilter(acc, acc_1, alpha);
                copyarrayto(acc, acc_1);
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            copyarrayto(event.values,mag);
            if(filterActivate) {
                lowpassfilter(mag, mag_1, alpha);
                copyarrayto(mag, mag_1);
            }
        }

        // Check dot product acc and mag. At north emysphere should be negative.
        //If positive, change signs to magnetometer. This happens, for instance, in my samsung S5 Neo




        Zaxis.copyVector(acc);  // Z = |acc|
        Zaxis.normalize();

        B.copyVector(mag);      // B = |mag|
        B.normalize();

        E=B.multiply(Zaxis);        // E = B x Z
        E.clearReal();
        E.normalize();

        N=Zaxis.multiply(E);        // N = Z x E
        N.clearReal();
        N.normalize();



        time= time+1;
        time_vel=time_vel+1;
        // point 1 is moving in a circle

        for (i=0;i<=2; i++) {
            //point[i] = point_1[i]+1f*vel_1[i]*((float)r.nextInt(100)/(100f)-0.5f);
            point[i] = point_1[i] + vel_1[i];
            point_1[i] = point[i];
        }
        if(time_vel>0){
            for (i=0;i<=2; i++) {
                vel[i] = beta*vel_1[i] + (1.0f-beta)*0.30f * ((float) r.nextInt(500) / (500f) - 0.5f);
                vel_1[i]=vel[i];
            }
            time_vel=0;
        }



        Evector[0]=E.getI();Evector[1]=E.getJ();Evector[2]=E.getK();
        Nvector[0]=N.getI();Nvector[1]=N.getJ();Nvector[2]=N.getK();
        Zvector[0]=Zaxis.getI();Zvector[1]=Zaxis.getJ();Zvector[2]=Zaxis.getK();


        for (i=0;i<=2; i++) {
            point_dev[i] = point[0]*Evector[i]+point[1]*Nvector[i]+point[2]*Zvector[i] ;
        }


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;

                                            // Set size of screen etc for Drawing
        int RADIUS= (int) width/3;
        drawView = new DrawView(this);
        drawView.setRadius(RADIUS);
        drawView.setCenterXY(width/2, height/2);
        drawView.setVerticalAxisOval(RADIUS);
        setContentView(drawView);


        drawView.setVectorsXYZ(E.getI(), E.getJ(), E.getK(), N.getI(), N.getJ(), N.getK(), Zaxis.getI(), Zaxis.getJ(), Zaxis.getK());
        drawView.setPoint(point_dev[0], point_dev[1], point_dev[2]);

        setContentView(drawView);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }



    @Override
    protected void onResume(){
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors

        sensorManager.registerListener((SensorEventListener) this,accelerometer,
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener((SensorEventListener) this,magnetometer,
                SensorManager.SENSOR_DELAY_GAME);

    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener((SensorEventListener) this,accelerometer);
        sensorManager.unregisterListener((SensorEventListener) this,magnetometer);

    }


    private void copyarrayto(float x[], float y[]) {
        int i;
        for (i = 0; i <= 2; i++) {
            y[i] = (float) x[i];
        }
    }

    private void lowpassfilter(float x[], float x_1[], float alpha){
        int i;
        for (i=0; i<=2; i++) {
            x[i]=(1.0f-alpha)*x[i]+alpha*x_1[i];
        }
    }

}

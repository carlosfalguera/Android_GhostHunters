package edu.upc.ac.jorge.GhostHunters;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatActivity;

public class DisplayAbsOrientation extends AppCompatActivity implements SensorEventListener{
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private int sig_mag=1;

    private float[] mag=new float[3];   // readings magnetometer
    private float[] acc=new float[3];   // readings accelerometer
    private float[] acc_1={0f,0f,0f};       // previous, for Low Pass filter
    private float[] mag_1={0f,0f,0f};       // previous, for Low Pass filter


    private float[] B = {0f,0f,0f};     // Normalized to length 1, MAGNETIC READING in DEV coordinates
    private  float[] E = {0f,0f,0f};    // EAST (Y) in DEV coordinates
    private float[] N = {0f,0f,0f};     // NORTH (Y) in DEV coordinates
    private float[] Zaxis = {0f,0f,0f};     // Vertical axis in DEV coordinates

    // external points
    private float time=0.0f;                // time for movinig points
    private float[] point_earth = {0f,10f,0.5f};  // point 1 in EARTH coordinates
    private float[] point_dev = {0f,0f,0f};         // POINT in DEV coordinates
    private float[] point2_earth = {0f,10f,0.5f};  // point 2 in EARTH coordinates
    private float[] point2_dev = {0f,0f,0f};         // POINT in DEV coordinates

    // For filtering purposes
    private String message;
    private float alpha=0.975f;             // coefficient of low-pass filter
    private boolean filterActivate;
    private String check = "Inicial";

    private DrawView drawView;  // screen CANVAS


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer=sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer=sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();

        message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
        if (message.equals("nofilter")){
            filterActivate=false;
        }
        if (message.equals("filter")){
            filterActivate=true;
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

        // find axis in DEV coordinates
        copyarrayto(acc, Zaxis);
        //normalize(acc);
        normalize(Zaxis);

        copyarrayto(mag,B);
        normalize(B);

        crossProduct(B,Zaxis,E);
        normalize(E);

        crossProduct(Zaxis,E,N);
        normalize(N);


        time= time+1;                           // point 1 is moving in a circle

        point_earth[0]= 10*(float)Math.cos((float)2*3.1415*time/3000);
        point_earth[1]= 10*(float)Math.sin((float)2*3.1415*time/3000);

        // multiply times rotation matrix [Edev Ndev Zdev]
        multRotationMat(point_earth,E,N,Zaxis,point_dev);
        multRotationMat(point2_earth,E,N,Zaxis,point2_dev);


        // drawing
        drawView.setVectorsXYZ(E[0], E[1], E[2], N[0], N[1], N[2], Zaxis[0], Zaxis[1], Zaxis[2]);
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



    private void crossProduct(float x[], float y[], float z[]){
        z[0] = x[1]*y[2] - x[2]*y[1];   // cross product
        z[1] = x[2]*y[0] - x[0]*y[2];
        z[2] = x[0]*y[1] - x[1]*y[0];

    }

    private float normalize(float x[]){
        float mod = (float) Math.sqrt(x[0]*x[0]+x[1]*x[1]+x[2]*x[2]);
        if(mod!=0.0f) {
            int i;
            for (i = 0; i <= 2; i++) {
                x[i] = x[i] / mod;            // normalize
            }
        }
        return mod;
    }

    private void copyarrayto(float x[], float y[]) {
        int i;
        for (i = 0; i <= 2; i++) {
            y[i] = (float) x[i];
        }
    }

    private void arraytovectorstring(float x[], String y[]) {
        int i;
        for (i=0;i<=2; i++) {
            y[i] = String.format("%.2f", x[i]);
        }
    }
    private void lowpassfilter(float x[], float x_1[], float alpha){
        int i;
        for (i=0; i<=2; i++) {
            x[i]=(1.0f-alpha)*x[i]+alpha*x_1[i];
        }
    }

    private void multRotationMat(float x_earth[], float RX[], float RY[], float RZ[], float x_dev[]){
        int i;
        for (i=0;i<=2; i++) {
            x_dev[i] = x_earth[0]*RX[i]+x_earth[1]*RY[i]+x_earth[2]*RZ[i] ;
        }

    }

}

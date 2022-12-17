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

import static android.hardware.Sensor.TYPE_GYROSCOPE;

public class QDisplayRelativeOrientation extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor gyroscope;

    private String message;             // intent message if any

    private float[] gyr = new float[3];   // readings GYRO
    private double gyrRotationVelocity;
    private float[] v = new float[3];     //rotation axis
    private double theta = 0d;

    // dynamics of FLY
    private float[] rotating_vel = {0.0f, 0.0f, 0.0f};  // rotating vel of FLY at time t
    private float[] rotating_vel_1 = {0.0f, 0.0f, 0.0f};  // rotating vel of FLY it time t-1
    private float beta = 0.9f;            // low pass filter for FLY velocity changes
    private Random r = new Random();
    private double FlyRotationVelocity;
    private float[] Flyv = new float[3];  //rotation axis
    private double Flytheta = 0d;

    // quaternions for vectors in DEV coordinates
    private Quaternion Xini = new Quaternion(0f, 1f, 0f, 0f);  // X in DEV coordinates
    private Quaternion Yini = new Quaternion(0f, 0f, 0f, -1f);  // Y in DEV coordinates
    private Quaternion Zini = new Quaternion(0f, 0f, 1f, 0f);  // Zin DEV coordinates
    private Quaternion FlyDev = new Quaternion(0f, 2f, 3f, 0f);  // Fly in DEV coordinates

    private double timestamp = 0d;
    private double dTns = 1d;
    private double dT = 1d;
    private static final double NS2S = 1000000000.0d;  // nanosecs to secs

    private DrawView drawView;  // screen CANVAS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            gyroscope = sensorManager.getDefaultSensor(TYPE_GYROSCOPE);
        }

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();

        // not used here
        message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

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
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int i;

        // el movil
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            copyarrayto(event.values, gyr);

            gyrRotationVelocity = rotatingvel(gyr, v);         // obtain rotation velocity and axis
        }

        // la mosca
        for (i = 0; i <= 2; i++) {
            rotating_vel[i] = beta * rotating_vel_1[i] + (1.0f - beta) * 0.2f * ((float) r.nextInt(500) / (500f) - 0.4f);
            rotating_vel_1[i] = rotating_vel[i];
        }

        FlyRotationVelocity = rotatingvel(rotating_vel, Flyv); // obtain rotation velocity and axis

        // evaluate time evolution
        if (timestamp != 0) {
            dTns = event.timestamp - timestamp;
            dT = dTns / NS2S;
            theta = gyrRotationVelocity * dT;
            Flytheta = FlyRotationVelocity;
        }
        timestamp = event.timestamp;

        // move fly. It does not mater rotate
        // in dev or in earth coordinates
        // => rotate in dev coordinates as we have
        //  a quaternion
        FlyDev.update(FlyDev.rotate((float) Flytheta, Flyv));

        // introduce rotation of device
        FlyDev.update(FlyDev.rotate((float) -theta, v));
        Xini.update(Xini.rotate((float) -theta, v));
        Yini.update(Yini.rotate((float) -theta, v));
        Zini.update(Zini.rotate((float) -theta, v));

        // draw axis and fly
        drawView.setVectorsXYZ(Xini.getI(), Xini.getJ(), Xini.getK(), Yini.getI(), Yini.getJ(), Yini.getK(), Zini.getI(), Zini.getJ(), Zini.getK());
        drawView.setPoint(FlyDev.getI(), FlyDev.getJ(), FlyDev.getK());

        // if fly hits the center => capture in
        // unit sphere
        if (FlyDev.getI() * FlyDev.getI() + FlyDev.getJ() * FlyDev.getJ() < 0.1f) {
            FlyDev.normalize();
        }

        setContentView(drawView);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors

        sensorManager.registerListener((SensorEventListener) this, gyroscope,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener((SensorEventListener) this, gyroscope);
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

}

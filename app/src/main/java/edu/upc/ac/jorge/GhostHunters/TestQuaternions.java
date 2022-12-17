package edu.upc.ac.jorge.GhostHunters;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class TestQuaternions extends AppCompatActivity {

    private Quaternion q1=new Quaternion();
    private Quaternion q2=new Quaternion();
    private Quaternion q3=new Quaternion();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_quaternions);

        TextView text1 = (TextView) findViewById(R.id.textView1);
        TextView text2 = (TextView) findViewById(R.id.textView2);
        TextView text3 = (TextView) findViewById(R.id.textView3);


        q1.update(1,1,1,1);
        q2.update(1,0,0,0);
        q3=q1.multiply(q2);



        text1.setText(q1.toString());
        text2.setText(q2.toString());
        text3.setText(q3.toString());



    }
}

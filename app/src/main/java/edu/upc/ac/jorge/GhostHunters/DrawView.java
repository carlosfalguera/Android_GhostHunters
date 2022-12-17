package edu.upc.ac.jorge.GhostHunters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public  class DrawView extends View {

    private int radius; //500
    private float centerX; //1000 550
    private float centerY; //5000 1000
    private int verticalAxisOval; // = radius

    // we draw three vectors and one point
    private float vectorPaintX[]={1f,0f,0f};
    private float vectorPaintY[]= {0f,1f,0f};
    private float vectorPaintZ[]= {0f,0f,1f};
    private float vectorPoint[]={0f,0.5f,-0.5f};


    private Color colPaint;

    private Paint paint = new Paint();

    public DrawView(Context context) {
        super(context);
        paint.setColor(Color.WHITE);
    }

    public void setRadius(int r){
        radius=r;
    }

    public void setVerticalAxisOval(int r){
        verticalAxisOval=r;
    }

    public void setCenterXY(float cx, float cy){
        centerX=cx;
        centerY=cy;
    }

    public void setVectorsXYZ(float x0, float x1, float x2, float y0, float y1, float y2, float z0, float z1, float z2){
        // x and y are in SENSOR device coordenates, vectorPAints i SCREEN coodinates
        vectorPaintX[0] = x0;vectorPaintX[1] = x1;vectorPaintX[2] = x2;  // EAST to x device
        vectorPaintY[0] = y0;vectorPaintY[1] = y1;vectorPaintY[2] = y2; // NORTH to -z device
        vectorPaintZ[0] = z0;vectorPaintZ[1] = z1;vectorPaintZ[2] = z2;

    }

    public void setPoint(float x0, float x1, float x2){
        vectorPoint[0]=x0;
        vectorPoint[1]=x1;
        vectorPoint[2]=x2;
    }

    public void setColor(Color col){
        colPaint=col;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);


                                                        // Draw external frame
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(5);
        canvas.drawCircle(centerX,centerY,radius,paint);
        canvas.drawLine(centerX-radius, centerY, centerX+radius, centerY, paint);
        canvas.drawLine(centerX, centerY-radius, centerX, centerY+radius, paint);


        paint.setStrokeWidth(20);       // Draw normalized EAST vector
        paint.setColor(Color.GREEN);
        canvas.drawLine(centerX, centerY, vectorPaintX[0]*radius+centerX, (-vectorPaintX[1])*radius+centerY, paint);

        paint.setStrokeWidth(20);       // Draw normalized NORTH vector
        paint.setColor(Color.RED);
        canvas.drawLine(centerX, centerY, vectorPaintY[0]*radius+centerX, (-vectorPaintY[1])*radius+centerY, paint);

        paint.setStrokeWidth(20);       //  Draw normalized Zearth vector
        paint.setColor(Color.BLUE);
        canvas.drawLine(centerX, centerY, vectorPaintZ[0]*radius+centerX, (-vectorPaintZ[1])*radius+centerY, paint);


                                        // draw moving point

        if (vectorPoint[2]<0) {                 // point 1 (Moving in circles)
            paint.setColor(Color.BLACK);        // pasa frente a nosotros
        } else{
            paint.setColor(Color.RED);          // pasa por detrás
        }
        paint.setStrokeWidth(40);
        canvas.drawLine(vectorPoint[0] * radius +centerX , (-vectorPoint[1] ) * radius + centerY -20,  vectorPoint[0] * radius + centerX, (-vectorPoint[1]) * radius + centerY+20, paint);


    }


}


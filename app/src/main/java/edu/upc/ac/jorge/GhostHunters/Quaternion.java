package edu.upc.ac.jorge.GhostHunters;

public final class Quaternion {
    public float w; // real part

    public float x;  // i
    public float y;  // j
    public float z;  // k


    public Quaternion() {
        this.x = 0f;
        this.y = 0f;
        this.z = 0f;
        this.w = 0f;
    }

    public Quaternion(float w, float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Quaternion( float theta, float[] v) {
        float cosThetaOverTwo = (float) Math.cos(theta/2.0f);
        float sinThetaOverTwo = (float) Math.sin(theta/2.0f);

        if(v[0]!=0f | v[1]!=0f | v[2]!=0) {
            this.w = cosThetaOverTwo;

            this.x = sinThetaOverTwo * v[0];
            this.y = sinThetaOverTwo * v[1];
            this.z = sinThetaOverTwo * v[2];
        }
        else{
            this.w = 1.0f;

            this.x = 0f;
            this.y = 0f;
            this.z = 0f;
        }
    }


    public void update(Quaternion q){
        this.w=q.getReal();
        this.x=q.getI();
        this.y=q.getJ();
        this.z=q.getK();
    }

    public void update(float ww, float xx, float yy, float zz){
        this.w=ww;
        this.x=xx;
        this.y=yy;
        this.z=zz;
    }

    public String toString(){
        return Float.toString(w)+" , "+Float.toString(x)+" , "+Float.toString(y)+" , "+Float.toString(z);
    }

    public String toStringIm(){
        String str =Float.toString(x)+" , "+Float.toString(y)+" , "+Float.toString(z);
        return str;
    }

    public float norm() {
        return (float) Math.sqrt( this.w * this.w + this.x * this.x + this.y * this.y + this.z * this.z);
    }

    public float getReal() {
        return this.w;
    }

    public float getI() {
        return this.x;
    }

    public float getJ() {
        return this.y;
    }

    public float getK() {
        return this.z;
    }


    public Quaternion multiply(Quaternion q) {
        float nw = w * q.w - x * q.x - y * q.y - z * q.z;
        float nx = w * q.x + x * q.w + y * q.z - z * q.y;
        float ny = w * q.y + y * q.w + z * q.x - x * q.z;
        float nz= w * q.z + z * q.w + x * q.y - y * q.x;
        return new Quaternion(nw,nx,ny,nz);
    }

    public Quaternion add(Quaternion q) {
        float nw = w + q.w;
        float nx =  x + q.x ;
        float ny = y + q.y ;
        float nz=  z + q.z;
        return new Quaternion(nw,nx,ny,nz);
    }

    public Quaternion timesConstant(float q) {
        float nw = w*q;
        float nx =  x*q;
        float ny = y*q;
        float nz=  z*q;
        return new Quaternion(nw,nx,ny,nz);
    }

    public Quaternion conjugate(){
        return new Quaternion(this.getReal(),-this.getI(),-this.getJ(),-this.getK());
    }


    public Quaternion copyVector(float[] vect) {

        w = 0f;
        x = vect[0];
        y = vect[1];
        z = vect[2];

        return this;
    }

    public Quaternion getVector(float[] vect) {

        vect[0]=x;
        vect[1]=y;
        vect[2]=z;

        return this;
    }

    public Quaternion normalize() {
        float n = this.norm();
        if (n != 0) {
            w = w / n;
            x = x / n;
            y = y / n;
            z = z / n;
        }
        return this;
    }

    public void clearReal(){
        this.w=0f;
    }

    public void clearIm(){
        this.x=0f;
        this.y=0f;
        this.z=0f;
    }

    public boolean equals(Quaternion q) {
        return x == q.x && y == q.y && z == q.z && w == q.w;
    }

    public Quaternion rotate(float theta, float[] v ){
        Quaternion tmp = new Quaternion();
        Quaternion deltaQuat = new Quaternion( (float) theta, v);
        Quaternion deltaQuatConjugate= deltaQuat.conjugate();

        tmp.update(this.multiply(deltaQuatConjugate));
        tmp.update(deltaQuat.multiply(tmp));
        //tmp.normalize();
        return tmp;
    }


/*
    public float[] toMatrix() {
        float[] matrixs = new float[16];
        toMatrix(matrixs);
        return matrixs;
    }

    public final void toMatrix(float[] matrixs) {
        matrixs[3] = 0.0f;
        matrixs[7] = 0.0f;
        matrixs[11] = 0.0f;
        matrixs[12] = 0.0f;
        matrixs[13] = 0.0f;
        matrixs[14] = 0.0f;
        matrixs[15] = 1.0f;

        matrixs[0] = (float) (1.0f - (2.0f * ((y * y) + (z * z))));
        matrixs[1] = (float) (2.0f * ((x * y) - (z * w)));
        matrixs[2] = (float) (2.0f * ((x * z) + (y * w)));

        matrixs[4] = (float) (2.0f * ((x * y) + (z * w)));
        matrixs[5] = (float) (1.0f - (2.0f * ((x * x) + (z * z))));
        matrixs[6] = (float) (2.0f * ((y * z) - (x * w)));

        matrixs[8] = (float) (2.0f * ((x * z) - (y * w)));
        matrixs[9] = (float) (2.0f * ((y * z) + (x * w)));
        matrixs[10] = (float) (1.0f - (2.0f * ((x * x) + (y * y))));
    }
    */
}
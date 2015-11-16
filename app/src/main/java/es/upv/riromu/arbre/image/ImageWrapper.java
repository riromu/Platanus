package es.upv.riromu.arbre.image;

import android.graphics.Bitmap;

/**
 * Created by ricard on 20/09/15.
 */
public class ImageWrapper {

    final static int MIN_H=0;
    final static int MAX_H=1;
    final static int MIN_S=2;
    final static int MAX_S=3;
    final static int MIN_V=4;
    final static int MAX_V=5;

    private Bitmap bitmap,histogram;
    private String mask,hist;
    private int[] values={50,140,10,240,10,240};
    private Double r,g,b;

    public String getMask() {
        return mask;
    }

    public void setMask(String mask) {
        this.mask = mask;
    }

    public String getHist() {
        return hist;
    }

    public void setHist(String hist) {
        this.hist = hist;
    }

    public ImageWrapper() {
    }

    public int getMinH() {return values[MIN_H];}
    public int getMaxH() {
        return values[MAX_H];
    }
    public int getMinS() {return values[MIN_S];}
    public int getMaxS() {
        return values[MAX_S];
    }
    public int getMinV() {return values[MIN_V];}
    public int getMaxV() {
        return values[MAX_V];
    }

    public void setMinH(int minH) {
        this.values[MIN_H]= minH;
    }
    public void setMaxH(int maxH) {
        this.values[MAX_H] = maxH;
    }
    public void setMinS(int minS) {
        this.values[MIN_S]= minS;
    }
    public void setMaxS(int maxS) {
        this.values[MAX_S] = maxS;
    }
    public void setMinV(int minV) {
        this.values[MIN_V]= minV;
    }
    public void setMaxV(int maxV) {
        this.values[MAX_V] = maxV;
    }



    public Double getR() {
        return r;
    }

    public Double getG() {
        return g;
    }

    public void setG(Double g) {
        this.g = g;
    }

    public Double getB() {
        return b;
    }

    public void setB(Double b) {
        this.b = b;
    }

    public void setR(Double r) {
        this.r = r;
    }


    public Bitmap getHistogram() {
        return histogram;
    }

    public void setHistogram(Bitmap histogram) {
        this.histogram = histogram;
    }

    public Bitmap getBitmap() {

        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
}



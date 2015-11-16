    package es.upv.riromu.arbre.image;

/**
 * Created by ricard on 13/04/15.
 */


import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.opencv.android.Utils;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

    public class ImageTreatment {

    private static final String TAG = "ImageTreatment";
    private Mat image, image_aux, mask;
    private Mat hsv;
    private Mat imageresult;
        private Mat imagethres;
    private Mat imagefinal;
     private Scalar colors;


    public ImageTreatment(Bitmap bitmap) {
        try {
            image_aux = new Mat();
            Utils.bitmapToMat(bitmap, image_aux);
            image_aux=getResizedImage(image_aux,1024);
            image = new Mat(image_aux.size(), CvType.CV_8UC4);
            mask = new Mat(image.size(), CvType.CV_8UC1, new Scalar(0, 0, 0));
            imagefinal = new Mat(image_aux.size(), CvType.CV_8UC4);
            imagefinal.setTo(new Scalar(255,255,255));
            // this should be in BGR format according to the
            // documentation.
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory exception in convertMatToBitMap: " + e.getMessage());
        } catch (CvException e) {
            Log.e(TAG, "Convert to Bitmap exception : " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Convert to Bitmap exception : " + e.getMessage());
        }
        Imgproc.cvtColor(image_aux, image, Imgproc.COLOR_BGR2RGB);
        //enhanceGreen();
        hsv = new Mat(image_aux.size(), CvType.CV_8UC4);
        imageresult = new Mat(image_aux.size(), CvType.CV_8UC1,new Scalar(255));
        //imagethres = new Mat(image_aux.size(), CvType.CV_8UC3,new Scalar(255));
    }

    public ImageTreatment(Mat img) {
        image = BGRtoRGB(img);
    }

    public Bitmap getBitmap() {
        Bitmap bitmap;
        bitmap=null;
        try {
            bitmap = Bitmap.createBitmap(imagefinal.width(), imagefinal.height(), Bitmap.Config.ARGB_8888);
            bitmap.copy(Bitmap.Config.ARGB_8888, true);
            imagefinal=BGRtoRGB(imagefinal);
            Utils.matToBitmap(imagefinal, bitmap);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory exception in convertMatToBitMap: " + e.getMessage());
        } catch (CvException e) {
            Log.e(TAG, "Convert to Bitmap exception : " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Convert to Bitmap exception : " + e.getMessage());
        }
        return bitmap;
    }
    public int enhanceGreen() {
        try {

            double[] aux;
            double r, g, b;
            for (int i = 0; i < image.rows(); i++) {
                for (int j = 0; j < image.rows(); j++) {
                    aux = image.get(i, j);
                    b = aux[0];
                    g = aux[1];
                    r = aux[2];
                    aux[0] = b / g + b + r;
                    aux[1] = 2 * g / g + b + r;
                    aux[2] = r / g + b + r;
                    if (aux[1] > 255) aux[1] = 255;
                    image.put(i, j, aux);
                }
     }
        } catch (Exception e) {
            Log.e(TAG, "Error" + e.getMessage());
        }
        return 1;
    }

    public int getHSV() {
//we convert from RGB to HSV and extract the propper channels
        try {
            Imgproc.cvtColor(image, hsv, Imgproc.COLOR_RGB2HSV);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory exception in convertMatToBitMap: " + e.getMessage());
        } catch (CvException e) {
            Log.e(TAG, "Convert to planes exception : " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Convert to planes exception : " + e.getMessage());
        }
        return 1;
    }



    public int performImageSmoothing() {

        Mat result = new Mat();
        org.opencv.core.Size s = new Size(15, 15);
        Imgproc.GaussianBlur(image, result, s, 3);
        image = result;
        return 1;
    }

    /**************************************************************/
//2. GENERATEHSV
    /**************************************************************/
    public int generateHSV() {
//we convert from RGB to HSV and extract the propper channels
        try {
            Imgproc.cvtColor(image, hsv, Imgproc.COLOR_RGB2HSV);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory exception in convertMatToBitMap: " + e.getMessage());
        } catch (CvException e) {
            Log.e(TAG, "Convert to planes exception : " + e.getMessage());
        }catch (Exception e) {
            Log.e(TAG, "Convert to planes exception : " + e.getMessage());
        }
        return 1;
    }

    /**************************************************************/
//3. PERFORM DOUBLE THRESHOLDING
    /**************************************************************/
   public Bitmap calchsvHistogram()
   {


     //using 3 bins for h, 3 for s, 3 for value
       int hBins=180;
       boolean accumulate=false;
       MatOfInt mHistSize=new MatOfInt(hBins);
       MatOfFloat  mRanges=new MatOfFloat(0,180);
       MatOfInt mChannels=new MatOfInt(0);
       Mat hsvRef = new Mat();
       Mat hsvSource = new Mat();
       Mat hist =  new Mat(hsv.size(), CvType.CV_8UC1);
       Mat histSource = new Mat();
       Bitmap bitmap=null;
       Mat backproj = new Mat();
       ArrayList<Mat> histImages=new ArrayList<Mat>();
       Core.split(hsv, histImages);
       Imgproc.calcHist(histImages,
               mChannels,
               mask,
               hist,
               mHistSize,
               mRanges,
                accumulate);


       Scalar colour;
       // Create space for histogram image
       Mat histImage = Mat.zeros( 100, (int)mHistSize.get(0, 0)[0], CvType.CV_8UC3);
// Normalize histogram
       Core.normalize(hist, hist, 1, histImage.rows() , Core.NORM_MINMAX, -1, new Mat() );
// Draw lines for histogram points
       for( int i = 0; i < (int)mHistSize.get(0, 0)[0]; i++ )
       {
          colour=HSVtoRGB(new Scalar(i,128,128));
           Core.line(
                   histImage,
                   new org.opencv.core.Point( i, histImage.rows() ),
                   new org.opencv.core.Point( i, histImage.rows()-Math.round( hist.get(i,0)[0] )) ,colour,
                   1, 8, 0 );
       }
       //Imgproc.calcBackProject(histImages, mChannels, hist, backproj, mRanges, 1);
       try {
           bitmap = Bitmap.createBitmap(histImage.width(), histImage.height(), Bitmap.Config.ARGB_8888);
           bitmap .copy(Bitmap.Config.ARGB_8888, true);
           Utils.matToBitmap(histImage,bitmap);
       } catch (OutOfMemoryError e) {
           Log.e(TAG, "Out of memory exception in convertMatToBitMap: " + e.getMessage());
       } catch (CvException e) {
           Log.e(TAG, "Convert to Bitmap exception : " + e.getMessage());
       } catch (Exception e) {
           Log.e(TAG, "Convert to Bitmap exception : " + e.getMessage());
       }
       return bitmap;
   };


        /**************************************************************/
//3. PERFORM DOUBLE THRESHOLDING
        /**************************************************************/
        public String calchsvHistogramJSON()
        {


            //using 3 bins for h, 3 for s, 3 for value
            int hBins=180;
            boolean accumulate=false;
            MatOfInt mHistSize=new MatOfInt(hBins);
            MatOfFloat  mRanges=new MatOfFloat(0,180);
            MatOfInt mChannels=new MatOfInt(0);
            Mat hsvRef = new Mat();
            Mat hsvSource = new Mat();
            Mat hist =  new Mat(hsv.size(), CvType.CV_8UC1);
            Mat histSource = new Mat();
            Bitmap bitmap=null;
            Mat backproj = new Mat();
            ArrayList<Mat> histImages=new ArrayList<Mat>();
            Core.split(hsv, histImages);
            Imgproc.calcHist(histImages,
                    mChannels,
                    mask,
                    hist,
                    mHistSize,
                    mRanges,
                    accumulate);


            Scalar colour;
            // Create space for histogram image
            Mat histImage = Mat.zeros( 100, (int)mHistSize.get(0, 0)[0], CvType.CV_8UC3);
// Normalize histogram
            Core.normalize(hist, hist, 1, histImage.rows() , Core.NORM_MINMAX, -1, new Mat() );
// Draw lines for histogram points

            //Imgproc.calcBackProject(histImages, mChannels, hist, backproj, mRanges, 1);
          hist.convertTo(histSource,CvType.CV_8U);
        return matToJson(histSource);
        };


        public String getmaskJSON()
        {
         return matToJson(mask);

        }




    public int performDoubleThresholding(int minH,int maxH,int minS,int maxS,int minV, int maxV) {
        try {
            Core.inRange(hsv, new Scalar(minH, minS, minV), new Scalar(maxH, maxS, maxV), imageresult);
        } catch (CvException e) {
            Log.e(TAG, "Error en threshold" + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error en threshold" + e.getMessage());
        }

        try {

            //   Imgproc.cvtColor(imageresult, imageresult, Imgproc.COLOR_HSV2RGB);
        } catch (CvException e) {
            Log.e(TAG, "Error en threshold" + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error en threshold" + e.getMessage());
        }
        return 1;
    }

        public int performImageFusion() {
        try {
            Mat operator = new Mat(7, 7, CvType.CV_8U, new Scalar(1));
            Imgproc.morphologyEx(imageresult, imageresult, Imgproc.MORPH_CLOSE, operator);
        } catch (CvException e) {
            Log.e(TAG, "Error en threshold" + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Error en threshold" + e.getMessage());
        }
  return 1;
    }

    public int drawContours() {
        int largest_contour_index=0;
        int largest_area=0;
        Mat alpha=new Mat(imagefinal.size(),CvType.CV_8UC1,new Scalar(255));


        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        List<List<MatOfPoint2f>> contours2f = new ArrayList<List<MatOfPoint2f>>();
        MatOfPoint2f contours_point = new MatOfPoint2f();
        List<MatOfPoint> approxContour = new ArrayList<MatOfPoint>();
        MatOfPoint2f approxContour2f = new MatOfPoint2f();
        MatOfPoint2f mMOP2f1 = new MatOfPoint2f();
        MatOfPoint2f mMOP2f2 = new MatOfPoint2f();
        Mat hierarchy = new Mat();

        Imgproc.findContours(imageresult, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
       //imaesult.release();
        Scalar aux = new Scalar(255, 255, 255);
        for (int i = 0; i < contours.size(); i++) {
           contours.get(i).convertTo(mMOP2f1, CvType.CV_32FC2);
            contours_point = new MatOfPoint2f(contours.get(i).toArray());
            Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, 2, true);

            double a=Imgproc.contourArea(contours.get(i),false);
            if(a>largest_area){
                largest_area=(int)a;
                largest_contour_index=i;                //Store the index of largest contour
            }
            Imgproc.drawContours(mask, contours,i, aux, -1);

        }

      Imgproc.drawContours(mask, contours, largest_contour_index, aux, -1);
        image.copyTo(imagefinal, mask);
        Mat hist=new Mat();
        colors = Core.mean(imagefinal,mask);

        //cvAvgStd
        //iluminación//V alto blanco...V bajo negro...
        //S 0 V0 blanco total
        //media desviación y mediano...
        //histogramas de colr
   //     mask.release();
        return 1;
    }


    public Mat BGRtoRGB(Mat img) {
        Mat result = new Mat(img.size(), CvType.CV_8U);
        if (img.width() > 0) {

            try {
                Imgproc.cvtColor(img, result, Imgproc.COLOR_BGR2RGB);
            } catch (CvException e) {

                Log.e(TAG, "error al procesar imatge" + e.getMessage());
                //   Toast.makeText(this, fileURL + " saved", Toast.LENGTH_SHORT).show();

                Log.d(TAG, "loadedImage: " + "chans: " + img.channels()
                        + ", (" + img.width() + ", " + img.height() + ")");

                // image.release();
                // image = null;
            } catch (Exception e) {

                Log.e(TAG, "error al procesar imatge" + e.getMessage());
                //   Toast.makeText(this, fileURL + " saved", Toast.LENGTH_SHORT).show();

                Log.d(TAG, "loadedImage: " + "chans: " + img.channels()
                        + ", (" + img.width() + ", " + img.height() + ")");
            }
        }
        img.release();
        return result;
    }

    private double getMin(double R, double G, double B) {
        double result = 0.0;

        if (R < G && R < B)
            result = R;
        if (G < R && G < B)
            result = G;
        if (B < G && B < R)
            result = B;
        return result;

    }


    /**
     * Make the black background of a PNG-Bitmap-Image transparent.
     * code based on example at http://j.mp/1uCxOV5
     *
     * @return output image
     * @Param image png bitmap image
     */

  public void makeBlackTransparent() {
      Mat dst;//(src.rows,src.cols,CV_8UC4);
      Mat tmp, thr;

      tmp = new Mat(imagefinal.size(), CvType.CV_8UC4);
      thr = new Mat(imagefinal.size(), CvType.CV_8UC4);


      Imgproc.cvtColor(imagefinal, tmp, Imgproc.COLOR_RGB2GRAY);
      Imgproc.threshold(tmp, thr, 100, 255, Imgproc.THRESH_BINARY);

      List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
      Mat hierarchy = new Mat();
      int largest_contour_index = 0;
      int largest_area = 0;

      Mat alpha = new Mat(imagefinal.size(), CvType.CV_8UC1, new Scalar(255));
      Imgproc.findContours(tmp, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE); // Find the contours in the image
      for (int i = 0; i < contours.size(); i++) // iterate through each contour.
      {
          double a = Imgproc.contourArea(contours.get(i), false);  //  Find the area of contour
          if (a > largest_area) {
              largest_area = (int) a;
              largest_contour_index = i;                //Store the index of largest contour
          }
      }
      //  Imgproc.drawContours(alpha,contours,largest_contour_index, new Scalar(255),1,8, hierarchy);
      Imgproc.drawContours(alpha, contours, largest_contour_index, new Scalar(0));

  }
    public Bitmap getCroppedImage() {
        Bitmap bitmap;
        bitmap=null;

        try {
            bitmap = Bitmap.createBitmap(image.width(), image.height(), Bitmap.Config.ARGB_8888);
            bitmap .copy(Bitmap.Config.ARGB_8888, true);

            Utils.matToBitmap(BGRtoRGB(image), bitmap);
        } catch (OutOfMemoryError e) {
            Log.e(TAG, "Out of memory exception in convertMatToBitMap: " + e.getMessage());
        } catch (CvException e) {
            Log.e(TAG, "Convert to Bitmap exception : " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Convert to Bitmap exception : " + e.getMessage());
        }
        return bitmap;
    }

        public static String matToJson(Mat mat){
            JsonObject obj = new JsonObject();

            if(mat.isContinuous()){
                int cols = mat.cols();
                int rows = mat.rows();
                int elemSize = (int) mat.elemSize();

                byte[] data = new byte[cols * rows * elemSize];

                mat.get(0, 0, data);

                obj.addProperty("rows", mat.rows());
                obj.addProperty("cols", mat.cols());
                obj.addProperty("type", mat.type());

                // We cannot set binary data to a json object, so:
                // Encoding data byte array to Base64.
                String dataString = new String(Base64.encode(data, Base64.DEFAULT));

                obj.addProperty("data", dataString);

                Gson gson = new Gson();
                String json = gson.toJson(obj);

                return json;
            } else {
                Log.e(TAG, "Mat not continuous.");
            }
            return "{}";
        }

        public static Mat matFromJson(String json){
            JsonParser parser = new JsonParser();
            JsonObject JsonObject = parser.parse(json).getAsJsonObject();

            int rows = JsonObject.get("rows").getAsInt();
            int cols = JsonObject.get("cols").getAsInt();
            int type = JsonObject.get("type").getAsInt();

            String dataString = JsonObject.get("data").getAsString();
            byte[] data = Base64.decode(dataString.getBytes(), Base64.DEFAULT);

            Mat mat = new Mat(rows, cols, type);
            mat.put(0, 0, data);

            return mat;
        }

    private Mat getResizedImage(Mat img,int dst_size) {
        Mat img_dst=new Mat();
        int origWidth=img.width();
        int origHeight=img.height();
        int dstWidth,dstHeight;

        if((origWidth>dst_size)||(origHeight>dst_size))
        {

        float origAspect = (origWidth /origHeight);
        // output aspect ratio
        if((origAspect>=1 )&& (origWidth>dst_size)) {
            dstWidth = dst_size;
            dstHeight = (Math.round(dst_size * origHeight / origWidth));

            if ((origAspect < 1) && (origHeight > dst_size)) {
                dstHeight = dst_size;
                dstWidth = (Math.round(dst_size * origWidth / origHeight));

            }
        img_dst=new Mat(new Size(dstWidth,dstHeight),CvType.CV_8UC4);

try{
            Imgproc.resize(img, img_dst,img_dst.size(),0,0, Imgproc.INTER_AREA);}
catch (CvException e)
{
Log.e(TAG,e.getMessage());
}
        }
            return img_dst;
        }else {
            return img;
        }

    }
    public double getRedColor() {
        return colors.val[0];
    }
    public double getGreenColor() {
        return colors.val[1];
    }
    public double getBlueColor() {
        return colors.val[2];
    }

        private Scalar HSVtoRGB(Scalar hsv)
        {
            Mat src; //load image her
            Mat RGB=new Mat(1,1,CvType.CV_8UC3);
            Mat HSV=new Mat(1,1,CvType.CV_8UC3);
            HSV.setTo(hsv);

           Imgproc.cvtColor(HSV, RGB, Imgproc.COLOR_HSV2RGB);

            double A=0.0;
            double R=RGB.get(0,0)[0]; //red
            double G=RGB.get(0,0)[1]; //blue
            double B=RGB.get(0,0)[2]; //gree

            return new Scalar(R,G,B,A);

        }
}

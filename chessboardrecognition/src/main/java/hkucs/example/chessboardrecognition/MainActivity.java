package hkucs.example.chessboardrecognition;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;
import hkucs.example.chessboardrecognition.KmeanCluster.Centroid;
import hkucs.example.chessboardrecognition.KmeanCluster.Distance;
import hkucs.example.chessboardrecognition.KmeanCluster.HoughDistance;
import hkucs.example.chessboardrecognition.KmeanCluster.Intersection;
import hkucs.example.chessboardrecognition.KmeanCluster.KMeans;
import hkucs.example.chessboardrecognition.KmeanCluster.Line;
import hkucs.example.chessboardrecognition.KmeanCluster.LineWithPoint;
import hkucs.example.chessboardrecognition.KmeanCluster.PointDistance;
import hkucs.example.chessboardrecognition.KmeanCluster.ThetaDistance;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "OPENCV_DEBUG";

    // key
    public static final String KEY_PREF_CANNY_THRES_1 = "canny_thres_1";
    public static final String KEY_PREF_CANNY_THRES_2 = "canny_thres_2";
    public static final String KEY_PREF_HOUGHLINE_THRES = "houghline_thres";
    public static final String KEY_PREF_HOUGHLINE_MINLINELENGTH = "houghline_minlinelength";
    public static final String KEY_PREF_HOUGHLINE_MAXLINEGAP = "houghline_maxlinegap";
    public static final String KEY_PREF_BINARY_THRES = "binary_thres";


    // value
    public int cannyEdgeThres1;
    public int cannyEdgeThres2;
    public int houghLinesThres;
    public int houghLinesMinLineLength;
    public int houghLinesMaxLineGap;
    public int binaryThres;

    private Bitmap currentBitmap;
    Mat originalMat;

    // perpective transform
    Mat recentAppliedTransform;
    Mat chessboardMatchTransform;

    public static final int blockSize = 100;
    public static final int AOI_indent = 10;
    public static final int AOI_height = 80;

    public static final int AOI_height_origin = 600;

    // AOI threshold = min number of canny edge
    public static final int AOI_canny_thres = 80;

    Point[][] detectedChessboardModel;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    //DO YOUR WORK/STUFF HERE
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        cannyEdgeThres1 = Integer.parseInt(sharedPreferences.getString(KEY_PREF_CANNY_THRES_1,"40"));
        cannyEdgeThres2 = Integer.parseInt(sharedPreferences.getString(KEY_PREF_CANNY_THRES_2,"100"));

        houghLinesThres = Integer.parseInt(sharedPreferences.getString(KEY_PREF_HOUGHLINE_THRES,"200"));
        houghLinesMinLineLength = Integer.parseInt(sharedPreferences.getString(KEY_PREF_HOUGHLINE_MINLINELENGTH,"300"));
        houghLinesMaxLineGap = Integer.parseInt(sharedPreferences.getString(KEY_PREF_HOUGHLINE_MAXLINEGAP,"500"));
        binaryThres = Integer.parseInt(sharedPreferences.getString(KEY_PREF_BINARY_THRES,"120"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.filename, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.OpenGallery) {
            //Intent intent = new Intent(Intent.ACTION_PICK, Uri.parse("content://media/internal/images/media"));
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), 0);
            return true;
        }else if(id == R.id.CannyEdges){
            Canny();
            return true;
        }else if(id == R.id.HoughLine){
            try {
                HoughLines();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("HoughLine", "error in hough lines function");
            }
            return true;
        }else if(id == R.id.Contour){
            Contours();
            return true;
        }else if(id == R.id.Settings){
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }else if(id == R.id.lines_and_points){
            ClusterLinesAndIntersection();
            return true;
        }else if(id == R.id.match_chessboard){
            try {
                MatchChessboard();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }else if(id == R.id.blur_and_thres){
            blurAndAT();
            return true;
        }else if(id == R.id.perspective_transform){
            tranformInterestArea();
            return true;
        }else if(id == R.id.SubContour){
            SubContours();
            return true;
        }else if(id == R.id.show_AOI){
            showAOI_Grid();
            return true;
        }else if(id == R.id.show_AOI_origin){
            showAOI_Origin();
            return true;
        }

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                Log.d(TAG, "External storage1");
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
                    //resume tasks needing this permission
                    Log.d("STORAGE_PERMISSION", "TRUE");
                }else{
                    Log.d("STORAGE_PERMISSION", "FALSE");
                }
                break;
        }
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK && null != data) {
            // get file path of the picture
            Uri selectedImage = data.getData();
            Log.d("PATH", "Path is: " + selectedImage.toString());
            /*
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            //Log.d("PATH", "Path is: " + picturePath.toString());
            cursor.close();


            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap temp = BitmapFactory.decodeFile(picturePath, options);
             */
            Bitmap temp= null;
            try {
                temp = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }



            /*
            int orientation = 0;
            try {
                ExifInterface imgParams = new ExifInterface(picturePath);
                orientation = imgParams.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            } catch (IOException e) {
                e.printStackTrace();
            }
            // rotate bitmap
            Matrix rotate90 = new Matrix();
            rotate90.postRotate(orientation);
            Bitmap originalBitmap = Bitmap.createBitmap(temp, 0, 0, temp.getWidth(),
                    temp.getHeight(), rotate90, true);

             */

            // convert tempBitmap to mat and install in originalMat
            Bitmap tempBitmap = temp.copy(Bitmap.Config.ARGB_8888,true);
            originalMat = new Mat(tempBitmap.getHeight(), tempBitmap.getWidth(), CvType.CV_8U);
            Utils.bitmapToMat(tempBitmap, originalMat);

            // ToDO: do something with originalMat

            currentBitmap = temp.copy(Bitmap.Config.ARGB_8888,false);
            loadImageToImageView();
        }
    }

    private void loadImageToImageView()
    {
        ImageView imgView = (ImageView) findViewById(R.id.image_view);
        imgView.setImageBitmap(currentBitmap);
    }

    public  boolean isReadStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted1");
                return true;
            } else {
                Log.v(TAG,"Permission is revoked1");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted1");
            return true;
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this, mOpenCVCallBack);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mOpenCVCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        // check storage permission
        isReadStoragePermissionGranted();
    }

    public void blurAndAT(){
        // its modified version of canny, it only detect canny edge of darker pixel, so that it can remove unnecessary detail. You can convert back to original one by removing code of binary mat
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat binaryMat = new Mat();

        //Converting the image to grayscale
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.threshold(grayMat, binaryMat, binaryThres, 255, Imgproc.THRESH_BINARY);

        Imgproc.medianBlur(grayMat, grayMat, 23);

        //Imgproc.threshold(grayMat, binaryMat, binaryThres, 255, Imgproc.THRESH_BINARY);
        Imgproc.adaptiveThreshold(grayMat, binaryMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 9, 0);

        Imgproc.medianBlur(binaryMat, binaryMat, 5);


        //Converting Mat back to Bitmap
        Utils.matToBitmap(binaryMat, currentBitmap);
        loadImageToImageView();
    }


    public void Canny()
    {
        // its modified version of canny, it only detect canny edge of darker pixel, so that it can remove unnecessary detail. You can convert back to original one by removing code of binary mat
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat binaryMat = new Mat();

        //Converting the image to grayscale
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.threshold(grayMat, binaryMat, binaryThres, 255, Imgproc.THRESH_BINARY);

        Imgproc.medianBlur(grayMat, grayMat, 23);

        //Imgproc.threshold(grayMat, binaryMat, binaryThres, 255, Imgproc.THRESH_BINARY);
        Imgproc.adaptiveThreshold(grayMat, binaryMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 9, 0);

        Imgproc.medianBlur(binaryMat, binaryMat, 5);

        //Log.d("THRES", cannyEdgeThres1 + " " + cannyEdgeThres2);
        Imgproc.Canny(binaryMat, cannyEdges, cannyEdgeThres1, cannyEdgeThres2);

       //Mat kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15,15));
        //Imgproc.dilate(binaryMat, binaryMat, kernelDilate);

        //Mat kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
        //Imgproc.erode(binaryMat, binaryMat, kernelErode);



        //Converting Mat back to Bitmap
        Utils.matToBitmap(cannyEdges, currentBitmap);
        loadImageToImageView();
    }


    void HoughLines() throws IOException {
        Mat grayMat = new Mat();
        Mat binaryMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contourList = new
                ArrayList<MatOfPoint>();

        Mat lines = new Mat();
        //A list to store all the contours
        //Converting the image to grayscale

        //Converting the image to grayscale
        Imgproc.cvtColor(originalMat,grayMat,Imgproc.COLOR_BGR2GRAY);



        Imgproc.Canny(grayMat, cannyEdges,cannyEdgeThres1, cannyEdgeThres2);

        // dilate the binary mat
        Mat kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9,9));
        Imgproc.dilate(cannyEdges, cannyEdges, kernelDilate);

        //finding contours
        Imgproc.findContours(cannyEdges,contourList
                ,hierarchy,Imgproc.RETR_CCOMP,
                Imgproc.CHAIN_APPROX_SIMPLE);


        int index = 0;
        double maxim = Imgproc.contourArea(contourList.get(0));
        for (int contourIdx = 1; contourIdx < contourList.size();
             contourIdx++) {
            double temp;
            temp=Imgproc.contourArea(contourList.get(contourIdx));
            if(maxim<temp)
            {
                maxim=temp;
                index=contourIdx;
            }
        }

        Mat contour_mask = Mat.zeros(cannyEdges.rows()
                ,cannyEdges.cols(), CvType.CV_8UC1);
        Imgproc.drawContours(contour_mask, contourList, index, new Scalar(255),
                -1);

/*
        //Drawing contours on a new image
        Mat contours = new Mat();
        contours.create(cannyEdges.rows()
                ,cannyEdges.cols(),CvType.CV_8UC3);
        Random r = new Random();
        for(int i = 0; i < contourList.size(); i++)
        {
            Imgproc.drawContours(contours
                    ,contourList,i,new Scalar(r.nextInt(255)
                            ,r.nextInt(255),r.nextInt(255)), -1);
        }
        */
        Mat masked_mat = new Mat();
        //Log.d("Size", "Size 1: " + contour_mask.size() + " Size 2: " + originalMat.size());
        Core.bitwise_and(contour_mask, grayMat, masked_mat);

        Imgproc.medianBlur(masked_mat, masked_mat, 23);

        //Imgproc.threshold(grayMat, binaryMat, binaryThres, 255, Imgproc.THRESH_BINARY);
        Imgproc.adaptiveThreshold(masked_mat, binaryMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 9, 0);

        Imgproc.medianBlur(binaryMat, binaryMat, 5);

        Imgproc.Canny(binaryMat, cannyEdges, cannyEdgeThres1, cannyEdgeThres2);



        Imgproc.HoughLinesP(cannyEdges, lines, 1, Math.PI / 180, houghLinesThres, houghLinesMinLineLength, houghLinesMaxLineGap);

        //Imgproc.HoughLines(cannyEdges, lines, 1, Math.PI / 180, houghLinesThres, 0, 0, houghLinesMinLineLength, houghLinesMaxLineGap);

        Mat houghLines = originalMat.clone();
        //houghLines.create(cannyEdges.rows(),
        //        cannyEdges.cols(), CvType.CV_8UC1);

        List<Line> crossLine = new ArrayList<Line>();

        //Drawing lines on the image
        Log.d("LENGTH", " " + lines.rows());


        for (int i = 0; i < lines.rows(); i++) {
            double[] points = lines.get(i,0);
            Line l = new Line(points, currentBitmap);
            crossLine.add(l);

            Log.d("Line", "theta = " + l.getTheta() + " | Rtho = " + l.getRtho());
        }

        Map<Centroid, List<Line>> clusters = KMeans.fit(crossLine, 2, new ThetaDistance(), 100);

        //Random random = new Random();

        // Write line data to file for python analyzing
        //OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput("line_data.txt", Context.MODE_PRIVATE));
        // i indicates different clusters
        Scalar colors[] = new Scalar[]{new Scalar(255,0,0), new Scalar(0, 255, 0)};
        int i = 0;
        for(Centroid centroid: clusters.keySet()){
            //Scalar scalar = new Scalar(random.nextDouble()*255, random.nextDouble()*255, random.nextDouble()*255);
            for(Line line: clusters.get(centroid)){
                Imgproc.line(houghLines, line.getP1(), line.getP2(), colors[i], 2);

                //outputStreamWriter.write(i + " " + line.getTheta() + " " + line.getRtho() + "\n");
            }
            i++;
        }

        //outputStreamWriter.close();


        //Converting Mat back to Bitmap
        Utils.matToBitmap(houghLines, currentBitmap);
        loadImageToImageView();
    }


    // find maximum contour
    void Contours()
    {
        Mat grayMat = new Mat();
        Mat binaryMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contourList = new
                ArrayList<MatOfPoint>();

        Mat lines = new Mat();
        //A list to store all the contours
        //Converting the image to grayscale

        //Converting the image to grayscale
        Imgproc.cvtColor(originalMat,grayMat,Imgproc.COLOR_BGR2GRAY);



        Imgproc.Canny(grayMat, cannyEdges,cannyEdgeThres1, cannyEdgeThres2);

        // dilate the binary mat
        Mat kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9,9));
        Imgproc.dilate(cannyEdges, cannyEdges, kernelDilate);

        //finding contours
        Imgproc.findContours(cannyEdges,contourList
                ,hierarchy,Imgproc.RETR_CCOMP,
                Imgproc.CHAIN_APPROX_SIMPLE);


        int index = 0;
        double maxim = Imgproc.contourArea(contourList.get(0));
        for (int contourIdx = 1; contourIdx < contourList.size();
             contourIdx++) {
            double temp;
            temp=Imgproc.contourArea(contourList.get(contourIdx));
            if(maxim<temp)
            {
                maxim=temp;
                index=contourIdx;
            }
        }

        Mat contour_mask = Mat.zeros(cannyEdges.rows()
                ,cannyEdges.cols(), CvType.CV_8UC1);
        Imgproc.drawContours(contour_mask, contourList, index, new Scalar(255),
                -1);

/*
        //Drawing contours on a new image
        Mat contours = new Mat();
        contours.create(cannyEdges.rows()
                ,cannyEdges.cols(),CvType.CV_8UC3);
        Random r = new Random();
        for(int i = 0; i < contourList.size(); i++)
        {
            Imgproc.drawContours(contours
                    ,contourList,i,new Scalar(r.nextInt(255)
                            ,r.nextInt(255),r.nextInt(255)), -1);
        }
        */
        Mat masked_mat = new Mat();
        //Log.d("Size", "Size 1: " + contour_mask.size() + " Size 2: " + originalMat.size());
        Core.bitwise_and(contour_mask, grayMat, masked_mat);


        //Converting Mat back to Bitmap
        Utils.matToBitmap(masked_mat, currentBitmap);
        loadImageToImageView();
    }

    // find maximum contour
    void SubContours()
    {
        Mat grayMat = new Mat();
        Mat binaryMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contourList = new
                ArrayList<MatOfPoint>();

        Mat lines = new Mat();
        //A list to store all the contours
        //Converting the image to grayscale

        Mat currentMat = new Mat(currentBitmap.getHeight(), currentBitmap.getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(currentBitmap, currentMat);

        //Converting the image to grayscale
        Imgproc.cvtColor(currentMat,grayMat,Imgproc.COLOR_BGR2GRAY);



        Imgproc.Canny(grayMat, cannyEdges,cannyEdgeThres1, cannyEdgeThres2);

        // dilate the binary mat
        Mat kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9,9));
        Imgproc.dilate(cannyEdges, cannyEdges, kernelDilate);

        //finding contours
        Imgproc.findContours(cannyEdges,contourList
                ,hierarchy,Imgproc.RETR_CCOMP,
                Imgproc.CHAIN_APPROX_SIMPLE);

        /*
        int index = 0;
        double maxim = Imgproc.contourArea(contourList.get(0));
        for (int contourIdx = 1; contourIdx < contourList.size();
             contourIdx++) {
            double temp;
            temp=Imgproc.contourArea(contourList.get(contourIdx));
            if(maxim<temp)
            {
                maxim=temp;
                index=contourIdx;
            }
        }

        Mat contour_mask = Mat.zeros(cannyEdges.rows()
                ,cannyEdges.cols(), CvType.CV_8UC1);
        Imgproc.drawContours(contour_mask, contourList, index, new Scalar(255),
                -1);

         */


        //Drawing contours on a new image
        Mat contours = new Mat();
        contours.create(cannyEdges.rows()
                ,cannyEdges.cols(),CvType.CV_8UC3);
        Random r = new Random();
        for(int i = 0; i < contourList.size(); i++)
        {
            Imgproc.drawContours(contours
                    ,contourList,i,new Scalar(r.nextInt(255)
                            ,r.nextInt(255),r.nextInt(255)), -1);
        }

        /*
        Mat masked_mat = new Mat();
        //Log.d("Size", "Size 1: " + contour_mask.size() + " Size 2: " + originalMat.size());
        Core.bitwise_and(contour_mask, grayMat, masked_mat);

         */




        //Converting Mat back to Bitmap
        Utils.matToBitmap(contours, currentBitmap);
        loadImageToImageView();
    }


    public void ClusterLinesAndIntersection(){
        /**mask part**/
        Mat grayMat = new Mat();
        Mat binaryMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contourList = new
                ArrayList<MatOfPoint>();
        //A list to store all the contours
        //Converting the image to grayscale

        /* Canny Edge */
        //Converting the image to grayscale
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);


        Imgproc.Canny(grayMat, cannyEdges,cannyEdgeThres1, cannyEdgeThres2);

        // dilate the binary mat
        Mat kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15,15));
        Imgproc.dilate(cannyEdges, cannyEdges, kernelDilate);

        /* Contour */
        //finding contours
        Imgproc.findContours(cannyEdges,contourList
                ,hierarchy,Imgproc.RETR_CCOMP,
                Imgproc.CHAIN_APPROX_SIMPLE);


        // find largest contour
        int index = 0;
        double maxim = Imgproc.contourArea(contourList.get(0));
        for (int contourIdx = 1; contourIdx < contourList.size();
             contourIdx++) {
            double temp;
            temp=Imgproc.contourArea(contourList.get(contourIdx));
            if(maxim<temp)
            {
                maxim=temp;
                index=contourIdx;
            }
        }

        // get mask with in shape of largest contour
        Mat contour_mask = Mat.zeros(cannyEdges.rows()
                ,cannyEdges.cols(), CvType.CV_8UC1);
        Imgproc.drawContours(contour_mask, contourList, index, new Scalar(255),
                -1);

        Log.d("Contour", "size of contour is: " + contourList.get(index).size());

        /*
        // find outer corners of chessboard
        double epsilon = 0.1*Imgproc.arcLength(new MatOfPoint2f(contourList.get(index).toArray()),true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(new MatOfPoint2f(contourList.get(index).toArray()),approx,epsilon,true);
        Log.d("Contour corner", "Corner: " + approx.rows());

         */

        /*
        //Drawing all contours on a new image
        Mat contours = new Mat();
        contours.create(cannyEdges.rows()
                ,cannyEdges.cols(),CvType.CV_8UC3);
        Random r = new Random();
        for(int i = 0; i < contourList.size(); i++)
        {
            Imgproc.drawContours(contours
                    ,contourList,i,new Scalar(r.nextInt(255)
                            ,r.nextInt(255),r.nextInt(255)), -1);
        }
        */

        // apply mask on grayMat
        Mat masked_mat = new Mat();
        //Log.d("Size", "Size 1: " + contour_mask.size() + " Size 2: " + originalMat.size());
        Core.bitwise_and(contour_mask, grayMat, masked_mat);


        /**Hough Line part**/

        Mat lines = new Mat();
        /* find hough line */

        Imgproc.medianBlur(masked_mat, masked_mat, 23);

        //Imgproc.threshold(grayMat, binaryMat, binaryThres, 255, Imgproc.THRESH_BINARY);
        Imgproc.adaptiveThreshold(masked_mat, binaryMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 9, 0);

        Imgproc.medianBlur(binaryMat, binaryMat, 5);

        Imgproc.Canny(binaryMat, cannyEdges, cannyEdgeThres1, cannyEdgeThres2);

        Imgproc.HoughLinesP(cannyEdges, lines, 1, Math.PI / 180, houghLinesThres, houghLinesMinLineLength, houghLinesMaxLineGap);

        //Imgproc.HoughLines(cannyEdges, lines, 1, Math.PI / 180, houghLinesThres, 0, 0, houghLinesMinLineLength, houghLinesMaxLineGap);

        Mat houghLines = originalMat.clone();



        List<Line> crossLine = new ArrayList<Line>();

        Log.d("LENGTH", " " + lines.rows());

        // store line in list
        for (int i = 0; i < lines.rows(); i++) {
            double[] points = lines.get(i,0);
            Line l = new Line(points, currentBitmap);
            crossLine.add(l);
        }

        /* cluster lines in two groups -- horizontal group and vertical group */


        Map<Centroid, List<Line>> clusters = KMeans.fit(crossLine, 2, new HoughDistance(), 500);

        //Map<Centroid, List<Line>> clusters_a = KMeans.fit2((List<Line>)clusters.values().toArray()[0], 2, new HoughDistance(), 500);

        /*  cluster lines in each group by crossPointCluster -- expected 9,9  */
        Centroid verticalCent;
        Centroid horizontalCent;

        Centroid firstCent = (Centroid)clusters.keySet().toArray()[0];
        Centroid secondCent = (Centroid)clusters.keySet().toArray()[1];
        Log.d("Orientation", "first cent: " + Math.abs(firstCent.line.getTheta()));
        Log.d("Orientation", "second cent: " + Math.abs(secondCent.line.getTheta()));
        if(Math.abs(firstCent.line.getTheta()) > Math.abs(secondCent.line.getTheta())){
            horizontalCent = firstCent;
            verticalCent = secondCent;
        }else{
            horizontalCent = secondCent;
            verticalCent = firstCent;
        }

        List<LineWithPoint> lineWithPoints_v = new ArrayList<>();
        List<LineWithPoint> lineWithPoints_h = new ArrayList<>();

        // calculate intersection points between all vercital line and a horizoncel line

        // get one line of another group
        Line hzLine = clusters.get(horizontalCent).get(0);
        Line vtLine = clusters.get(verticalCent).get(0);

        for(Line line: clusters.get(verticalCent)){
            Point ins = Intersection.calculate(line.getP1(), line.getP2(), hzLine.getP1(), hzLine.getP2());
            lineWithPoints_v.add(new LineWithPoint(line, ins));
        }

        for(Line line: clusters.get(horizontalCent)){
            Point ins = Intersection.calculate(line.getP1(), line.getP2(), vtLine.getP1(), vtLine.getP2());
            lineWithPoints_h.add(new LineWithPoint(line, ins));
        }

        // cluster cross points by coordinates
        Map<Centroid, List<LineWithPoint>> verticalIntersectionClusters = KMeans.fit2_point(lineWithPoints_v, 60, new PointDistance(), 500);
        Map<Centroid, List<LineWithPoint>> horizontalIntersectionClusters = KMeans.fit2_point(lineWithPoints_h, 60, new PointDistance(), 500);

        // sort verticalIntersectionClusters's keys in order according to their point
        Map<Centroid, List<LineWithPoint>> sortedVerticalClusters = new TreeMap<>(new CentroidComparator());
        Map<Centroid, List<LineWithPoint>> sortedHorizontalClusters = new TreeMap<>(new CentroidComparator());
        sortedVerticalClusters.putAll(verticalIntersectionClusters);
        sortedHorizontalClusters.putAll(horizontalIntersectionClusters);

        // convert to standard cluster type for looping
        ArrayList<List<Line>> sortedVerticalClusters_SD = new ArrayList<>();
        ArrayList<List<Line>> sortedHorizontalClusters_SD = new ArrayList<>();

        for(Map.Entry<Centroid, List<LineWithPoint>> entry: sortedVerticalClusters.entrySet()){
            // Convert List<LineWithPoint> to List<Line>
            Log.d("Point", "GP ---------");
            List<Line> gp = new ArrayList<>();
            for(LineWithPoint lp: entry.getValue()){
                gp.add(lp.line);
                Log.d("Point", " x = " + lp.point.x + " y = " + lp.point.y);
            }
            sortedVerticalClusters_SD.add(gp);
        }

        for(Map.Entry<Centroid, List<LineWithPoint>> entry: sortedHorizontalClusters.entrySet()){
            // Convert List<LineWithPoint> to List<Line>
            Log.d("Point", "GP ---------");
            List<Line> gp = new ArrayList<>();
            for(LineWithPoint lp: entry.getValue()){
                gp.add(lp.line);
                Log.d("Point", " x = " + lp.point.x + " y = " + lp.point.y);
            }
            sortedHorizontalClusters_SD.add(gp);
        }



        Random random = new Random();

        // Write line data to file for python analyzing
        // i indicates different clusters

        int colors[] = new int[]{
                Color.BLUE,
                Color.YELLOW,
                Color.CYAN,
                Color.MAGENTA,
                Color.BLACK,
                Color.DKGRAY,
                Color.GRAY,
                Color.LTGRAY,
                Color.WHITE,
                Color.RED,
                Color.GREEN,
                0x663366,
                0xFF6666,
                0xCCCC00,
                0x734C00,
                0x3CCC00
        };


        /* Calculate mean line of each group and removing duplicated line */
        List<Line> verticalCrossLine = new ArrayList<>();
        List<Line> horizontalCrossLine = new ArrayList<>();
        //int i = 0;
        for(List<Line> line_gp: sortedVerticalClusters_SD){
            Scalar scalar = new Scalar(random.nextDouble()*255, random.nextDouble()*255, random.nextDouble()*255, 255);
            // Log.d("Line", "Color: " + scalar);
            Line meanLine = calculateMeanLine(line_gp);
            verticalCrossLine.add(meanLine);
            Imgproc.line(houghLines, meanLine.getP1(), meanLine.getP2(), scalar, 10);
            //i++;
        }

        //i = 0;
        for(List<Line> line_gp: sortedHorizontalClusters_SD){
            Scalar scalar = new Scalar(random.nextDouble()*255, random.nextDouble()*255, random.nextDouble()*255, 255);
            //Log.d("Line", "Color: " + scalar);
            Line meanLine = calculateMeanLine(line_gp);
            horizontalCrossLine.add(meanLine);
            Imgproc.line(houghLines, meanLine.getP1(), meanLine.getP2(), scalar, 10);
            //i++;
        }


        /*for(int i = 0; i < approx.rows(); i++){
            Point p = new Point(approx.get(i, 0));
            Imgproc.circle(masked_mat, p,
                    10, new Scalar(255,0,0), 2);
        }*/



        /*  calculate intersection points between two refined groups */
        List<List<Point>> intersectPoints = new ArrayList<>();

        for(Line line_a: horizontalCrossLine){
            List<Point> rowPoints = new ArrayList<>();
            for(Line line_b: verticalCrossLine){
                rowPoints.add(Intersection.calculate(line_a.getP1(), line_a.getP2(), line_b.getP1(), line_b.getP2()));
            }
            intersectPoints.add(rowPoints);
        }


        for(List<Point> row : intersectPoints){
            for(Point p : row)
                Imgproc.circle(houghLines, p,
                        30, new Scalar(0,255,0, 255), 10);
            //outputStreamWriter.write(i + " " + p.x + " " + p.y + "\n");
        }

        //Converting Mat back to Bitmap
        Utils.matToBitmap(houghLines, currentBitmap);
        loadImageToImageView();


    }



    public void MatchChessboard() throws IOException {

        /**mask part**/
        Mat grayMat = new Mat();
        Mat binaryMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contourList = new
                ArrayList<MatOfPoint>();
        //A list to store all the contours
        //Converting the image to grayscale

        /* Canny Edge */
        //Converting the image to grayscale
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);


        Imgproc.Canny(grayMat, cannyEdges,cannyEdgeThres1, cannyEdgeThres2);

        // dilate the binary mat
        Mat kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15,15));
        Imgproc.dilate(cannyEdges, cannyEdges, kernelDilate);

        /* Contour */
        //finding contours
        Imgproc.findContours(cannyEdges,contourList
                ,hierarchy,Imgproc.RETR_CCOMP,
                Imgproc.CHAIN_APPROX_SIMPLE);


        // find largest contour
        int index = 0;
        double maxim = Imgproc.contourArea(contourList.get(0));
        for (int contourIdx = 1; contourIdx < contourList.size();
             contourIdx++) {
            double temp;
            temp=Imgproc.contourArea(contourList.get(contourIdx));
            if(maxim<temp)
            {
                maxim=temp;
                index=contourIdx;
            }
        }

        // get mask with in shape of largest contour
        Mat contour_mask = Mat.zeros(cannyEdges.rows()
                ,cannyEdges.cols(), CvType.CV_8UC1);
        Imgproc.drawContours(contour_mask, contourList, index, new Scalar(255),
                -1);

        Log.d("Contour", "size of contour is: " + contourList.get(index).size());

        /*
        // find outer corners of chessboard
        double epsilon = 0.1*Imgproc.arcLength(new MatOfPoint2f(contourList.get(index).toArray()),true);
        MatOfPoint2f approx = new MatOfPoint2f();
        Imgproc.approxPolyDP(new MatOfPoint2f(contourList.get(index).toArray()),approx,epsilon,true);
        Log.d("Contour corner", "Corner: " + approx.rows());

         */

        /*
        //Drawing all contours on a new image
        Mat contours = new Mat();
        contours.create(cannyEdges.rows()
                ,cannyEdges.cols(),CvType.CV_8UC3);
        Random r = new Random();
        for(int i = 0; i < contourList.size(); i++)
        {
            Imgproc.drawContours(contours
                    ,contourList,i,new Scalar(r.nextInt(255)
                            ,r.nextInt(255),r.nextInt(255)), -1);
        }
        */

        // apply mask on grayMat
        Mat masked_mat = new Mat();
        //Log.d("Size", "Size 1: " + contour_mask.size() + " Size 2: " + originalMat.size());
        Core.bitwise_and(contour_mask, grayMat, masked_mat);


        /**Hough Line part**/

        Mat lines = new Mat();
        /* find hough line */

        Imgproc.medianBlur(masked_mat, masked_mat, 23);

        //Imgproc.threshold(grayMat, binaryMat, binaryThres, 255, Imgproc.THRESH_BINARY);
        Imgproc.adaptiveThreshold(masked_mat, binaryMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 9, 0);

        Imgproc.medianBlur(binaryMat, binaryMat, 5);

        Imgproc.Canny(binaryMat, cannyEdges, cannyEdgeThres1, cannyEdgeThres2);

        Imgproc.HoughLinesP(cannyEdges, lines, 1, Math.PI / 180, houghLinesThres, houghLinesMinLineLength, houghLinesMaxLineGap);

        //Imgproc.HoughLines(cannyEdges, lines, 1, Math.PI / 180, houghLinesThres, 0, 0, houghLinesMinLineLength, houghLinesMaxLineGap);

        Mat houghLines = originalMat.clone();



        List<Line> crossLine = new ArrayList<Line>();

        Log.d("LENGTH", " " + lines.rows());

        // store line in list
        for (int i = 0; i < lines.rows(); i++) {
            double[] points = lines.get(i,0);
            Line l = new Line(points, currentBitmap);
            crossLine.add(l);
        }

        /* cluster lines in two groups -- horizontal group and vertical group */


        Map<Centroid, List<Line>> clusters = KMeans.fit(crossLine, 2, new HoughDistance(), 500);

        //Map<Centroid, List<Line>> clusters_a = KMeans.fit2((List<Line>)clusters.values().toArray()[0], 2, new HoughDistance(), 500);

        /*  cluster lines in each group by crossPointCluster -- expected 9,9  */
        Centroid verticalCent;
        Centroid horizontalCent;

        Centroid firstCent = (Centroid)clusters.keySet().toArray()[0];
        Centroid secondCent = (Centroid)clusters.keySet().toArray()[1];
        //Log.d("Orientation", "first cent: " + Math.abs(firstCent.line.getTheta()));
        //Log.d("Orientation", "second cent: " + Math.abs(secondCent.line.getTheta()));
        if(Math.abs(firstCent.line.getTheta()) > Math.abs(secondCent.line.getTheta())){
            horizontalCent = secondCent;
            verticalCent = firstCent;
        }else{
            horizontalCent = firstCent;
            verticalCent = secondCent;
        }

        List<LineWithPoint> lineWithPoints_v = new ArrayList<>();
        List<LineWithPoint> lineWithPoints_h = new ArrayList<>();

        // calculate intersection points between all vercital line and a horizoncel line

        // get one line of another group
        Line hzLine = clusters.get(horizontalCent).get(0);
        Line vtLine = clusters.get(verticalCent).get(0);

        for(Line line: clusters.get(verticalCent)){
            Point ins = Intersection.calculate(line.getP1(), line.getP2(), hzLine.getP1(), hzLine.getP2());
            lineWithPoints_v.add(new LineWithPoint(line, ins));
        }

        for(Line line: clusters.get(horizontalCent)){
            Point ins = Intersection.calculate(line.getP1(), line.getP2(), vtLine.getP1(), vtLine.getP2());
            lineWithPoints_h.add(new LineWithPoint(line, ins));
        }

        // cluster cross points by coordinates
        Map<Centroid, List<LineWithPoint>> verticalIntersectionClusters = KMeans.fit2_point(lineWithPoints_v, 60, new PointDistance(), 500);
        Map<Centroid, List<LineWithPoint>> horizontalIntersectionClusters = KMeans.fit2_point(lineWithPoints_h, 60, new PointDistance(), 500);

        // sort verticalIntersectionClusters's keys in order according to their point
        Map<Centroid, List<LineWithPoint>> sortedVerticalClusters = new TreeMap<>(new CentroidComparator());
        Map<Centroid, List<LineWithPoint>> sortedHorizontalClusters = new TreeMap<>(new CentroidComparator());
        sortedVerticalClusters.putAll(verticalIntersectionClusters);
        sortedHorizontalClusters.putAll(horizontalIntersectionClusters);

        // convert to standard cluster type for looping
        ArrayList<List<Line>> sortedVerticalClusters_SD = new ArrayList<>();
        ArrayList<List<Line>> sortedHorizontalClusters_SD = new ArrayList<>();

        for(Map.Entry<Centroid, List<LineWithPoint>> entry: sortedVerticalClusters.entrySet()){
            // Convert List<LineWithPoint> to List<Line>
            Log.d("Point", "GP ---------");
            List<Line> gp = new ArrayList<>();
            for(LineWithPoint lp: entry.getValue()){
                gp.add(lp.line);
                Log.d("Point", " x = " + lp.point.x + " y = " + lp.point.y);
            }
            sortedVerticalClusters_SD.add(gp);
        }

        for(Map.Entry<Centroid, List<LineWithPoint>> entry: sortedHorizontalClusters.entrySet()){
            // Convert List<LineWithPoint> to List<Line>
            Log.d("Point", "GP ---------");
            List<Line> gp = new ArrayList<>();
            for(LineWithPoint lp: entry.getValue()){
                gp.add(lp.line);
                Log.d("Point", " x = " + lp.point.x + " y = " + lp.point.y);
            }
            sortedHorizontalClusters_SD.add(gp);
        }



        Random random = new Random();

        // Write line data to file for python analyzing
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(this.openFileOutput("line_data.txt", Context.MODE_PRIVATE));
        // i indicates different clusters

        int colors[] = new int[]{
                Color.BLUE,
                Color.YELLOW,
                Color.CYAN,
                Color.MAGENTA,
                Color.BLACK,
                Color.DKGRAY,
                Color.GRAY,
                Color.LTGRAY,
                Color.WHITE,
                Color.RED,
                Color.GREEN,
                0x663366,
                0xFF6666,
                0xCCCC00,
                0x734C00,
                0x3CCC00
        };


        /* Calculate mean line of each group and removing duplicated line */
        List<Line> verticalCrossLine = new ArrayList<>();
        List<Line> horizontalCrossLine = new ArrayList<>();
        //int i = 0;
        for(List<Line> line_gp: sortedVerticalClusters_SD){
            Scalar scalar = new Scalar(random.nextDouble()*255, random.nextDouble()*255, random.nextDouble()*255, 255);
            //Log.d("Line", "Color: " + scalar);
            Line meanLine = calculateMeanLine(line_gp);
            verticalCrossLine.add(meanLine);
            Imgproc.line(houghLines, meanLine.getP1(), meanLine.getP2(), scalar, 10);
            //i++;
        }

        //i = 0;
        for(List<Line> line_gp: sortedHorizontalClusters_SD){
            Scalar scalar = new Scalar(random.nextDouble()*255, random.nextDouble()*255, random.nextDouble()*255, 255);
            //Log.d("Line", "Color: " + scalar);
            Line meanLine = calculateMeanLine(line_gp);
            horizontalCrossLine.add(meanLine);
            Imgproc.line(houghLines, meanLine.getP1(), meanLine.getP2(), scalar, 10);
            //i++;
        }



        //outputStreamWriter.write(i + " " + line.getTheta() + " " + line.getRtho() + "\n");

        outputStreamWriter.close();



        /*for(int i = 0; i < approx.rows(); i++){
            Point p = new Point(approx.get(i, 0));
            Imgproc.circle(masked_mat, p,
                    10, new Scalar(255,0,0), 2);
        }*/



        /*  calculate intersection points between two refined groups */
        List<List<Point>> intersectPoints = new ArrayList<>();

        Collections.reverse(verticalCrossLine);

        for(Line line_a: verticalCrossLine){
            List<Point> rowPoints = new ArrayList<>();
            for(Line line_b: horizontalCrossLine){
                rowPoints.add(Intersection.calculate(line_a.getP1(), line_a.getP2(), line_b.getP1(), line_b.getP2()));
            }
            intersectPoints.add(rowPoints);
        }

        /*
        for(List<Point> row : intersectPoints){
            for(Point p : row)
                Imgproc.circle(houghLines, p,
                    30, new Scalar(0,255,0, 255), 10);
            //outputStreamWriter.write(i + " " + p.x + " " + p.y + "\n");
        }
        */



        /**  match chessboard part **/
        if(intersectPoints.size() > 8 && intersectPoints.get(0).size() > 8) {
            Point[][] chessboardReferenceModel = new Point[9][9];
            // initialize the chessboard
            for (int i = 0; i < 9; i++)
                for (int j = 0; j < 9; j++) {
                    chessboardReferenceModel[i][j] = new Point(blockSize * j, blockSize * i);
                }

            // chessboard kernel mat
            List<List<Point>> kernelMat = new ArrayList<>();
            double minDistance = Double.MAX_VALUE;

            // top left corner
            int idx_x = 0;
            int idx_y = 0;
            // outer Dot Mat loop, locate top left corner of the mat, size of chessboard is 9x9
            // boundary is 8 not 9
            for (int row = 0; row < intersectPoints.size() - 8; row++)
                for (int col = 0; col < intersectPoints.get(row).size() - 8; col++) {
                    // inner Chessboard kernel loop

                    // crop 9x9 points array
                    Point[][] chessboardModel = new Point[9][9];
                    for (int i = 0; i < 9; i++) {
                        for (int j = 0; j < 9; j++) {
                            chessboardModel[i][j] = intersectPoints.get(row + i).get(col + j);
                        }
                    }
                    // calculate distance from this model to chessboard reference model
                    double bD = distanceToChessboardModel(chessboardModel, chessboardReferenceModel);
                    //Log.d("Distance", "Distance to model reference: " + bD);

                    if (bD < minDistance) {
                        chessboardMatchTransform = recentAppliedTransform;
                        detectedChessboardModel = chessboardModel;
                        minDistance = bD;
                        idx_x = row;
                        idx_y = col;
                    }
                }

            for (int i = 0; i < 9; i++)
                for (int j = 0; j < 9; j++) {
                    Point p = intersectPoints.get(idx_x + i).get(idx_y + j);
                    Imgproc.circle(houghLines, p,
                            30, new Scalar(0, 255, 0, 255), 10);
                }
        }



        //Converting Mat back to Bitmap
        Utils.matToBitmap(houghLines, currentBitmap);
        loadImageToImageView();

    }

    public void tranformInterestArea(){
        // its modified version of canny, it only detect canny edge of darker pixel, so that it can remove unnecessary detail. You can convert back to original one by removing code of binary mat
        Mat grayMat = new Mat();
        Mat cannyEdges = new Mat();
        Mat binaryMat = new Mat();

        //Converting the image to grayscale
        Imgproc.cvtColor(originalMat, grayMat, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.threshold(grayMat, binaryMat, binaryThres, 255, Imgproc.THRESH_BINARY);

        Imgproc.medianBlur(grayMat, grayMat, 23);

        //Imgproc.threshold(grayMat, binaryMat, binaryThres, 255, Imgproc.THRESH_BINARY);
        Imgproc.adaptiveThreshold(grayMat, binaryMat, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 9, 0);

        Imgproc.medianBlur(binaryMat, binaryMat, 5);

        //Log.d("THRES", cannyEdgeThres1 + " " + cannyEdgeThres2);
        Imgproc.Canny(binaryMat, cannyEdges, cannyEdgeThres1, cannyEdgeThres2);

        // dilate the binary mat
        //Mat kernelErode = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(3, 3));
        //Imgproc.erode(cannyEdges, cannyEdges, kernelErode);

        if(chessboardMatchTransform != null) {
            Mat outputMat = new Mat(8 * blockSize + 1, 8 * blockSize + 1, CvType.CV_8UC4);
            Imgproc.warpPerspective(originalMat, outputMat, chessboardMatchTransform, new Size(8 * blockSize + 1, 8 * blockSize + 1));
            currentBitmap = Bitmap.createBitmap(8 * blockSize + 1, 8 * blockSize + 1, Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(outputMat, currentBitmap);
            loadImageToImageView();
        }
    }

    public void showAOI_Grid(){
        Mat chessboardMat = new Mat(currentBitmap.getHeight(), currentBitmap.getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(currentBitmap, chessboardMat);
        for(int i = 0; i < 8; i ++)
            for(int j = 0; j < 8; j ++){
                Point start = new Point(blockSize*i + AOI_indent, blockSize * j + AOI_indent);
                Point end = new Point(blockSize*(i+1) - AOI_indent, blockSize * j + AOI_indent + AOI_height);
                Imgproc.rectangle(chessboardMat, start, end, new Scalar(0, 255, 0, 255), 1);
                Log.d("CannyEdgeInAOI", "col: " + i + ", row: " + j + " Edge: " + countCannyEdgeInAOI(chessboardMat, start, end, binaryThres));

            }

        Utils.matToBitmap(chessboardMat, currentBitmap);
        loadImageToImageView();
    }

    public void showAOI_Origin(){
        int[][] occupancy = new int[8][8];
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++) {
                Log.d("Occ", "occupancy[i][j]");

            }
        Mat chessboardMat = new Mat(currentBitmap.getHeight(), currentBitmap.getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(currentBitmap, chessboardMat);

        // find possible occupancy on chessboard
        for(int i = 0; i < 8; i ++)
            for(int j = 0; j < 8; j ++){
                Point start = new Point(blockSize*i + AOI_indent, blockSize * j + AOI_indent);
                Point end = new Point(blockSize*(i+1) - AOI_indent, blockSize * j + AOI_indent + AOI_height);
                //Imgproc.rectangle(chessboardMat, start, end, new Scalar(0, 255, 0, 255), 1);
                Log.d("CannyEdgeInAOI", "col: " + i + ", row: " + j + " Edge: " + countCannyEdgeInAOI(chessboardMat, start, end, binaryThres));
                if (countCannyEdgeInAOI(chessboardMat, start, end, binaryThres) > AOI_canny_thres) {
                    occupancy[j][i] = 1;
                    Imgproc.rectangle(chessboardMat, start, end, new Scalar(0, 255, 0, 255), 1);
                }
            }
        Mat AOI_mat = originalMat.clone();

        for (int i = 0; i < 8; i++) {
            Log.d("Chessboard Intersection", " row ----");
            for (int j = 0; j < 8; j++) {
                Log.d("Chessboard Intersection", "x: " + detectedChessboardModel[i][j].x + ", y: " + detectedChessboardModel[i][j].y);
            }
        }

        // localize piece on original image
        if (detectedChessboardModel != null) {
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++) {
                    if(occupancy[i][j] != 0){
                        // four corner points are; detectedChessboardModel[i][j], detectedChessboardModel[i + 1][j], detectedChessboardModel[i][j + 1], and detectedChessboardModel[i + 1][j + 1],
                        Point[] rect = getAOI(detectedChessboardModel[i][j], detectedChessboardModel[i + 1][j], detectedChessboardModel[i][j + 1], detectedChessboardModel[i + 1][j + 1]);
                        Imgproc.rectangle(AOI_mat, rect[0], rect[1], new Scalar(0, 255, 0, 255), 5);
                    }
                }
        }

        currentBitmap = Bitmap.createBitmap(originalMat.width(), originalMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(AOI_mat, currentBitmap);
        loadImageToImageView();
    }

    public Point[] getAOI(Point p1, Point p2, Point p3, Point p4){
        Point[] rectangle = new Point[2];
        double left = Math.min(p1.x, Math.min(p2.x, Math.min(p3.x, p4.x)));
        double right = Math.max(p1.x, Math.max(p2.x, Math.max(p3.x, p4.x)));
        double bottom = Math.max(p1.y, Math.max(p2.y, Math.max(p3.y, p4.y)));
        rectangle[0] = new Point(left, bottom);
        rectangle[1] = new Point(right, bottom - AOI_height_origin);
        return rectangle;
    }

    public int countCannyEdgeInAOI(Mat src, Point start, Point end, int threshold){
        int tot = 0;
        for(int i = (int)start.y; i <= end.y; i++)
            for(int j = (int)start.x; j <= end.x; j++){
                if(src.get(i,j)[0] > threshold)
                    tot ++;
            }

        return tot;
    }


    // calculate transform matrix and calculate distance after map other inner points to reference model
    public double distanceToChessboardModel(Point[][] src, Point[][] model){

        MatOfPoint2f src4corner = new MatOfPoint2f(src[0][0], src[0][src[0].length-1], src[src.length-1][src[0].length-1], src[src.length-1][0]);
        MatOfPoint2f model4corner = new MatOfPoint2f(model[0][0], model[0][model[0].length-1], model[model.length-1][model[0].length-1], model[model.length-1][0]);

        Mat transform = Imgproc.getPerspectiveTransform(src4corner, model4corner);
        recentAppliedTransform = transform;

        // convert src format
        MatOfPoint2f srcMat = new MatOfPoint2f();
        for(Point[] row: src){
            MatOfPoint2f rowMat = new MatOfPoint2f(row);
            srcMat.push_back(rowMat);
        }

        // convert model format
        MatOfPoint2f modelMat = new MatOfPoint2f();
        for(Point[] row: model){
            MatOfPoint2f rowMat = new MatOfPoint2f(row);
            modelMat.push_back(rowMat);
        }

        // calculate projected points on reference model
        MatOfPoint2f dstMat = new MatOfPoint2f();
        Core.perspectiveTransform(srcMat, dstMat, transform);

        return chessboardDistance(dstMat, modelMat);

    }

    // compute distance of each pair points
    // boardA boardB should have the same size
    public double chessboardDistance(MatOfPoint2f boardA, MatOfPoint2f boardB){
        Distance dist = new PointDistance();
        double tot_dist = 0;
        for(int i = 0; i < boardA.rows(); i ++){
            tot_dist += dist.calculate(new Point(boardA.get(i, 0)), new Point(boardB.get(i, 0)));
        }
        return tot_dist/boardA.rows();
    }

    public Line calculateMeanLine(List<Line> lines){
        // find mean point of p1 and p2 set
        double p1_x_tot = 0;
        double p1_y_tot = 0;
        double p2_x_tot = 0;
        double p2_y_tot = 0;

        for(Line line: lines){
            //Log.d("Line", i + " theta = " + line.getTheta() + " | Rtho = " + line.getRtho());
            //outputStreamWriter.write(i + " " + line.getTheta() + " " + line.getRtho() + "\n");
            p1_x_tot += line.getP1().x;
            p1_y_tot += line.getP1().y;
            p2_x_tot += line.getP2().x;
            p2_y_tot += line.getP2().y;
        }

        return new Line(new double[]{p1_x_tot/lines.size(), p1_y_tot/lines.size(), p2_x_tot/lines.size(), p2_y_tot/lines.size()});
    }

    public Scalar toRGB(int hex) {
        float red   = (hex >> 16) & 0xFF;
        float green = (hex >> 8)  & 0xFF;
        float blue  = (hex)       & 0xFF;

        return new Scalar(red, green, blue);
        //System.out.println("red="+red+"--green="+green+"--blue="+blue);
    }

    private class CentroidComparator implements Comparator<Centroid>
    {
        @Override
        public int compare (Centroid o1, Centroid o2){
            Point p1 = o1.point;
            Point p2 = o2.point;
            if (p1.x != p2.x) {
                // compare x
                if (p1.x > p2.x)
                    return 1;
                else
                    return -1;
            } else {
                // compare y
                if (p1.y == p2.y)
                    return 0;
                else if (p1.y > p2.y)
                    return 1;
                else
                    return -1;
            }
        }
    }
}

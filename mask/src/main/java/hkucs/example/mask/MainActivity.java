package hkucs.example.mask;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
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
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "OPENCV_DEBUG";

    private final int SELECT_PHOTO_1 = 1;
    private final int SELECT_PHOTO_2 = 2;

    Mat src_frame, src_bg;
    private ImageView frameImage, bgImage;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
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

        frameImage = findViewById(R.id.frame_img);
        bgImage = findViewById(R.id.bg_img);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mOpenCVCallBack);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mOpenCVCallBack.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_load_frame_image) {
            Intent photoPickerIntent = new
                    Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent,
                    SELECT_PHOTO_1);
            return true;
        } else if (id == R.id.action_load_bg_image) {
            Intent photoPickerIntent = new
                    Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent,
                    SELECT_PHOTO_2);
            return true;
        } else if(id == R.id.action_diff){
            if (src_frame != null && src_bg != null) {
                new AsyncTask<Void, Void, Bitmap>() {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        //startTime = System.currentTimeMillis();
                    }

                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        return executeTask_2();
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        frameImage.setImageBitmap(bitmap);
                        /*
                        tvKeyPointsObject1.setText("Object 1 : "+keypointsObject1);
                        tvKeyPointsObject2.setText("Object 2 : "+keypointsObject2);
                        tvKeyPointsMatches.setText("Keypoint Matches : "+keypointMatches);
                        tvTime.setText("Time taken : "+(endTime-startTime)+" ms");

                         */
                    }
                }.execute();
            }
        } else if (id == R.id.compute_mask_and_show) {
            if (src_frame != null && src_bg != null) {
                new AsyncTask<Void, Void, Bitmap>() {

                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        //startTime = System.currentTimeMillis();
                    }

                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        return executeTask();
                    }

                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        frameImage.setImageBitmap(bitmap);
                        /*
                        tvKeyPointsObject1.setText("Object 1 : "+keypointsObject1);
                        tvKeyPointsObject2.setText("Object 2 : "+keypointsObject2);
                        tvKeyPointsMatches.setText("Keypoint Matches : "+keypointMatches);
                        tvTime.setText("Time taken : "+(endTime-startTime)+" ms");

                         */
                    }
                }.execute();
            }
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                Log.d(TAG, "External storage1");
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
                    //resume tasks needing this permission
                } else {

                }
                break;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Code to load image into a Bitmap and convert it to a Mat
        if (resultCode == RESULT_OK) {

            Uri selectedPath = data.getData();
            Log.d("PATH", selectedPath.toString());
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(selectedPath, filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();


            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap selectedImage = BitmapFactory.decodeFile(picturePath, options);


            switch (requestCode) {
                case SELECT_PHOTO_1:
                    src_frame = new Mat(selectedImage.getHeight(),
                            selectedImage.getWidth(), CvType.CV_8UC4);
                    Utils.bitmapToMat(selectedImage, src_frame);
                    frameImage.setImageBitmap(selectedImage);
                    break;

                case SELECT_PHOTO_2:
                    src_bg = new Mat(selectedImage.getHeight(),
                            selectedImage.getWidth(), CvType.CV_8UC4);
                    Utils.bitmapToMat(selectedImage, src_bg);
                    bgImage.setImageBitmap(selectedImage);

                    break;
            }


        }
    }

    private Bitmap executeTask() {
        // define result
        Bitmap resultBitmap;

        Mat differenceMat = new Mat();

        // temp mat
        Mat binaryMat = new Mat();
        Mat grayMat_frame = new Mat();
        Mat grayMat_bg = new Mat();



        // do conversion
        Imgproc.cvtColor(src_frame, grayMat_frame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(src_bg, grayMat_bg, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.threshold(grayMat, binaryMat, 100, 255, Imgproc.THRESH_BINARY);

        // subtraction
        Core.subtract(grayMat_frame, grayMat_bg, differenceMat);

        // abs
        Core.absdiff(grayMat_frame, grayMat_bg, differenceMat);

        // threshold
        Imgproc.threshold(differenceMat, differenceMat,30, 255, Imgproc.THRESH_BINARY);

        Mat cannyEdges = new Mat();
        Mat hierarchy = new Mat();
        List<MatOfPoint> contourList = new
                ArrayList<MatOfPoint>();
        //A list to store all the contours
        //Converting the image to grayscale

        // dilate the binary mat
        Mat kernelDilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(15,15));
        Imgproc.dilate(differenceMat, differenceMat, kernelDilate);

        Imgproc.Canny(differenceMat, cannyEdges,10, 100);

        //finding contours
        Imgproc.findContours(cannyEdges,contourList
                ,hierarchy,Imgproc.RETR_CCOMP,
                Imgproc.CHAIN_APPROX_SIMPLE);

        // draw contour
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


        // initialize result bitmap
        resultBitmap = Bitmap.createBitmap(differenceMat.cols(),
                differenceMat.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(contours, resultBitmap);

        return resultBitmap;
    }

    private Bitmap executeTask_2() {
        // define result
        Bitmap resultBitmap;

        Mat differenceMat = new Mat();

        // temp mat
        Mat binaryMat = new Mat();
        Mat grayMat_frame = new Mat();
        Mat grayMat_bg = new Mat();



        // do conversion
        Imgproc.cvtColor(src_frame, grayMat_frame, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(src_bg, grayMat_bg, Imgproc.COLOR_BGR2GRAY);
        //Imgproc.threshold(grayMat, binaryMat, 100, 255, Imgproc.THRESH_BINARY);

        // subtraction
        Core.subtract(grayMat_frame, grayMat_bg, differenceMat);

        // abs
        Core.absdiff(grayMat_frame, grayMat_bg, differenceMat);

        // threshold
        Imgproc.threshold(differenceMat, differenceMat,30, 255, Imgproc.THRESH_BINARY);




        // initialize result bitmap
        resultBitmap = Bitmap.createBitmap(differenceMat.cols(),
                differenceMat.rows(), Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(differenceMat, resultBitmap);

        return resultBitmap;
    }
}

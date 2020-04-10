package hkucs.example.sift;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
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
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "OPENCV_DEBUG";

    private final int SELECT_PHOTO_1 = 1;
    private final int SELECT_PHOTO_2 = 2;

    private int ACTION_MODE = 0;

    Mat src1, src2;
    private ImageView ivImage;

    FeatureDetector detector;
    MatOfKeyPoint keypoints1, keypoints2;
    DescriptorExtractor descriptorExtractor;
    Mat descriptors1, descriptors2;
    MatOfDMatch matches;

    int keypointsObject1, keypointsObject2;

    private BaseLoaderCallback mOpenCVCallBack = new BaseLoaderCallback(this) {
        @Override public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    //DO YOUR WORK/STUFF HERE
                    System.loadLibrary("nonfree");
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
        ivImage = findViewById(R.id.ivImage);

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_load_first_image) {
            Intent photoPickerIntent = new
                    Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent,
                    SELECT_PHOTO_1);
            return true;
        }else if(id == R.id.action_load_second_image) {
            Intent photoPickerIntent = new
                    Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent,
                    SELECT_PHOTO_2);
            return true;
        }else if(id == R.id.match_image){
            if(src1 != null && src2 != null){
                new AsyncTask<Void, Void, Bitmap>() {
                    private long startTime, endTime;



                    @Override
                    protected void onPreExecute() {
                        super.onPreExecute();
                        startTime = System.currentTimeMillis();
                    }
                    @Override
                    protected Bitmap doInBackground(Void... params) {
                        return executeTask();
                    }
                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        super.onPostExecute(bitmap);
                        endTime = System.currentTimeMillis();
                        ivImage.setImageBitmap(bitmap);
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
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
                    //resume tasks needing this permission
                }else{

                }
                break;
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //Code to load image into a Bitmap and convert it to a Mat
        if (resultCode == RESULT_OK){
            try {
                final Uri imageUri = data.getData();
                final InputStream imageStream;
                imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage =
                        BitmapFactory.decodeStream(imageStream);

                switch(requestCode) {
                    case SELECT_PHOTO_1:
                        src1 = new Mat(selectedImage.getHeight(),
                                selectedImage.getWidth(), CvType.CV_8UC4);
                        Utils.bitmapToMat(selectedImage, src1);
                        /*
                        switch (ACTION_MODE){
                            //Add different cases here depending on the required operation
                            case HomeActivity.MEAN_BLUR:
                                Imgproc.blur(src, src, new Size(3,3));
                                break;

                            case HomeActivity.GAUSSIAN_BLUR:
                                Imgproc.GaussianBlur(src, src, new Size(3,3), 0);
                                break;

                            case HomeActivity.MEDIAN_BLUR:
                                Imgproc.medianBlur(src, src, 3);
                                break;
                        }


                        //Code to convert Mat to Bitmap to load in an ImageView. Also load original image in imageView
                        Bitmap processedImage = Bitmap.createBitmap(src.cols(),
                                src.rows(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(src, processedImage);
                        ivImage.setImageBitmap(selectedImage);
                        ivImageProcessed.setImageBitmap(processedImage);
                         */
                        break;
                    case SELECT_PHOTO_2:
                        src2 = new Mat(selectedImage.getHeight(),
                                selectedImage.getWidth(), CvType.CV_8UC4);
                        Utils.bitmapToMat(selectedImage, src2);
                        break;

                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        }
    }
    /*
    private void loadImageToImageView()
    {
        ImageView imgView = (ImageView) findViewById(R.id.image_view);
        imgView.setImageBitmap(currentBitmap_1);
    }

     */

    private Bitmap executeTask(){
        Bitmap resultBitmap;
        Mat resultMat;

        keypoints1 = new MatOfKeyPoint();
        keypoints2 = new MatOfKeyPoint();
        descriptors1 = new Mat();
        descriptors2 = new Mat();

        switch (ACTION_MODE){
            case 0:
                detector = FeatureDetector.create(FeatureDetector.SIFT);
                descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.SIFT);
                //Add SIFT specific code
                detector.detect(src2, keypoints2);
                detector.detect(src1, keypoints1);
                keypointsObject1 = keypoints1.toArray().length;
                //These have been added to display the number of keypoints later.
                keypointsObject2 = keypoints2.toArray().length;

                descriptorExtractor.compute(src1, keypoints1, descriptors1);
                descriptorExtractor.compute(src2, keypoints2, descriptors2);

                // Matching features
                DescriptorMatcher descriptorMatcher;
                matches = new MatOfDMatch();
                descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.
                        BRUTEFORCE_SL2);
                descriptorMatcher.match(descriptors1, descriptors2, matches);

                break;
            //Add cases for other algorithms
        }
        resultMat = drawMatches(src1, keypoints1, src2, keypoints2, matches, true);
        resultBitmap = Bitmap.createBitmap(resultMat.cols(),
                resultMat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resultMat, resultBitmap);
        return resultBitmap;
    }

    static Mat drawMatches(Mat img1, MatOfKeyPoint key1, Mat img2,
                           MatOfKeyPoint key2, MatOfDMatch matches, boolean imageOnly){
        Mat out = new Mat();
        Mat im1 = new Mat();
        Mat im2 = new Mat();
        Imgproc.cvtColor(img1, im1, Imgproc.COLOR_BGR2RGB);
        Imgproc.cvtColor(img2, im2, Imgproc.COLOR_BGR2RGB);
        if (imageOnly){
            MatOfDMatch emptyMatch = new MatOfDMatch();
            MatOfKeyPoint emptyKey1 = new MatOfKeyPoint();
            MatOfKeyPoint emptyKey2 = new MatOfKeyPoint();
            Features2d.drawMatches(im1, emptyKey1,
                    im2, emptyKey2, emptyMatch, out);
        } else {
            Features2d.drawMatches(im1, key1,
                    im2, key2, matches, out);
        }
        Bitmap bmp = Bitmap.createBitmap(out.cols(),
                out.rows(), Bitmap.Config.ARGB_8888);
        Imgproc.cvtColor(out, out, Imgproc.COLOR_BGR2RGB);
        Imgproc.putText(out, "FRAME", new Point(img1.width() / 2,30),
                Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0,255,255),3);
        Imgproc.putText(out, "MATCHED", new Point(img1.width() +
                img2.width() / 2,30), Core.FONT_HERSHEY_PLAIN, 2, new
                Scalar(255,0,0),3);
        return out;
    }
}

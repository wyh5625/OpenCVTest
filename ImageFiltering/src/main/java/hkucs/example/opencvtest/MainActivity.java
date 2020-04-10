package hkucs.example.opencvtest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {
    public static final String TAG = "OPENCV_DEBUG";
    private final int SELECT_PHOTO = 1;
    private ImageView ivImage, ivImageProcessed;
    Mat src;
    static int ACTION_MODE = 0;


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

        // Android specific code
        ivImage = (ImageView)findViewById(R.id.ivImage);
        ivImageProcessed =
                (ImageView)findViewById(R.id.ivImageProcessed);
        Intent intent = getIntent();
        if(intent.hasExtra("ACTION_MODE"))
            ACTION_MODE = intent.getIntExtra("ACTION_MODE", 0);

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

        isReadStoragePermissionGranted();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_load_image) {
            Intent photoPickerIntent = new
                    Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            startActivityForResult(photoPickerIntent,
                    SELECT_PHOTO);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case SELECT_PHOTO:
                if(resultCode == RESULT_OK){
                    try {
                        //Code to load image into a Bitmap and convert it to a Mat for processing.
                        final Uri imageUri = data.getData();
                        final InputStream imageStream =
                                getContentResolver().openInputStream(imageUri);
                        final Bitmap selectedImage =
                                BitmapFactory.decodeStream(imageStream);
                        src = new Mat(selectedImage.getHeight(),
                                selectedImage.getWidth(), CvType.CV_8UC4);
                        Utils.bitmapToMat(selectedImage, src);
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
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }
}

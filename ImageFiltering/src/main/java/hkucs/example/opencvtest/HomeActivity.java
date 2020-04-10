package hkucs.example.opencvtest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HomeActivity extends AppCompatActivity {
    public static final int MEAN_BLUR = 1;
    public static final int GAUSSIAN_BLUR = 2;
    public static final int MEDIAN_BLUR = 3;
    Button bMean;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bMean = (Button)findViewById(R.id.bMean);
        bMean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),
                        MainActivity.class);
                i.putExtra("ACTION_MODE", MEAN_BLUR);
                startActivity(i);
            }
        });
    }
}

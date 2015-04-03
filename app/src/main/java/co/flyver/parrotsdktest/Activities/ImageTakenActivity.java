package co.flyver.parrotsdktest.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;

import co.flyver.parrotsdktest.NetworkingManager;
import co.flyver.parrotsdktest.R;

public class ImageTakenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_taken);
        Intent intent = getIntent();
        final String filepath = intent.getStringExtra("filepath");
        Bitmap bitmap = BitmapFactory.decodeFile(filepath);
        ImageView view = (ImageView) findViewById(R.id.imageView2);
        view.setImageBitmap(bitmap);
        ImageButton imageButton = (ImageButton) findViewById(R.id.button_share);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareToFacebook(filepath);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_image_taken, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void shareToFacebook(String filepath) {
        File file = new File(filepath);
        Intent share = new Intent();
        share.setAction(Intent.ACTION_SEND);
        share.putExtra(Intent.EXTRA_TEXT, "Shared via Flyver selfie app");
        share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        share.setType("image/jpeg");
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, "send"));
        ImageTakenActivity.this.finish();
        System.exit(0);
    }
}

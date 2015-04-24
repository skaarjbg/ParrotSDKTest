package co.flyver.parrotsdktest.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.File;
import java.lang.ref.WeakReference;

import co.flyver.parrotsdktest.R;

public class ImageTakenActivity extends Activity {

    private final String TAG = ImageTakenActivity.class.getSimpleName();
    private final String PICTURES_PATH = Environment.getExternalStorageDirectory().getPath().concat("/co.flyver/droneselfie/pictures/");
    private AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_taken);
        dialog = new AlertDialog.Builder(ImageTakenActivity.this).setMessage("Image is being processed, please wait.").show();
        Intent intent = getIntent();
        final String filepath = intent.getStringExtra("filepath");
        Log.d(TAG, "Filepath: ".concat(PICTURES_PATH).concat(filepath));
        ImageView view = (ImageView) findViewById(R.id.imageView2);
        BitmapWorkerTask task = new BitmapWorkerTask(view);
        task.execute(PICTURES_PATH.concat(filepath));
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
//        ImageTakenActivity.this.finish();
//        System.exit(0);
    }

    class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewRef;

        BitmapWorkerTask(ImageView imageView) {
            imageViewRef = new WeakReference<>(imageView);
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            Log.d(TAG, "BITMAP DECODE");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap bitmap = BitmapFactory.decodeFile(params[0]);
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            Log.d(TAG, "POST-EXECUTE!");
            if (bitmap != null) {
                final ImageView imageView = imageViewRef.get();
                if (imageView != null) {
                    imageView.setImageBitmap(bitmap);
                    dialog.dismiss();
                    ImageButton button = (ImageButton) findViewById(R.id.button_share);
                    button.setVisibility(View.VISIBLE);
                }
            }
        }
    }
}

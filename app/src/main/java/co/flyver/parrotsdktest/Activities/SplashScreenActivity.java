package co.flyver.parrotsdktest.Activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import co.flyver.parrotsdktest.Activities.Tutorial.TutorialActivity;
import co.flyver.parrotsdktest.FFMPEGWrapper.FFMPEGManager;
import co.flyver.parrotsdktest.FFMPEGWrapper.FFMPEGWrapper;
import co.flyver.parrotsdktest.R;

public class SplashScreenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                SharedPreferences preferences = getSharedPreferences("tutorialStarted", Context.MODE_PRIVATE);
                if(preferences.getInt("tutorialSeen", 0) == 1) {
//                    FFMPEGManager.init(getApplicationContext());
                    Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                    SplashScreenActivity.this.startActivity(intent);
                    SplashScreenActivity.this.finish();
                } else {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putInt("tutorialSeen", 1);
                    editor.apply();
//                    FFMPEGManager.init(getApplicationContext());
                    Intent intent = new Intent(SplashScreenActivity.this, TutorialActivity.class);
                    SplashScreenActivity.this.startActivity(intent);
                    SplashScreenActivity.this.finish();
                }
            }
        }, 1000);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash_screen, menu);
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
}

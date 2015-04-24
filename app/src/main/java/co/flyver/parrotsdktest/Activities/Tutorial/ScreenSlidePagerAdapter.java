package co.flyver.parrotsdktest.Activities.Tutorial;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.util.Log;

/**
 * Created by Petar Petrov on 4/6/15.
 */
public class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

    private static final int NUM_PAGES = 3;

    public ScreenSlidePagerAdapter(android.support.v4.app.FragmentManager fragmentManager) {
        super(fragmentManager);
    }

    @Override
    public Fragment getItem(int position) {
        switch(position) {
            case 0: {
                return FirstFragment.newInstance();
            }
            case 1: {
                return SecondFragment.newInstance();
            }
            case 2: {
                return ThirdFragment.newInstance();
            }
            default: {
                Log.d("tutorial", "DEFAULT");
                return FirstFragment.newInstance();
            }
        }
    }

    @Override
    public int getCount() {
        return NUM_PAGES;
    }
}

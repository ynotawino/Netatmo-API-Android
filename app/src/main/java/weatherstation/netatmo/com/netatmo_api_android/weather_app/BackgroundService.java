package weatherstation.netatmo.com.netatmo_api_android.weather_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import weatherstation.netatmo.com.netatmo_api_android.api.NetatmoUtils;
import weatherstation.netatmo.com.netatmo_api_android.api.model.Measures;

public class BackgroundService extends JobService {
    private SampleHttpClient sampleHttpClient;
    private MyTaskLoader myTaskLoader;
    private DatabaseReference reference=FirebaseDatabase.getInstance().getReference();

    @Override
    public void onCreate() {
        super.onCreate();
        sampleHttpClient=new SampleHttpClient(this);
        myTaskLoader=new MyTaskLoader(this, 0);

    }

    @Override
    public boolean onStartJob(final JobParameters job) {
        if(sampleHttpClient.getAccessToken() != null) {
            //if the user is already logged
            Log.e("Expires at", PreferenceManager.getDefaultSharedPreferences(this).getLong(NetatmoUtils.KEY_EXPIRES_AT, 0) + "");
            Log.e("Current time", System.currentTimeMillis() + "");
            Runnable getData=new Runnable() {
                @Override
                public void run() {
                    addDbData();
                    jobFinished(job, true);
                }
            };

            Runnable refresh=new Runnable() {
                @Override
                public void run() {
                    sampleHttpClient.refresh();
                    addDbData();
                    jobFinished(job, true);
                }
            };

            if (PreferenceManager.getDefaultSharedPreferences(this).getLong(NetatmoUtils.KEY_EXPIRES_AT, 0) > System.currentTimeMillis()) {
                new Thread(getData).start();

            }
            else {
                new Thread(refresh).start();
            }
        }
        return false;
    }

    private void addDbData() {
        List<Measures> measures=sampleHttpClient.getMeasures();
        Calendar calendar=Calendar.getInstance();
        String day="Sunday";
        switch (calendar.get(Calendar.DAY_OF_WEEK)){
            case Calendar.SUNDAY:
                day="Sunday";
                break;
            case Calendar.MONDAY:
                day="Monday";
                break;
            case Calendar.TUESDAY:
                day="Tuesday";
                break;
            case Calendar.WEDNESDAY:
                day="Wednesday";
                break;
            case Calendar.THURSDAY:
                day="Thursday";
                break;
            case Calendar.FRIDAY:
                day="Friday";
                break;
            case Calendar.SATURDAY:
                day="Saturday";
                break;
            default:
        }
        final int hour=calendar.get(Calendar.HOUR_OF_DAY);

        for (final Measures measure: measures){

            reference.child(measure.getStationId()).child(day).child("Hour "+hour).setValue(measure);

        }
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }


}

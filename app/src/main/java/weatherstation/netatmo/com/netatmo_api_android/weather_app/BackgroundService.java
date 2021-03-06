package weatherstation.netatmo.com.netatmo_api_android.weather_app;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.List;

import weatherstation.netatmo.com.netatmo_api_android.api.NetatmoUtils;
import weatherstation.netatmo.com.netatmo_api_android.api.model.Measures;

//This class is for sending station data to the database in the background
public class BackgroundService extends Worker {
    private SampleHttpClient sampleHttpClient;
    private DatabaseReference reference = FirebaseDatabase.getInstance().getReference();

    public BackgroundService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        sampleHttpClient = new SampleHttpClient(context);
    }

    //Get data from Netatmo and add it to the db
    private void addDbData() {
        List<Measures> measures = sampleHttpClient.getMeasures();
        Calendar calendar = Calendar.getInstance();
        String day = "Sunday";
        switch (calendar.get(Calendar.DAY_OF_WEEK)) {
            case Calendar.SUNDAY:
                day = "Sunday";
                break;
            case Calendar.MONDAY:
                day = "Monday";
                break;
            case Calendar.TUESDAY:
                day = "Tuesday";
                break;
            case Calendar.WEDNESDAY:
                day = "Wednesday";
                break;
            case Calendar.THURSDAY:
                day = "Thursday";
                break;
            case Calendar.FRIDAY:
                day = "Friday";
                break;
            case Calendar.SATURDAY:
                day = "Saturday";
                break;
            default:
        }
        final int hour = calendar.get(Calendar.HOUR_OF_DAY);

        for (final Measures measure : measures) {

            reference.child(measure.getStationId()).child(day).child("Hour " + hour).setValue(measure);

        }
    }

    @NonNull
    @Override
    public Result doWork() {
        //if the user is already logged
        if (sampleHttpClient.getAccessToken() != null) {
            Runnable getData = new Runnable() {
                @Override
                public void run() {
                    addDbData();
                }
            };

            Runnable refresh = new Runnable() {
                @Override
                public void run() {
                    sampleHttpClient.refresh();
                    addDbData();
                }
            };

            if (PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getLong(NetatmoUtils.KEY_EXPIRES_AT, 0) > System.currentTimeMillis()) {
                //If the access token has not expired, get data from Netatmo and add it to the database
                new Thread(getData).start();
            }
            else {
                //If the access token has expired, get the access token first then get Netatmo data and add it to the database
                new Thread(refresh).start();
            }
        }
        return Result.success();
    }
}

package weatherstation.netatmo.com.netatmo_api_android.weather_app;

import android.content.Context;

import androidx.loader.content.AsyncTaskLoader;

import java.util.List;

import weatherstation.netatmo.com.netatmo_api_android.api.model.Measures;

//Load data in the background
public class MyTaskLoader extends AsyncTaskLoader<List<Measures>> {

    private int caller;
    private SampleHttpClient sampleHttpClient;

    public MyTaskLoader(Context context, int caller) {
        super(context);
        this.caller = caller;
        sampleHttpClient = new SampleHttpClient(context);
    }

    @Override
    public List<Measures> loadInBackground() {
        //If the caller was 1, refresh the access token
        if (caller == 1) {
            sampleHttpClient.refresh();
            return null;
        }
        //Otherwise, fetch data from the database
        else {
            return sampleHttpClient.getMeasures();
        }
    }
}

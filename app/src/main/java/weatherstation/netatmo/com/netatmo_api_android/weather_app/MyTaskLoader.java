package weatherstation.netatmo.com.netatmo_api_android.weather_app;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;
import android.view.View;

import java.util.List;

import weatherstation.netatmo.com.netatmo_api_android.api.model.Measures;

public class MyTaskLoader extends android.support.v4.content.AsyncTaskLoader<List<Measures>> {

    private int caller;
    private SampleHttpClient sampleHttpClient;
    public MyTaskLoader(Context context, int caller) {
        super(context);
        this.caller=caller;
        sampleHttpClient=new SampleHttpClient(context);
    }

    @Override
    public List<Measures> loadInBackground() {
        if (caller==1) {
            sampleHttpClient.refresh();
            return null;
        }
        else {
            return sampleHttpClient.getMeasures();
        }
    }
}

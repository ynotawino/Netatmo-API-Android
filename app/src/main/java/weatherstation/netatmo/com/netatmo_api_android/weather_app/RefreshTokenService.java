package weatherstation.netatmo.com.netatmo_api_android.weather_app;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class RefreshTokenService extends Worker {
    public RefreshTokenService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SampleHttpClient sampleHttpClient = new SampleHttpClient(getApplicationContext());
        sampleHttpClient.refresh();
        return Result.success();
    }
}

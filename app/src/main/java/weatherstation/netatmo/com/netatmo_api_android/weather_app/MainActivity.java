package weatherstation.netatmo.com.netatmo_api_android.weather_app;

import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v7.app.AppCompatActivity;
import android.util.ArraySet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.Trigger;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import weatherstation.netatmo.com.netatmo_api_android.R;
import weatherstation.netatmo.com.netatmo_api_android.api.NetatmoUtils;
import weatherstation.netatmo.com.netatmo_api_android.api.model.Measures;


public class MainActivity extends AppCompatActivity implements android.support.v4.app.LoaderManager.LoaderCallbacks<List<Measures>> {

    public static final String TAG = "MainActivity";
    private MyAdapter mAdapter;
    private List<Measures> mListItems = new ArrayList<Measures>();

    private ProgressBar progressBar;
    private TextView emptyView, textLocation;
    private ListView listView;
    private Button btnLocation, btnRefresh;

    public static String lat_sw="-4.562415";
    public static String lon_sw="35.438138";
    public static String lat_ne="4.323179";
    public static String lon_ne="41.821195";
    private SampleHttpClient sampleHttpClient;
    private FirebaseJobDispatcher dispatcher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sampleHttpClient = new SampleHttpClient(this);
        btnLocation= (Button) findViewById(R.id.button_location);
        btnRefresh= (Button) findViewById(R.id.button_refresh);
        textLocation= (TextView) findViewById(R.id.text_location_selected);
        progressBar= (ProgressBar) findViewById(R.id.progressBar_main);
        emptyView= (TextView) findViewById(R.id.text_empty_view);
        mAdapter = new MyAdapter(this, mListItems);
        listView = (ListView)findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        listView.setEmptyView(emptyView);
        dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        Job job= dispatcher.newJobBuilder().setService(BackgroundService.class).setConstraints(Constraint.ON_ANY_NETWORK).setLifetime(Lifetime.FOREVER).setRecurring(true).setTrigger(Trigger.executionWindow(0, 1200)).setTag("Fetch data always").build();
        dispatcher.mustSchedule(job);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent=new Intent(MainActivity.this, WeatherActivity.class);
                Measures measures=(Measures)parent.getAdapter().getItem(position);
                intent.putExtra("stationId", measures.getStationId());
                startActivity(intent);
            }
        });
        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePicker.IntentBuilder intentBuilder=new PlacePicker.IntentBuilder();
                try {
                    startActivityForResult(intentBuilder.build(MainActivity.this), 10);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Kindly download Google Play Services",Toast.LENGTH_LONG).show();
                }
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initActionBar();
            }
        });

        if(sampleHttpClient.getAccessToken() != null){
            //if the user is already logged
            Log.e("Expires at", PreferenceManager.getDefaultSharedPreferences(this).getLong(NetatmoUtils.KEY_EXPIRES_AT, 0)+"");
            Log.e("Current time", System.currentTimeMillis()+"");
            if (PreferenceManager.getDefaultSharedPreferences(this).getLong(NetatmoUtils.KEY_EXPIRES_AT, 0)>System.currentTimeMillis()) {
                initActionBar();
            }
            else {
                LoaderManager.getInstance(this).initLoader(1, null, this).forceLoad();
            }
        }else{
            //else, starts the LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent,0);
        }



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * "Disconnects" the user by clearing stored tokens. Then, starts the LoginActivity.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_sign_out) {
            sampleHttpClient.clearTokens();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent,0);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Job job= dispatcher.newJobBuilder().setService(BackgroundService.class).setConstraints(Constraint.ON_ANY_NETWORK).setLifetime(Lifetime.UNTIL_NEXT_BOOT).setReplaceCurrent(true).setRecurring(false).setTrigger(Trigger.executionWindow(0, 0)).setTag("Fetch location data").build();
        dispatcher.mustSchedule(job);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode!=10) {
            if (resultCode == RESULT_CANCELED) {
                finish();
            } else if (resultCode == RESULT_OK) {
                initActionBar();
            }
        }
        else {
            if (resultCode==RESULT_OK){
                Job job= dispatcher.newJobBuilder().setService(BackgroundService.class).setConstraints(Constraint.ON_ANY_NETWORK).setLifetime(Lifetime.UNTIL_NEXT_BOOT).setReplaceCurrent(true).setRecurring(false).setTrigger(Trigger.executionWindow(0, 0)).setTag("Fetch location data").build();
                dispatcher.mustSchedule(job);
                Place place=PlacePicker.getPlace(this, data);
                textLocation.setText(place.getName());
                LatLng coordinates=place.getLatLng();
                LatLngBounds coordinateBounds=place.getViewport();
                if (coordinateBounds!=null){
                    lat_sw=coordinateBounds.southwest.latitude+"";
                    lon_sw=coordinateBounds.southwest.longitude+"";
                    lat_ne=coordinateBounds.northeast.latitude+"";
                    lon_ne=coordinateBounds.northeast.longitude+"";
                }
                else {
                    lat_sw=coordinates.latitude+"";
                    lon_sw=coordinates.longitude+"";
                    lat_ne=(coordinates.latitude+1)+"";
                    lon_ne=(coordinates.longitude+1)+"";
                }
                initActionBar();
            }

        }

    }


    private void initActionBar(){

        if (LoaderManager.getInstance(this).getLoader(0)==null)
            LoaderManager.getInstance(this).initLoader(0, null, this).forceLoad();
        else
            LoaderManager.getInstance(this).restartLoader(0, null, this).forceLoad();


    }



    @NonNull
    @Override
    public android.support.v4.content.Loader<List<Measures>> onCreateLoader(int id, Bundle args) {
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        if (id==0) {
            return new MyTaskLoader(this, 0);
        }
        else {
            return new MyTaskLoader(this, 1);
        }
    }

    @Override
    public void onLoadFinished(android.support.v4.content.Loader<List<Measures>> loader, List<Measures> data) {
        if (loader.getId()==1) {
            initActionBar();
            initActionBar();
        }
        else {
            listView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            mAdapter.clear();
            mAdapter.addAll(data);
            emptyView.setText("No items found");

        }

    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<List<Measures>> loader) {
        loader.reset();
    }


}

package weatherstation.netatmo.com.netatmo_api_android.weather_app;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v7.app.AppCompatActivity;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import weatherstation.netatmo.com.netatmo_api_android.R;
import weatherstation.netatmo.com.netatmo_api_android.api.NetatmoUtils;
import weatherstation.netatmo.com.netatmo_api_android.api.model.Measures;


public class MainActivity extends AppCompatActivity implements android.support.v4.app.LoaderManager.LoaderCallbacks<List<Measures>> {

    public static String lat_sw = "-4.562415";
    public static String lon_sw = "35.438138";
    public static String lat_ne = "4.323179";
    public static String lon_ne = "41.821195";
    private MyAdapter mAdapter;
    private ProgressBar progressBar;
    private TextView emptyView, textLocation;
    private ListView listView;
    private Button btnLocation, btnRefresh;
    private SampleHttpClient sampleHttpClient;
    private FirebaseJobDispatcher dispatcher;
    private Set<Measures> locations;
    private List<Measures> stations;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sampleHttpClient = new SampleHttpClient(this);
        btnLocation = (Button) findViewById(R.id.button_location);
        btnRefresh = (Button) findViewById(R.id.button_refresh);
        textLocation = (TextView) findViewById(R.id.text_location_selected);
        progressBar = (ProgressBar) findViewById(R.id.progressBar_main);
        emptyView = (TextView) findViewById(R.id.text_empty_view);
        stations = new ArrayList<>();
        locations = new HashSet<>();
        mAdapter = new MyAdapter(this, stations);
        listView = findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        listView.setEmptyView(emptyView);
        dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        Job job = dispatcher.newJobBuilder().setService(BackgroundService.class).setConstraints(Constraint.ON_ANY_NETWORK).setLifetime(Lifetime.FOREVER).setRecurring(true).setTrigger(Trigger.executionWindow(0, 1200)).setTag("Fetch data always").build();
        dispatcher.mustSchedule(job);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, WeatherActivity.class);
                Measures measures = (Measures) parent.getAdapter().getItem(position);
                if (measures.getStationId() != null && !measures.getStationId().equals("")) {
                    intent.putExtra("stationId", measures.getStationId());
                }
                intent.putExtra("data", measures);
                startActivity(intent);
            }
        });
        btnLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlacePicker.IntentBuilder intentBuilder = new PlacePicker.IntentBuilder();
                try {
                    startActivityForResult(intentBuilder.build(MainActivity.this), 10);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Kindly download Google Play Services", Toast.LENGTH_LONG).show();
                }
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initActionBar();
            }
        });

        if (sampleHttpClient.getAccessToken() != null) {
            //if the user is already logged
            Log.e("Expires at", PreferenceManager.getDefaultSharedPreferences(this).getLong(NetatmoUtils.KEY_EXPIRES_AT, 0) + "");
            Log.e("Current time", System.currentTimeMillis() + "");
            if (PreferenceManager.getDefaultSharedPreferences(this).getLong(NetatmoUtils.KEY_EXPIRES_AT, 0) > System.currentTimeMillis()) {
                initActionBar();
            } else {
                LoaderManager.getInstance(this).initLoader(1, null, this).forceLoad();
            }
        } else {
            //else, starts the LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, 0);
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
            startActivityForResult(intent, 0);
            return true;
        } else if (id == R.id.action_main_location) {
            if (item.getTitle().toString().equals("View Locations")) {
                mAdapter.clear();
                mAdapter.addAll(locations);
                item.setTitle("View Stations");
            } else {
                mAdapter.clear();
                mAdapter.addAll(stations);
                item.setTitle("View Locations");
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Job job = dispatcher.newJobBuilder().setService(BackgroundService.class).setConstraints(Constraint.ON_ANY_NETWORK).setLifetime(Lifetime.UNTIL_NEXT_BOOT).setReplaceCurrent(true).setRecurring(false).setTrigger(Trigger.executionWindow(0, 0)).setTag("Fetch location data").build();
        dispatcher.mustSchedule(job);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 10) {
            if (resultCode == RESULT_CANCELED) {
                finish();
            } else if (resultCode == RESULT_OK) {
                initActionBar();
            }
        } else {
            if (resultCode == RESULT_OK) {
                Job job = dispatcher.newJobBuilder().setService(BackgroundService.class).setConstraints(Constraint.ON_ANY_NETWORK).setLifetime(Lifetime.UNTIL_NEXT_BOOT).setReplaceCurrent(true).setRecurring(false).setTrigger(Trigger.executionWindow(0, 0)).setTag("Fetch location data").build();
                dispatcher.mustSchedule(job);
                Place place = PlacePicker.getPlace(this, data);
                textLocation.setText(place.getName());
                LatLng coordinates = place.getLatLng();
                LatLngBounds coordinateBounds = place.getViewport();
                if (coordinateBounds != null) {
                    lat_sw = coordinateBounds.southwest.latitude + "";
                    lon_sw = coordinateBounds.southwest.longitude + "";
                    lat_ne = coordinateBounds.northeast.latitude + "";
                    lon_ne = coordinateBounds.northeast.longitude + "";
                } else {
                    lat_sw = coordinates.latitude + "";
                    lon_sw = coordinates.longitude + "";
                    lat_ne = (coordinates.latitude + 1) + "";
                    lon_ne = (coordinates.longitude + 1) + "";
                }
                initActionBar();
            }

        }

    }


    private void initActionBar() {

        if (LoaderManager.getInstance(this).getLoader(0) == null)
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
        if (id == 0) {
            return new MyTaskLoader(this, 0);
        } else {
            return new MyTaskLoader(this, 1);
        }
    }

    @Override
    public void onLoadFinished(@NonNull android.support.v4.content.Loader<List<Measures>> loader, List<Measures> data) {
        if (loader.getId() == 1) {
            initActionBar();
        } else {
            listView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            if (!stations.isEmpty())
                stations.clear();
            stations = data;
            mAdapter.clear();
            mAdapter.addAll(stations);
            emptyView.setText("No items found");

            if (!locations.isEmpty())
                locations.clear();
            Set<Measures> tempLocations = new HashSet<>(stations);

            for (Measures location : tempLocations) {
                int stationsHumidity, stationsGustStrength, stationsGustAngle, stationsNoise, stationsPressure, stationsRain, stationsRainHour, stationsRainDay, stationsWindStrength, stationsWindAngle, stationsTemp;
                stationsHumidity = 0;
                stationsGustStrength = 0;
                stationsGustAngle = 0;
                stationsNoise = 0;
                stationsPressure = 0;
                stationsRain = 0;
                stationsRainHour = 0;
                stationsRainDay = 0;
                stationsWindStrength = 0;
                stationsWindAngle = 0;
                stationsTemp = 0;
                float valueHumidity, valueGustStrength, valueGustAngle, valueNoise, valuePressure, valueRain, valueRainHour, valueRainDay, valueWindStrength, valueWindAngle, valueTemp;
                valueHumidity = 0;
                valueGustStrength = 0;
                valueGustAngle = 0;
                valueNoise = 0;
                valuePressure = 0;
                valueRain = 0;
                valueRainHour = 0;
                valueRainDay = 0;
                valueWindStrength = 0;
                valueWindAngle = 0;
                valueTemp = 0;
                for (Measures station : data) {
                    if (station.getLocation().equals(location.getLocation())) {
                        if (!station.getHumidity().equals(Measures.STRING_NO_DATA)) {
                            stationsHumidity++;
                            valueHumidity += Float.valueOf(station.getHumidity());
                        }
                        if (!station.getGustStrength().equals(Measures.STRING_NO_DATA)) {
                            stationsGustStrength++;
                            valueGustStrength += Float.valueOf(station.getGustStrength());
                        }
                        if (!station.getGustAngle().equals(Measures.STRING_NO_DATA)) {
                            stationsGustAngle++;
                            valueGustAngle += Float.valueOf(station.getGustAngle());
                        }
                        if (!station.getNoise().equals(Measures.STRING_NO_DATA)) {
                            stationsNoise++;
                            valueNoise += Float.valueOf(station.getNoise());
                        }
                        if (!station.getPressure().equals(Measures.STRING_NO_DATA)) {
                            stationsPressure++;
                            valuePressure += Float.valueOf(station.getPressure());
                        }
                        if (!station.getRain().equals(Measures.STRING_NO_DATA)) {
                            stationsRain++;
                            valueRain += Float.valueOf(station.getRain());
                        }
                        if (!station.getSum_rain_1().equals(Measures.STRING_NO_DATA)) {
                            stationsRainHour++;
                            valueRainHour += Float.valueOf(station.getSum_rain_1());
                        }
                        if (!station.getSum_rain_24().equals(Measures.STRING_NO_DATA)) {
                            stationsRainDay++;
                            valueRainDay += Float.valueOf(station.getSum_rain_24());
                        }
                        if (!station.getWindStrength().equals(Measures.STRING_NO_DATA)) {
                            stationsWindStrength++;
                            valueWindStrength += Float.valueOf(station.getWindStrength());
                        }
                        if (!station.getWindAngle().equals(Measures.STRING_NO_DATA)) {
                            stationsWindAngle++;
                            valueWindAngle += Float.valueOf(station.getWindAngle());
                        }
                        if (!station.getTemperature().equals(Measures.STRING_NO_DATA)) {
                            stationsTemp++;
                            valueTemp += Float.valueOf(station.getTemperature());
                        }

                    }
                }

                DecimalFormat decimalFormat = new DecimalFormat("####.##");
                Measures locationMeasure = new Measures();
                locationMeasure.setLocation(location.getLocation());
                if (stationsHumidity > 0) {
                    locationMeasure.setHumidity(decimalFormat.format(WeatherActivity.getAvg(valueHumidity, stationsHumidity)));
                } else {
                    locationMeasure.setHumidity(Measures.STRING_NO_DATA);
                }
                if (stationsGustStrength > 0) {
                    locationMeasure.setGustStrength(decimalFormat.format((WeatherActivity.getAvg(valueGustStrength, stationsGustStrength))));
                } else {
                    locationMeasure.setGustStrength(Measures.STRING_NO_DATA);
                }
                if (stationsGustAngle > 0) {
                    locationMeasure.setGustAngle(decimalFormat.format(WeatherActivity.getAvg(valueGustAngle, stationsGustAngle)));
                } else {
                    locationMeasure.setGustAngle(Measures.STRING_NO_DATA);
                }
                if (stationsNoise > 0) {
                    locationMeasure.setNoise(decimalFormat.format(WeatherActivity.getAvg(valueNoise, stationsNoise)));
                } else {
                    locationMeasure.setNoise(Measures.STRING_NO_DATA);
                }
                if (stationsPressure > 0) {
                    locationMeasure.setPressure(decimalFormat.format(WeatherActivity.getAvg(valuePressure, stationsPressure)));
                } else {
                    locationMeasure.setPressure(Measures.STRING_NO_DATA);
                }
                if (stationsRain > 0) {
                    locationMeasure.setRain(decimalFormat.format(WeatherActivity.getAvg(valueRain, stationsRain)));
                } else {
                    locationMeasure.setRain(Measures.STRING_NO_DATA);
                }
                if (stationsRainHour > 0) {
                    locationMeasure.setSum_rain_1(decimalFormat.format(WeatherActivity.getAvg(valueRainHour, stationsRainHour)));
                } else {
                    locationMeasure.setSum_rain_1(Measures.STRING_NO_DATA);
                }
                if (stationsRainDay > 0) {
                    locationMeasure.setSum_rain_24(decimalFormat.format(WeatherActivity.getAvg(valueRainDay, stationsRainDay)));
                } else {
                    locationMeasure.setSum_rain_24(Measures.STRING_NO_DATA);
                }
                if (stationsWindStrength > 0) {
                    locationMeasure.setWindStrength(decimalFormat.format(WeatherActivity.getAvg(valueWindStrength, stationsWindStrength)));
                } else {
                    location.setWindStrength(Measures.STRING_NO_DATA);
                }
                if (stationsWindAngle > 0) {
                    locationMeasure.setWindAngle(decimalFormat.format(WeatherActivity.getAvg(valueWindAngle, stationsWindAngle)));
                } else {
                    locationMeasure.setWindAngle(Measures.STRING_NO_DATA);
                }
                if (stationsTemp > 0) {
                    locationMeasure.setTemperature(decimalFormat.format(WeatherActivity.getAvg(valueTemp, stationsTemp)));
                } else {
                    locationMeasure.setTemperature(Measures.STRING_NO_DATA);
                }

                locations.add(locationMeasure);
            }

        }

    }

    @Override
    public void onLoaderReset(android.support.v4.content.Loader<List<Measures>> loader) {
        loader.reset();
    }


}

package weatherstation.netatmo.com.netatmo_api_android.weather_app;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

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
import java.util.concurrent.TimeUnit;

import weatherstation.netatmo.com.netatmo_api_android.R;
import weatherstation.netatmo.com.netatmo_api_android.api.NetatmoUtils;
import weatherstation.netatmo.com.netatmo_api_android.api.model.Measures;

//The main activity contains the main list
public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<Measures>> {

    public static final int REQUESTPLACE = 10;
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
    private Set<Measures> locations;
    private List<Measures> stations;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sampleHttpClient = new SampleHttpClient(this);
        btnLocation =  findViewById(R.id.button_location);
        btnRefresh =  findViewById(R.id.button_refresh);
        textLocation =  findViewById(R.id.text_location_selected);
        progressBar =  findViewById(R.id.progressBar_main);
        emptyView =  findViewById(R.id.text_empty_view);
        stations = new ArrayList<>();
        locations = new HashSet<>();
        mAdapter = new MyAdapter(this, stations);
        listView = findViewById(R.id.list);
        listView.setAdapter(mAdapter);
        listView.setEmptyView(emptyView);

        //Start a new background job fetching data in the background every hour
        androidx.work.Constraints constraints=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest workRequest=new PeriodicWorkRequest.Builder(BackgroundService.class, 1, TimeUnit.HOURS).setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, workRequest);

        //Start a new background job fetching data in the background refreshing access token every hour
        PeriodicWorkRequest tokenRequest=new PeriodicWorkRequest.Builder(RefreshTokenService.class, 1, TimeUnit.HOURS).setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, tokenRequest);

        //Open the descriptive view of the item selected
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, WeatherActivity.class);
                Measures measures = (Measures) parent.getAdapter().getItem(position);
                //If the measure item selected belongs to a station, add stationId
                if (measures.getStationId() != null && !measures.getStationId().contains(",")) {
                    intent.putExtra("stationId", measures.getStationId());
                }
                //If the measure item selected belongs to a location, add stationIds
                else if (measures.getStationId() != null && measures.getStationId().contains(",")) {
                    intent.putExtra("stationIds", measures.getStationId());
                }
                intent.putExtra("data", measures);
                startActivity(intent);
            }
        });

        //if the user is already logged
        if (sampleHttpClient.getAccessToken() != null) {
            LoaderManager.getInstance(this).initLoader(1, null, this).forceLoad();
            //If the access token is still valid, fetch data from Netatmo
            if (PreferenceManager.getDefaultSharedPreferences(this).getLong(NetatmoUtils.KEY_EXPIRES_AT, 0) > System.currentTimeMillis()) {
                initActionBar();
            }
            //If the access token expired, refresh it
            else {
                LoaderManager.getInstance(this).initLoader(1, null, this).forceLoad();
            }
        }
        //else, starts the LoginActivity
        else {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, 0);
        }

        //Select a location to view measures for
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

        //Fetch data again
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getLong(NetatmoUtils.KEY_EXPIRES_AT, 0) > System.currentTimeMillis()) {
                    initActionBar();
                }
                //If the access token expired, refresh it
                else {
                    LoaderManager.getInstance(MainActivity.this).initLoader(1, null, MainActivity.this).forceLoad();
                }
            }
        });


    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu.getItem(1).getTitle().toString().equals("View Stations")){
            menu.getItem(1).setTitle("View Locations");
        }
        return super.onPrepareOptionsMenu(menu);
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
        //If the user signs out, clear their access tokens
        if (id == R.id.action_sign_out) {
            sampleHttpClient.clearTokens();
            Intent intent = new Intent(this, LoginActivity.class);
            startActivityForResult(intent, 0);
            return true;
        }
        //Toggle between viewing stations and locations
        else if (id == R.id.action_main_location) {
            //When the user clicks view locations, view the list of locations
            if (item.getTitle().toString().equals("View Locations")) {
                item.setTitle("View Stations");
                mAdapter.clear();
                mAdapter.addAll(locations);
            }
            //When the user clicks view stations, view a list of stations
            else {
                item.setTitle("View Locations");
                mAdapter.clear();
                mAdapter.addAll(stations);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //If the requestCode is not for getting a place, it is for login in
        if (requestCode != REQUESTPLACE) {
            //If the user did not log in, close the app
            if (resultCode == RESULT_CANCELED) {
                finish();
            }
            //If the user logged in, display a list of measurements
            else if (resultCode == RESULT_OK) {
                initActionBar();
            }
        }
        //If the requestCode is for picking a place
        else {
            if (resultCode == RESULT_OK) {
                //Send data about the stations in the picked place to the database
                androidx.work.Constraints constraints=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
                OneTimeWorkRequest workRequest=new OneTimeWorkRequest.Builder(BackgroundService.class).setConstraints(constraints).build();
                WorkManager.getInstance(this).enqueueUniqueWork("place", ExistingWorkPolicy.REPLACE, workRequest);
                Place place = PlacePicker.getPlace(this, data);
                textLocation.setText(place.getName());
                LatLngBounds coordinateBounds = place.getViewport();
                //If the place picked has coordinateBounds, use them to set latitudes and longitudes
                if (coordinateBounds != null) {
                    lat_sw = coordinateBounds.southwest.latitude + "";
                    lon_sw = coordinateBounds.southwest.longitude + "";
                    lat_ne = coordinateBounds.northeast.latitude + "";
                    lon_ne = coordinateBounds.northeast.longitude + "";
                }
                //If the place does not have coordinate bounds, use its latitude and longitude for southwest bound, and their values plus one as north east
                else {
                    LatLng coordinates = place.getLatLng();
                    lat_sw = coordinates.latitude + "";
                    lon_sw = coordinates.longitude + "";
                    lat_ne = (coordinates.latitude + 1) + "";
                    lon_ne = (coordinates.longitude + 1) + "";
                }
                //Fetch data from Netatmo
                initActionBar();
            }

        }

    }


    private void initActionBar() {

        //If the loader has not been instantiated yet, create it
        if (LoaderManager.getInstance(this).getLoader(0) == null)
            LoaderManager.getInstance(this).initLoader(0, null, this).forceLoad();
        //Otherwise restart the loader
        else
            LoaderManager.getInstance(this).restartLoader(0, null, this).forceLoad();


    }


    @NonNull
    @Override
    public Loader<List<Measures>> onCreateLoader(int id, Bundle args) {
        progressBar.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        //Start the loader to fetch data
        if (id == 0) {
            return new MyTaskLoader(this, 0);
        }
        //Start the loader to refresh tokens
        else {
            return new MyTaskLoader(this, 1);
        }
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<Measures>> loader, List<Measures> data) {
        //If the loader was refreshing tokens, fetch data
        if (loader.getId() == 1) {
            initActionBar();
        }
        //Display the fetched data
        else {
            invalidateOptionsMenu();
            listView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            //Empty the stations list
            if (!stations.isEmpty())
                stations.clear();
            //Add stations to the stations list
            stations = data;
            //Display the stations
            mAdapter.clear();
            mAdapter.addAll(stations);
            emptyView.setText("No items found");

            //Empty the locations list
            if (!locations.isEmpty())
                locations.clear();
            //Store one station per location in a temporary set
            Set<Measures> tempLocations = new HashSet<>(stations);

            //Iterate over every available location
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
                StringBuilder stationIds = new StringBuilder();
                //Iterate over all fetched stations
                for (Measures station : data) {
                    //If a station belongs to the location
                    if (station.getLocation().equals(location.getLocation())) {
                        //Add the stationId
                        stationIds.append(station.getStationId()).append(",");
                        //If there is data, increase the count of the element by 1, and add the value to the running total
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
                //Set the stationId to the concatenated stationIds
                locationMeasure.setStationId(stationIds.toString());
                //Set the location to the location of the station
                locationMeasure.setLocation(location.getLocation());
                //If one or more stations in a location measured an element, set the element to the average obtained by dividing the running total of the
                //element by the number of stations that measure the element. Otherwise set the element to "No data"
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

                //Add the measure to the locations list
                locations.add(locationMeasure);
            }

        }

    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<Measures>> loader) {
        loader.reset();
    }


}

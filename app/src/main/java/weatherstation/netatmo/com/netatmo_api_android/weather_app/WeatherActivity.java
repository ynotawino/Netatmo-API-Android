package weatherstation.netatmo.com.netatmo_api_android.weather_app;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;

import java.util.Calendar;
import java.util.HashMap;

import weatherstation.netatmo.com.netatmo_api_android.R;
import weatherstation.netatmo.com.netatmo_api_android.api.model.Measures;

//Detail view of station or location data
public class WeatherActivity extends AppCompatActivity {

    private GraphView graphView;
    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private String stationId;
    private DataPoint[] dataPointsTemp, dataPointsHumidity, dataPointsGust, dataPointsNoise, dataPointsPressure, dataPointsRain, dataPointsRainHour, dataPointsRainDay, dataPointsWind;
    private ProgressBar progressBar;
    private float maxTemp = -500;
    private float minTemp = 100;
    private float maxHumidity = 0;
    private float minHumidity = 100;
    private float maxNoise = -200;
    private float minNoise = 200;
    private float maxPressure = 100;
    private float minPressure = 2000;
    private float maxRainHour = 0;
    private float minRainHour = 3000;
    private float maxRainDay = 0;
    private float minRainDay = 3000;
    private float maxRain = 0;
    private float minRain = 3000;
    private float maxWind = -1000;
    private float minWind = 1000;
    private float maxGust = -1000;
    private float minGust = 1000;
    private TextView textViewLocation, textViewHumidity, textViewGustStrength, textViewGustAngle, textViewNoise, textViewPressure, textViewRain, textViewRainHour, textViewRainDay, textViewWindStrength, textViewWindAngle, textViewTemp;
    private Spinner spinner;
    private String elements[];
    private Series<DataPoint> seriesHumidity, seriesGust, seriesNoise, seriesPressure, seriesRain, seriesRainHour, seriesRainDay, seriesWind, seriesTemp;
    private ScrollView scrollView;

    public static float getAvg(float total, int count) {
        return total / count;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        graphView = findViewById(R.id.weather_graph);
        progressBar = findViewById(R.id.progressBar_weather);
        textViewLocation = findViewById(R.id.textView_weather_location);
        textViewHumidity = findViewById(R.id.textView_weather_humidity);
        textViewGustStrength = findViewById(R.id.textView_weather_gust_strength);
        textViewGustAngle = findViewById(R.id.textView_weather_gust_angle);
        textViewNoise = findViewById(R.id.textView_weather_noise);
        textViewPressure = findViewById(R.id.textView_weather_pressure);
        textViewRain = findViewById(R.id.textView_weather_rain_live);
        textViewRainHour = findViewById(R.id.textView_weather_rain_hour);
        textViewRainDay = findViewById(R.id.textView_weather_rain_day);
        textViewWindStrength = findViewById(R.id.textView_weather_wind_strength);
        textViewWindAngle = findViewById(R.id.textView_weather_wind_angle);
        textViewTemp = findViewById(R.id.textView_weather_temperature);
        spinner = findViewById(R.id.spinner_weather);
        scrollView = findViewById(R.id.scrollView_weather);
        elements = getResources().getStringArray(R.array.menu_humidity);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                //Refresh graph when an item is selected
                refreshGraph();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        stationId = getIntent().getStringExtra("stationId");

        dataPointsTemp = new DataPoint[7];
        dataPointsHumidity = new DataPoint[7];
        dataPointsGust = new DataPoint[7];
        dataPointsNoise = new DataPoint[7];
        dataPointsPressure = new DataPoint[7];
        dataPointsRain = new DataPoint[7];
        dataPointsRainHour = new DataPoint[7];
        dataPointsRainDay = new DataPoint[7];
        dataPointsWind = new DataPoint[7];

    }

    @Override
    protected void onStart() {
        super.onStart();

        graphView.setVisibility(View.GONE);
        spinner.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        //If a station was selected
        if (stationId != null) {
            //Get data from the database using the stationId
            databaseReference.child(stationId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    //Iterate over the days in the db
                    for (DataSnapshot days : dataSnapshot.getChildren()) {
                        //Get averages of the days and set the data points for each day
                        HashMap<String, Float> values = getStationDayValues(days);
                        setDataPoints(values);
                    }
                    //Draw the graph
                    drawGraph();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }

            });
        }
        //If a location was selected
        else {
            final String[] stationIds = getIntent().getStringExtra("stationIds").split(",");
            databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    HashMap<String, Float> values = new HashMap<>();
                    DataPoint[] temp, humidity, gust, noise, pressure, rain, rainHour, rainDay, wind;
                    int countTemp, countHumidity, countGust, countNoise, countPressure, countRain, countRainHour, countRainDay, countWind;
                    countTemp = 0;
                    countHumidity = 0;
                    countGust = 0;
                    countNoise = 0;
                    countPressure = 0;
                    countRain = 0;
                    countRainHour = 0;
                    countRainDay = 0;
                    countWind = 0;
                    //The datapoints keep a running total per day for the measures
                    temp = new DataPoint[]{new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0)};
                    humidity = new DataPoint[]{new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0)};
                    gust = new DataPoint[]{new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0)};
                    noise = new DataPoint[]{new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0)};
                    pressure = new DataPoint[]{new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0)};
                    rain = new DataPoint[]{new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0)};
                    rainHour = new DataPoint[]{new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0)};
                    rainDay = new DataPoint[]{new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0)};
                    wind = new DataPoint[]{new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0), new DataPoint(0, 0)};
                    //Iterate over all stationIds in the location
                    for (String id : stationIds) {
                        if (!TextUtils.isEmpty(id)) {
                            DataSnapshot station = dataSnapshot.child(id);
                            int dayCountTemp, dayCountHumidity, dayCountGust, dayCountNoise, dayCountPressure, dayCountRain, dayCountRainHour, dayCountRainDay, dayCountWind;
                            dayCountTemp = 0;
                            dayCountHumidity = 0;
                            dayCountGust = 0;
                            dayCountNoise = 0;
                            dayCountPressure = 0;
                            dayCountRain = 0;
                            dayCountRainHour = 0;
                            dayCountRainDay = 0;
                            dayCountWind = 0;
                            //Iterate over all days of the station
                            for (DataSnapshot days : station.getChildren()) {
                                //Get the station values for the day
                                HashMap<String, Float> valueStationDay = getStationDayValues(days);
                                int key = valueStationDay.get("day").intValue();
                                //If an element's value was recorded at least once in a day, increase the day's running total of the element by the day's average. Then increase the
                                //element's day count by 1
                                if (valueStationDay.get("countTemp").intValue() > 0) {
                                    temp[key] = new DataPoint(key, temp[key] == null ? 0 : temp[key].getY() + getAvg(valueStationDay.get("totalTemp"), valueStationDay.get("countTemp").intValue()));
                                    dayCountTemp++;
                                }

                                if (valueStationDay.get("countHumidity").intValue() > 0) {
                                    humidity[key] = new DataPoint(key, humidity[key] == null ? 0 : humidity[key].getY() + getAvg(valueStationDay.get("totalHumidity"), valueStationDay.get("countHumidity").intValue()));
                                    dayCountHumidity++;
                                }
                                if (valueStationDay.get("countGust").intValue() > 0) {
                                    gust[key] = new DataPoint(key, gust[key] == null ? 0 : gust[key].getY() + getAvg(valueStationDay.get("totalGust"), valueStationDay.get("countGust").intValue()));
                                    dayCountGust++;
                                }
                                if (valueStationDay.get("countNoise").intValue() > 0) {
                                    noise[key] = new DataPoint(key, noise[key] == null ? 0 : noise[key].getY() + getAvg(valueStationDay.get("totalNoise"), valueStationDay.get("countNoise").intValue()));
                                    dayCountNoise++;
                                }
                                if (valueStationDay.get("countPressure").intValue() > 0) {
                                    pressure[key] = new DataPoint(key, pressure[key] == null ? 700 : pressure[key].getY() + getAvg(valueStationDay.get("totalPressure"), valueStationDay.get("countPressure").intValue()));
                                    dayCountPressure++;
                                }
                                if (valueStationDay.get("countRain").intValue() > 0) {
                                    rain[key] = new DataPoint(key, rain[key] == null ? 0 : rain[key].getY() + getAvg(valueStationDay.get("totalRain"), valueStationDay.get("countRain").intValue()));
                                    dayCountRain++;
                                }
                                if (valueStationDay.get("countRainHour").intValue() > 0) {
                                    rainHour[key] = new DataPoint(key, rainHour[key] == null ? 0 : rainHour[key].getY() + getAvg(valueStationDay.get("totalRainHour"), valueStationDay.get("countRainHour").intValue()));
                                    dayCountRainHour++;
                                }
                                if (valueStationDay.get("countRainDay").intValue() > 0) {
                                    rainDay[key] = new DataPoint(key, rainDay[key] == null ? 0 : rainDay[key].getY() + getAvg(valueStationDay.get("totalRainDay"), valueStationDay.get("countRainDay").intValue()));
                                    dayCountRainDay++;
                                }
                                if (valueStationDay.get("countWind").intValue() > 0) {
                                    wind[key] = new DataPoint(key, wind[key] == null ? 0 : wind[key].getY() + getAvg(valueStationDay.get("totalWind"), valueStationDay.get("countWind").intValue()));
                                    dayCountWind++;
                                }

                            }

                            //If an element's day count is at least 1, increase the element's count by 1
                            if (dayCountTemp > 0) {
                                countTemp++;
                            }
                            if (dayCountHumidity > 0) {
                                countHumidity++;
                            }
                            if (dayCountGust > 0) {
                                countGust++;
                            }
                            if (dayCountNoise > 0) {
                                countNoise++;
                            }
                            if (dayCountPressure > 0) {
                                countPressure++;
                            }
                            if (dayCountRain > 0) {
                                countRain++;
                            }
                            if (dayCountRainHour > 0) {
                                countRainHour++;
                            }
                            if (dayCountRainDay > 0) {
                                countRainDay++;
                            }
                            if (dayCountWind > 0) {
                                countWind++;
                            }

                        }
                    }

                    //Iterate 7 times for 7 days
                    for (int x = 0; x < 7; x++) {
                        if (temp[x] != null) {
                            //If the temperature for a day has a value, set the elements' totals for that day by dividing the running total by the number of stations recording the element
                            values.put("totalTemp", temp[x] == null ? 0 : (float) (temp[x].getY()));
                            values.put("countTemp", (float) countTemp);
                            values.put("totalHumidity", humidity[x] == null ? 0 : (float) humidity[x].getY());
                            values.put("countHumidity", (float) countHumidity);
                            values.put("totalGust", gust[x] == null ? 0 : (float) (gust[x].getY() / countGust));
                            values.put("countGust", (float) countGust);
                            values.put("totalNoise", noise[x] == null ? 0 : (float) noise[x].getY());
                            values.put("countNoise", (float) countNoise);
                            values.put("totalPressure", pressure[x] == null ? 0 : (pressure[x].getY() < 800 ? 800 : (float) pressure[x].getY()));
                            values.put("countPressure", (float) countPressure);
                            values.put("totalRain", rain[x] == null ? 0 : (float) rain[x].getY());
                            values.put("countRain", (float) countRain);
                            values.put("totalRainHour", rainHour == null ? 0 : (float) rainHour[x].getY());
                            values.put("countRainHour", (float) countRainHour);
                            values.put("totalRainDay", rainDay == null ? 0 : (float) rainDay[x].getY());
                            values.put("countRainDay", (float) countRainDay);
                            values.put("totalWind", wind[x] == null ? 0 : (float) wind[x].getY());
                            values.put("countWind", (float) countWind);
                            values.put("day", (float) x);
                            setDataPoints(values);
                        }
                    }
                    //Draw the graph with the set datapoints
                    drawGraph();

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }

        //Set the day's values of elements in the textviews
        Measures measures = getIntent().getParcelableExtra("data");
        textViewLocation.setText(measures.getLocation());
        if (measures.getHumidity() != null) {
            textViewHumidity.setText(measures.getHumidity().equals(Measures.STRING_NO_DATA) ? measures.getHumidity() : getString(R.string.value_humidity, measures.getHumidity()));
        } else {
            textViewHumidity.setText(Measures.STRING_NO_DATA);
        }
        if (measures.getGustStrength() != null) {
            textViewGustStrength.setText(measures.getGustStrength().equals(Measures.STRING_NO_DATA) ? measures.getGustStrength() : getString(R.string.value_wind, measures.getGustStrength()));
        } else {
            textViewGustStrength.setText(Measures.STRING_NO_DATA);
        }
        if (measures.getGustAngle() != null) {
            textViewGustAngle.setText(measures.getGustAngle().equals(Measures.STRING_NO_DATA) ? measures.getGustAngle() : getString(R.string.value_angle, measures.getGustAngle()));
        } else {
            textViewGustAngle.setText(Measures.STRING_NO_DATA);
        }
        if (measures.getNoise() != null) {
            textViewNoise.setText(measures.getNoise().equals(Measures.STRING_NO_DATA) ? measures.getNoise() : getString(R.string.value_noise, measures.getNoise()));
        } else {
            textViewNoise.setText(Measures.STRING_NO_DATA);
        }
        if (measures.getPressure() != null) {
            textViewPressure.setText(measures.getPressure().equals(Measures.STRING_NO_DATA) ? measures.getPressure() : getString(R.string.value_pressure, measures.getPressure()));
        } else {
            textViewPressure.setText(Measures.STRING_NO_DATA);
        }
        if (measures.getRain() != null) {
            textViewRain.setText(measures.getRain().equals(Measures.STRING_NO_DATA) ? measures.getRain() : getString(R.string.value_rain, measures.getRain()));
        } else {
            textViewRain.setText(Measures.STRING_NO_DATA);
        }
        if (measures.getSum_rain_1() != null) {
            textViewRainHour.setText(measures.getSum_rain_1().equals(Measures.STRING_NO_DATA) ? measures.getSum_rain_1() : getString(R.string.value_rain, measures.getSum_rain_1()));
        } else {
            textViewRainHour.setText(Measures.STRING_NO_DATA);
        }
        if (measures.getSum_rain_24() != null) {
            textViewRainDay.setText(measures.getSum_rain_24().equals(Measures.STRING_NO_DATA) ? measures.getSum_rain_24() : getString(R.string.value_rain, measures.getSum_rain_24()));
        } else {
            textViewRainDay.setText(Measures.STRING_NO_DATA);
        }
        if (measures.getWindStrength() != null) {
            textViewWindStrength.setText(measures.getWindStrength().equals(Measures.STRING_NO_DATA) ? measures.getWindStrength() : getString(R.string.value_wind, measures.getWindStrength()));
        } else {
            textViewWindStrength.setText(Measures.STRING_NO_DATA);
        }
        if (measures.getWindAngle() != null) {
            textViewWindAngle.setText(measures.getWindAngle().equals(Measures.STRING_NO_DATA) ? measures.getWindAngle() : getString(R.string.value_angle, measures.getWindAngle()));
        } else {
            textViewWindAngle.setText(Measures.STRING_NO_DATA);
        }
        if (measures.getTemperature() != null) {
            textViewTemp.setText(measures.getTemperature().equals(Measures.STRING_NO_DATA) ? measures.getTemperature() : getString(R.string.value_temperature, measures.getTemperature()));
        } else {
            textViewTemp.setText(Measures.STRING_NO_DATA);
        }

    }

    //Set the datapoints for each day from the hashmap passed
    private void setDataPoints(HashMap<String, Float> hashMap) {
        int countTemp = hashMap.get("countTemp").intValue();
        float totalTemp = hashMap.get("totalTemp");
        int countHumidity = hashMap.get("countHumidity").intValue();
        float totalHumidity = hashMap.get("totalHumidity");
        int countGust = hashMap.get("countGust").intValue();
        float totalGust = hashMap.get("totalGust");
        int countNoise = hashMap.get("countNoise").intValue();
        float totalNoise = hashMap.get("totalNoise");
        int countPressure = hashMap.get("countPressure").intValue();
        float totalPressure = hashMap.get("totalPressure");
        int countRain = hashMap.get("countRain").intValue();
        float totalRain = hashMap.get("totalRain");
        int countRainHour = hashMap.get("countRainHour").intValue();
        float totalRainHour = hashMap.get("totalRainHour");
        int countRainDay = hashMap.get("countRainDay").intValue();
        float totalRainDay = hashMap.get("totalRainDay");
        int countWind = hashMap.get("countWind").intValue();
        float totalWind = hashMap.get("totalWind");
        int key = hashMap.get("day").intValue();
        //Set the datapoints for each element's day if the element was measured at least once. Then check if the value is the maximum or minimum of the points
        if (countTemp > 0) {
            dataPointsTemp[key] = new DataPoint(key, getAvg(totalTemp, countTemp));
            maxTemp = setMax(getAvg(totalTemp, countTemp), maxTemp);
            minTemp = setMin(getAvg(totalTemp, countTemp), minTemp);
        }
        if (countHumidity > 0) {
            dataPointsHumidity[key] = new DataPoint(key, getAvg(totalHumidity, countHumidity));
            maxHumidity = setMax(getAvg(totalHumidity, countHumidity), maxHumidity);
            minHumidity = setMin(getAvg(totalHumidity, countHumidity), minHumidity);
        }
        if (countGust > 0) {
            dataPointsGust[key] = new DataPoint(key, getAvg(totalGust, countGust));
            maxGust = setMax(getAvg(totalGust, countGust), maxGust);
            minGust = setMin(getAvg(totalGust, countGust), minGust);
        }
        if (countNoise > 0) {
            dataPointsNoise[key] = new DataPoint(key, getAvg(totalNoise, countNoise));
            maxNoise = setMax(getAvg(totalNoise, countNoise), maxNoise);
            minNoise = setMin(getAvg(totalNoise, countNoise), minNoise);
        }
        if (countPressure > 0) {
            dataPointsPressure[key] = new DataPoint(key, getAvg(totalPressure, countPressure));
            maxPressure = setMax(getAvg(totalPressure, countPressure), maxPressure);
            minPressure = setMin(getAvg(totalPressure, countPressure), minPressure);
        }
        if (countRain > 0) {
            dataPointsRain[key] = new DataPoint(key, getAvg(totalRain, countRain));
            maxRain = setMax(getAvg(totalRain, countRain), maxRain);
            minRain = setMin(getAvg(totalRain, countRain), minRain);
        }
        if (countRainHour > 0) {
            dataPointsRainHour[key] = new DataPoint(key, getAvg(totalRainHour, countRainHour));
            maxRainHour = setMax(getAvg(totalRainHour, countRainHour), maxRainHour);
            minRainHour = setMin(getAvg(totalRainHour, countRainHour), minRainHour);
        }
        if (countRainDay > 0) {
            dataPointsRainDay[key] = new DataPoint(key, getAvg(totalRainDay, countRainDay));
            maxRainDay = setMax(getAvg(totalRainDay, countRainDay), maxRainDay);
            minRainDay = setMin(getAvg(totalRainDay, countRainDay), minRainDay);
        }
        if (countWind > 0) {
            dataPointsWind[key] = new DataPoint(key, getAvg(totalWind, countWind));
            maxWind = setMax(getAvg(totalWind, countWind), maxWind);
            minWind = setMin(getAvg(totalWind, countWind), minWind);
        }
    }

    //Get the values recorded by a station in a day and return it as hashmap
    private HashMap<String, Float> getStationDayValues(DataSnapshot days) {
        float totalTemp, totalHumidity, totalGust, totalNoise, totalPressure, totalRain, totalRainHour, totalRainDay, totalWind;
        totalTemp = 0;
        totalHumidity = 0;
        totalGust = 0;
        totalNoise = 0;
        totalPressure = 0;
        totalRain = 0;
        totalRainHour = 0;
        totalRainDay = 0;
        totalWind = 0;
        int countTemp, countHumidity, countGust, countNoise, countPressure, countRain, countRainHour, countRainDay, countWind;
        countTemp = 0;
        countHumidity = 0;
        countGust = 0;
        countNoise = 0;
        countPressure = 0;
        countRain = 0;
        countRainHour = 0;
        countRainDay = 0;
        countWind = 0;

        int key = -1;
        switch (days.getKey()) {
            case "Sunday":
                key = Calendar.SUNDAY - 1;
                break;
            case "Monday":
                key = Calendar.MONDAY - 1;
                break;
            case "Tuesday":
                key = Calendar.TUESDAY - 1;
                break;
            case "Wednesday":
                key = Calendar.WEDNESDAY - 1;
                break;
            case "Thursday":
                key = Calendar.THURSDAY - 1;
                break;
            case "Friday":
                key = Calendar.FRIDAY - 1;
                break;
            case "Saturday":
                key = Calendar.SATURDAY - 1;
                break;
            default:
        }

        //Iterate over the hours of each day
        for (DataSnapshot hours : days.getChildren()) {
            //Get the values recorded every hour
            Measures measure = hours.getValue(Measures.class);
            //If an element's value was stored in the hour, increase the running total by the hour's value. Then increase the count of the element by 1
            if (measure.getTemperature() != null && !measure.getTemperature().equals(Measures.STRING_NO_DATA)) {
                totalTemp += Float.valueOf(measure.getTemperature());
                countTemp++;
            }
            if (measure.getHumidity() != null && !measure.getHumidity().equals(Measures.STRING_NO_DATA)) {
                totalHumidity += Float.valueOf(measure.getHumidity());
                countHumidity++;
            }
            if (measure.getGustStrength() != null && !measure.getGustStrength().equals(Measures.STRING_NO_DATA)) {
                totalGust += Float.valueOf(measure.getGustStrength());
                countGust++;
            }
            if (measure.getNoise() != null && !measure.getNoise().equals(Measures.STRING_NO_DATA)) {
                totalNoise += Float.valueOf(measure.getNoise());
                countNoise++;
            }
            if (measure.getPressure() != null && !measure.getPressure().equals(Measures.STRING_NO_DATA)) {
                totalPressure += Float.valueOf(measure.getPressure());
                countPressure++;
            }
            if (measure.getRain() != null && !measure.getRain().equals(Measures.STRING_NO_DATA)) {
                totalRain += Float.valueOf(measure.getRain());
                countRain++;
            }
            if (measure.getSum_rain_1() != null && !measure.getSum_rain_1().equals(Measures.STRING_NO_DATA)) {
                totalRainHour += Float.valueOf(measure.getSum_rain_1());
                countRainHour++;
            }
            if (measure.getSum_rain_24() != null && !measure.getSum_rain_24().equals(Measures.STRING_NO_DATA)) {
                totalRainDay += Float.valueOf(measure.getSum_rain_24());
                countRainDay++;
            }
            if (measure.getWindStrength() != null && !measure.getWindStrength().equals(Measures.STRING_NO_DATA)) {
                totalWind += Float.valueOf(measure.getWindStrength());
                countWind++;
            }
        }

        //Put the elements' totals and counts in the hashmap
        HashMap<String, Float> values = new HashMap<>();
        values.put("totalTemp", totalTemp);
        values.put("countTemp", (float) countTemp);
        values.put("totalHumidity", totalHumidity);
        values.put("countHumidity", (float) countHumidity);
        values.put("totalGust", totalGust);
        values.put("countGust", (float) countGust);
        values.put("totalNoise", totalNoise);
        values.put("countNoise", (float) countNoise);
        values.put("totalPressure", totalPressure);
        values.put("countPressure", (float) countPressure);
        values.put("totalRain", totalRain);
        values.put("countRain", (float) countRain);
        values.put("totalRainHour", totalRainHour);
        values.put("countRainHour", (float) countRainHour);
        values.put("totalRainDay", totalRainDay);
        values.put("countRainDay", (float) countRainDay);
        values.put("totalWind", totalWind);
        values.put("countWind", (float) countWind);
        values.put("day", (float) key);
        return values;

    }

    //Set the maximum
    private float setMax(float avg, float max) {
        if (avg > max)
            max = avg;
        return max;
    }

    //Set the minimum
    private float setMin(float avg, float min) {
        if (avg < min)
            min = avg;
        return min;
    }

    //Draw the graph
    private void drawGraph() {

        //Iterate 7 times for days
        for (int pos = 0; pos < 7; pos++) {
            DataPoint dataPointTemp = dataPointsTemp[pos];
            DataPoint dataPointHumidity = dataPointsHumidity[pos];
            DataPoint dataPointGust = dataPointsGust[pos];
            DataPoint dataPointNoise = dataPointsNoise[pos];
            DataPoint dataPointPressure = dataPointsPressure[pos];
            DataPoint dataPointWind = dataPointsWind[pos];
            DataPoint dataPointRain = dataPointsRain[pos];
            DataPoint dataPointRainHour = dataPointsRainHour[pos];
            DataPoint dataPointRainDay = dataPointsRainDay[pos];

            //If an element's value for a particular day is null and greater than 0, set it to 0, otherwise set it to the minimum-30
            if (dataPointTemp == null)
                dataPointsTemp[pos] = new DataPoint(pos, maxTemp >= 0 ? 0 : minTemp - 30);

            if (dataPointHumidity == null)
                dataPointsHumidity[pos] = new DataPoint(pos, maxHumidity >= 0 ? 0 : minHumidity - 30);

            if (dataPointGust == null)
                dataPointsGust[pos] = new DataPoint(pos, maxGust >= 0 ? 0 : minGust - 30);

            if (dataPointNoise == null)
                dataPointsNoise[pos] = new DataPoint(pos, maxNoise >= 0 ? 0 : minNoise - 30);

            if (dataPointPressure == null)
                dataPointsPressure[pos] = new DataPoint(pos, maxPressure >= 0 ? 800 : minPressure - 30);

            if (dataPointWind == null)
                dataPointsWind[pos] = new DataPoint(pos, maxWind >= 0 ? 0 : minWind - 30);

            if (dataPointRain == null)
                dataPointsRain[pos] = new DataPoint(pos, 0);

            if (dataPointRainHour == null)
                dataPointsRainHour[pos] = new DataPoint(pos, 0);

            if (dataPointRainDay == null)
                dataPointsRainDay[pos] = new DataPoint(pos, 0);
        }

        seriesTemp = new LineGraphSeries<>(dataPointsTemp);
        seriesHumidity = new LineGraphSeries<>(dataPointsHumidity);
        seriesGust = new LineGraphSeries<>(dataPointsGust);
        seriesNoise = new LineGraphSeries<>(dataPointsNoise);
        seriesPressure = new LineGraphSeries<>(dataPointsPressure);
        seriesRain = new LineGraphSeries<>(dataPointsRain);
        seriesRainHour = new LineGraphSeries<>(dataPointsRainHour);
        seriesRainDay = new LineGraphSeries<>(dataPointsRainDay);
        seriesWind = new LineGraphSeries<>(dataPointsWind);

        ((LineGraphSeries<DataPoint>) seriesHumidity).setColor(Color.RED);
        ((LineGraphSeries<DataPoint>) seriesGust).setColor(Color.BLACK);
        ((LineGraphSeries<DataPoint>) seriesNoise).setColor(Color.YELLOW);
        ((LineGraphSeries<DataPoint>) seriesPressure).setColor(Color.GREEN);
        ((LineGraphSeries<DataPoint>) seriesRain).setColor(Color.MAGENTA);
        ((LineGraphSeries<DataPoint>) seriesRainHour).setColor(Color.DKGRAY);
        ((LineGraphSeries<DataPoint>) seriesRainDay).setColor(Color.CYAN);
        ((LineGraphSeries<DataPoint>) seriesWind).setColor(Color.BLUE);

        ((LineGraphSeries<DataPoint>) seriesHumidity).setDrawDataPoints(true);
        ((LineGraphSeries<DataPoint>) seriesGust).setDrawDataPoints(true);
        ((LineGraphSeries<DataPoint>) seriesNoise).setDrawDataPoints(true);
        ((LineGraphSeries<DataPoint>) seriesPressure).setDrawDataPoints(true);
        ((LineGraphSeries<DataPoint>) seriesRain).setDrawDataPoints(true);
        ((LineGraphSeries<DataPoint>) seriesRainHour).setDrawDataPoints(true);
        ((LineGraphSeries<DataPoint>) seriesRainDay).setDrawDataPoints(true);
        ((LineGraphSeries<DataPoint>) seriesWind).setDrawDataPoints(true);
        ((LineGraphSeries<DataPoint>) seriesTemp).setDrawDataPoints(true);

        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(6);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setYAxisBoundsManual(true);
        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graphView);
        staticLabelsFormatter.setHorizontalLabels(new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"});
        graphView.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);
        graphView.getGridLabelRenderer().setPadding(32);
        progressBar.setVisibility(View.GONE);
        graphView.setVisibility(View.VISIBLE);
        spinner.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.VISIBLE);

        refreshGraph();
    }

    //Draw the correct series
    private void refreshGraph() {

        graphView.removeAllSeries();

        //Draw the graph depending on the selected element
        if (spinner.getSelectedItem().toString().equals(elements[0]) && seriesHumidity != null) {
            setGraphMinMax(minHumidity, maxHumidity);
            graphView.addSeries(seriesHumidity);
        } else if (spinner.getSelectedItem().toString().equals(elements[1]) && seriesGust != null) {
            setGraphMinMax(minGust, maxGust);
            graphView.addSeries(seriesGust);
        } else if (spinner.getSelectedItem().toString().equals(elements[2]) && seriesNoise != null) {
            setGraphMinMax(minNoise, maxNoise);
            graphView.addSeries(seriesNoise);
        } else if (spinner.getSelectedItem().toString().equals(elements[3]) && seriesPressure != null) {
            setGraphMinMax(minPressure, maxPressure);
            graphView.addSeries(seriesPressure);
        } else if (spinner.getSelectedItem().toString().equals(elements[4]) && seriesRain != null) {
            setGraphMinMax(minRain, maxRain);
            graphView.addSeries(seriesRain);
        } else if (spinner.getSelectedItem().toString().equals(elements[5]) && seriesRainHour != null) {
            setGraphMinMax(minRainHour, maxRainHour);
            graphView.addSeries(seriesRainHour);
        } else if (spinner.getSelectedItem().toString().equals(elements[6]) && seriesRainDay != null) {
            setGraphMinMax(minRainDay, maxRainDay);
            graphView.addSeries(seriesRainDay);
        } else if (spinner.getSelectedItem().toString().equals(elements[7]) && seriesWind != null) {
            setGraphMinMax(minWind, maxWind);
            graphView.addSeries(seriesWind);
        } else if (spinner.getSelectedItem().toString().equals(elements[8]) && seriesTemp != null) {
            setGraphMinMax(minTemp, maxTemp);
            graphView.addSeries(seriesTemp);
        }

    }

    //Set the minimum and maximum Y axes of the graph
    private void setGraphMinMax(float min, float max) {
        graphView.getViewport().setMinY(max >= 0 ? (max > 700 ? 700 : 0) : min - 30);
        graphView.getViewport().setMaxY(max);
    }


}

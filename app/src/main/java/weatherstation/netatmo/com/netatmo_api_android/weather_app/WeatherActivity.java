package weatherstation.netatmo.com.netatmo_api_android.weather_app;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

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

import weatherstation.netatmo.com.netatmo_api_android.R;
import weatherstation.netatmo.com.netatmo_api_android.api.model.Measures;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        graphView = findViewById(R.id.weather_graph);
        progressBar = findViewById(R.id.progressBar_weather);
        textViewLocation = findViewById(R.id.textView_weather_location);
        textViewHumidity=findViewById(R.id.textView_weather_humidity);
        textViewGustStrength=findViewById(R.id.textView_weather_gust_strength);
        textViewGustAngle=findViewById(R.id.textView_weather_gust_angle);
        textViewNoise=findViewById(R.id.textView_weather_noise);
        textViewPressure=findViewById(R.id.textView_weather_pressure);
        textViewRain=findViewById(R.id.textView_weather_rain_live);
        textViewRainHour=findViewById(R.id.textView_weather_rain_hour);
        textViewRainDay=findViewById(R.id.textView_weather_rain_day);
        textViewWindStrength =findViewById(R.id.textView_weather_wind_strength);
        textViewWindAngle=findViewById(R.id.textView_weather_wind_angle);
        textViewTemp=findViewById(R.id.textView_weather_temperature);
        spinner = findViewById(R.id.spinner_weather);
        scrollView=findViewById(R.id.scrollView_weather);
        elements = getResources().getStringArray(R.array.menu_humidity);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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

        progressBar.setVisibility(View.VISIBLE);
        graphView.setVisibility(View.GONE);
        spinner.setVisibility(View.GONE);
        scrollView.setVisibility(View.GONE);

        databaseReference.child(stationId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot days : dataSnapshot.getChildren()) {
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

                    int key = 0;
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

                    for (DataSnapshot hours : days.getChildren()) {
                        Measures measure = hours.getValue(Measures.class);

                        Calendar calendar=Calendar.getInstance();
                        if (calendar.get(Calendar.DAY_OF_WEEK)==key+1 && hours.getKey().split(" ")[1].equals(calendar.get(Calendar.HOUR_OF_DAY)+"")){
                            textViewHumidity.setText(measure.getHumidity().equals("No data")?measure.getHumidity():getString(R.string.value_humidity,measure.getHumidity()));
                            textViewGustStrength.setText(measure.getGustStrength().equals("No data")?measure.getGustStrength():getString(R.string.value_wind,measure.getGustStrength()));
                            textViewGustAngle.setText(measure.getGustAngle().equals("No data")?measure.getGustAngle():getString(R.string.value_angle,measure.getGustAngle()));
                            textViewNoise.setText(measure.getNoise().equals("No data")?measure.getNoise():getString(R.string.value_noise, measure.getNoise()));
                            textViewPressure.setText(measure.getPressure().equals("No data")?measure.getPressure(): getString(R.string.value_pressure,measure.getPressure()));
                            textViewRain.setText(measure.getRain().equals("No data")?measure.getRain():getString(R.string.value_rain,measure.getRain()));
                            textViewRainHour.setText(measure.getSum_rain_1().equals("No data")?measure.getSum_rain_1():getString(R.string.value_rain, measure.getSum_rain_1()));
                            textViewRainDay.setText(measure.getSum_rain_24().equals("No data")?measure.getSum_rain_24():getString(R.string.value_rain,measure.getSum_rain_24()));
                            textViewWindStrength.setText(measure.getWindStrength().equals("No data")?measure.getWindStrength():getString(R.string.value_wind, measure.getWindStrength()));
                            textViewWindAngle.setText(measure.getWindAngle().equals("No data")?measure.getWindAngle():getString(R.string.value_angle,measure.getWindAngle()));
                            textViewTemp.setText(measure.getTemperature().equals("No data")?measure.getTemperature():getString(R.string.value_temperature, measure.getTemperature()));
                        }


                        if (TextUtils.isEmpty(textViewLocation.getText().toString())) {
                            textViewLocation.setText(measure.getLocation());
                        }

                        if (!measure.getTemperature().equals("No data")) {
                            totalTemp += Float.valueOf(measure.getTemperature());
                            countTemp++;
                        }
                        if (!measure.getHumidity().equals("No data")) {
                            totalHumidity += Float.valueOf(measure.getHumidity());
                            countHumidity++;
                        }
                        if (!measure.getGustStrength().equals("No data")) {
                            totalGust += Float.valueOf(measure.getGustStrength());
                            countGust++;
                        }
                        if (!measure.getNoise().equals("No data")) {
                            totalNoise += Float.valueOf(measure.getNoise());
                            countNoise++;
                        }
                        if (!measure.getPressure().equals("No data")) {
                            totalPressure += Float.valueOf(measure.getPressure());
                            countPressure++;
                        }
                        if (!measure.getRain().equals("No data")) {
                            totalRain += Float.valueOf(measure.getRain());
                            countRain++;
                        }
                        if (!measure.getSum_rain_1().equals("No data")) {
                            totalRainHour += Float.valueOf(measure.getSum_rain_1());
                            countRainHour++;
                        }
                        if (!measure.getSum_rain_24().equals("No data")) {
                            totalRainDay += Float.valueOf(measure.getSum_rain_24());
                            countRainDay++;
                        }
                        if (!measure.getWindStrength().equals("No data")) {
                            totalWind += Float.valueOf(measure.getWindStrength());
                            countWind++;
                        }
                    }


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

                drawGraph();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }

        });

    }

    private float setMax(float avg, float max) {
        if (avg > max)
            max = avg;
        return max;
    }

    private float setMin(float avg, float min) {
        if (avg < min)
            min = avg;
        return min;
    }

    private float getAvg(float total, int count) {
        return total / count;
    }

    private void drawGraph() {

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

            if (dataPointTemp == null)
                //If the maximum is negative, reduce the minimum
                dataPointsTemp[pos] = new DataPoint(pos, maxTemp >= 0 ? 0 : minTemp - 30);

            if (dataPointHumidity == null)
                //If the maximum is negative, reduce the minimum
                dataPointsHumidity[pos] = new DataPoint(pos, maxHumidity >= 0 ? 0 : minHumidity - 30);

            if (dataPointGust == null)
                //If the maximum is negative, reduce the minimum
                dataPointsGust[pos] = new DataPoint(pos, maxGust >= 0 ? 0 : minGust - 30);

            if (dataPointNoise == null)
                //If the maximum is negative, reduce the minimum
                dataPointsNoise[pos] = new DataPoint(pos, maxNoise >= 0 ? 0 : minNoise - 30);

            if (dataPointPressure == null)
                //If the maximum is negative, reduce the minimum
                dataPointsPressure[pos] = new DataPoint(pos, maxPressure >= 0 ? 800 : minPressure - 30);

            if (dataPointWind == null)
                //If the maximum is negative, reduce the minimum
                dataPointsWind[pos] = new DataPoint(pos, maxWind >= 0 ? 0 : minWind - 30);

            if (dataPointRain == null)
                //If there is no rain measurement, set 0
                dataPointsRain[pos] = new DataPoint(pos, 0);

            if (dataPointRainHour == null)
                //If there is no rain measurement, set 0
                dataPointsRainHour[pos] = new DataPoint(pos, 0);

            if (dataPointRainDay == null)
                //If there is no rain measurement, set 0
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

    private void refreshGraph() {

        graphView.removeAllSeries();

        if (spinner.getSelectedItem().toString().equals(elements[0]) && seriesHumidity!=null) {
            setGraphMinMax(minHumidity, maxHumidity);
            graphView.addSeries(seriesHumidity);
        }
        else if (spinner.getSelectedItem().toString().equals(elements[1]) && seriesGust!=null) {
            setGraphMinMax(minGust, maxGust);
            graphView.addSeries(seriesGust);
        }
        else if (spinner.getSelectedItem().toString().equals(elements[2]) && seriesNoise!=null) {
            setGraphMinMax(minNoise, maxNoise);
            graphView.addSeries(seriesNoise);
        }
        else if (spinner.getSelectedItem().toString().equals(elements[3]) && seriesPressure!=null) {
            setGraphMinMax(minPressure, maxPressure);
            graphView.addSeries(seriesPressure);
        }
        else if (spinner.getSelectedItem().toString().equals(elements[4]) && seriesRain!=null) {
            setGraphMinMax(minRain,maxRain);
            graphView.addSeries(seriesRain);
        }
        else if (spinner.getSelectedItem().toString().equals(elements[5]) && seriesRainHour!=null) {
            setGraphMinMax(minRainHour, maxRainHour);
            graphView.addSeries(seriesRainHour);
        }
        else if (spinner.getSelectedItem().toString().equals(elements[6]) && seriesRainDay!=null) {
            setGraphMinMax(minRainDay, maxRainDay);
            graphView.addSeries(seriesRainDay);
        }
        else if (spinner.getSelectedItem().toString().equals(elements[7]) && seriesWind!=null) {
            setGraphMinMax(minWind, maxWind);
            graphView.addSeries(seriesWind);
        }
        else if (spinner.getSelectedItem().toString().equals(elements[8]) && seriesTemp!=null) {
            setGraphMinMax(minTemp, maxTemp);
            graphView.addSeries(seriesTemp);
        }

    }

    private void setGraphMinMax(float min, float max) {
        graphView.getViewport().setMinY(max >= 0 ? (max > 700 ? 700 : 0) : min - 30);
        graphView.getViewport().setMaxY(max);
    }


}

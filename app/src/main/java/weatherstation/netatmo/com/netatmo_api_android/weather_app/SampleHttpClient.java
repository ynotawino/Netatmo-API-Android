package weatherstation.netatmo.com.netatmo_api_android.weather_app;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import weatherstation.netatmo.com.netatmo_api_android.R;
import weatherstation.netatmo.com.netatmo_api_android.api.NetatmoHttpClient;
import weatherstation.netatmo.com.netatmo_api_android.api.NetatmoUtils;
import weatherstation.netatmo.com.netatmo_api_android.api.model.Measures;
import weatherstation.netatmo.com.netatmo_api_android.api.model.Params;

/**
 * This is just an example of how you can extend NetatmoHttpClient.
 * Tokens are stored in the shared preferences of the app, but you can store them as you wish
 * as long as they are properly returned by the getters.
 * If you want to add your own '/getmeasure' requests, this is also the place to do it.
 */
public class SampleHttpClient extends NetatmoHttpClient {

    private Context context;

    private SharedPreferences mSharedPreferences;


    public SampleHttpClient(Context context) {
        super(context);
        this.context = context;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Override
    protected String getClientId() {
        return context.getString(R.string.client_id);
    }

    @Override
    protected String getClientSecret() {
        return context.getString(R.string.client_secret);
    }

    @Override
    protected String getAppScope() {
        return context.getString(R.string.app_scope);
    }

    @Override
    protected void storeTokens(String refreshToken, String accessToken, long expiresAt) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putString(NetatmoUtils.KEY_REFRESH_TOKEN, refreshToken);
        editor.putString(NetatmoUtils.KEY_ACCESS_TOKEN, accessToken);
        editor.putLong(NetatmoUtils.KEY_EXPIRES_AT, expiresAt);
        editor.apply();
    }

    @Override
    protected void clearTokens() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    @Override
    protected String getRefreshToken() {
        return mSharedPreferences.getString(NetatmoUtils.KEY_REFRESH_TOKEN, null);
    }

    @Override
    protected String getAccessToken() {
        return mSharedPreferences.getString(NetatmoUtils.KEY_ACCESS_TOKEN, null);
    }

    @Override
    protected long getExpiresAt() {
        return mSharedPreferences.getLong(NetatmoUtils.KEY_EXPIRES_AT, 0);
    }


    public List<Measures> getMeasures() {

        Uri.Builder netatmoBuilder = new Uri.Builder();
        netatmoBuilder.scheme("https");
        netatmoBuilder.authority("api.netatmo.com");
        netatmoBuilder.appendPath("api");
        netatmoBuilder.appendPath("getpublicdata");
        netatmoBuilder.appendQueryParameter("access_token", getAccessToken());
        netatmoBuilder.appendQueryParameter("lat_ne", MainActivity.lat_ne);
        netatmoBuilder.appendQueryParameter("lon_ne", MainActivity.lon_ne);
        netatmoBuilder.appendQueryParameter("lat_sw", MainActivity.lat_sw);
        netatmoBuilder.appendQueryParameter("lon_sw", MainActivity.lon_sw);
        String urlString = netatmoBuilder.build().toString();
        URL url = null;
        List<Measures> measures = new ArrayList<>();
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpsURLConnection urlConnection;
        try {
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(10000);
            urlConnection.setReadTimeout(15000);
            urlConnection.setRequestMethod("GET");
            InputStream inputStream = urlConnection.getInputStream();
            String data = readStream(inputStream);
            JSONObject jsonData = new JSONObject(data);
            JSONArray body = jsonData.getJSONArray("body");
            measures = getMeasures(body);


        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return measures;
    }

    public String readStream(InputStream inputStream) {
        String string = "";
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            string = stringBuilder.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return string;
    }

    public List<Measures> getMeasures(JSONArray jsonArray) {
        List<Measures> myList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Measures aMeasure = new Measures(0);
                String stationId = jsonObject.getString("_id");
                aMeasure.setStationId(stationId);
                JSONObject place = jsonObject.getJSONObject("place");
                String location = place.getString("timezone");
                aMeasure.setLocation(location);
                JSONObject measures = jsonObject.getJSONObject("measures");
                Iterator<String> measuresKeys = measures.keys();
                while (measuresKeys.hasNext()) {
                    String module = (String) measuresKeys.next();
                    if (measures.get(module) instanceof JSONObject) {
                        JSONObject moduleMeasures = measures.getJSONObject(module);
                        JSONArray measureTypes = moduleMeasures.optJSONArray("type");
                        if (measureTypes != null) {
                            JSONObject res = moduleMeasures.getJSONObject("res");
                            Iterator<String> resKeys = res.keys();
                            String resKey = "";
                            while (resKeys.hasNext()) {
                                resKey = (String) resKeys.next();
                            }
                            JSONArray resValues = res.getJSONArray(resKey);
                            for (int k = 0; k < measureTypes.length(); k++) {
                                String measureType = measureTypes.getString(k);
                                String measureValue = resValues.getString(k);
                                switch (measureType) {
                                    case Params.TYPE_TEMPERATURE:
                                        aMeasure.setTemperature(measureValue);
                                        break;
                                    case Params.TYPE_HUMIDITY:
                                        aMeasure.setHumidity(measureValue);
                                        break;
                                    case Params.TYPE_PRESSURE:
                                        aMeasure.setPressure(measureValue);
                                        break;
                                    case Params.TYPE_NOISE:
                                        aMeasure.setNoise(measureValue);
                                        break;
                                    case Params.TYPE_MIN_TEMP:
                                        aMeasure.setMinTemp(measureValue);
                                        break;
                                    case Params.TYPE_MAX_TEMP:
                                        aMeasure.setMaxTemp(measureValue);
                                        break;
                                    case Params.TYPE_RAIN:
                                        aMeasure.setRain(measureValue);
                                        break;
                                    case Params.TYPE_RAIN_SUM_1:
                                        aMeasure.setSum_rain_1(measureValue);
                                        break;
                                    case Params.TYPE_RAIN_SUM_24:
                                        aMeasure.setSum_rain_24(measureValue);
                                        break;
                                    case Params.TYPE_WIND_ANGLE:
                                        aMeasure.setWindAngle(measureValue);
                                        break;
                                    case Params.TYPE_WIND_STRENGTH:
                                        aMeasure.setWindStrength(measureValue);
                                        break;
                                    case Params.TYPE_GUST_ANGLE:
                                        aMeasure.setGustAngle(measureValue);
                                        break;
                                    case Params.TYPE_GUST_STRENGTH:
                                        aMeasure.setGustStrength(measureValue);
                                        break;
                                    default:
                                }
                            }
                        } else {
                            Iterator<String> resKeys = moduleMeasures.keys();
                            String resKey = "";
                            while (resKeys.hasNext()) {
                                resKey = (String) resKeys.next();
                                String measureType = resKey;
                                String measureValue = moduleMeasures.getString(resKey);
                                switch (measureType) {
                                    case Params.TYPE_TEMPERATURE:
                                        aMeasure.setTemperature(measureValue);
                                        break;
                                    case Params.TYPE_HUMIDITY:
                                        aMeasure.setHumidity(measureValue);
                                        break;
                                    case Params.TYPE_PRESSURE:
                                        aMeasure.setPressure(measureValue);
                                        break;
                                    case Params.TYPE_NOISE:
                                        aMeasure.setNoise(measureValue);
                                        break;
                                    case Params.TYPE_MIN_TEMP:
                                        aMeasure.setMinTemp(measureValue);
                                        break;
                                    case Params.TYPE_MAX_TEMP:
                                        aMeasure.setMaxTemp(measureValue);
                                        break;
                                    case Params.TYPE_RAIN:
                                        aMeasure.setRain(measureValue);
                                        break;
                                    case Params.TYPE_RAIN_SUM_1:
                                        aMeasure.setSum_rain_1(measureValue);
                                        break;
                                    case Params.TYPE_RAIN_SUM_24:
                                        aMeasure.setSum_rain_24(measureValue);
                                        break;
                                    case Params.TYPE_WIND_ANGLE:
                                        aMeasure.setWindAngle(measureValue);
                                        break;
                                    case Params.TYPE_WIND_STRENGTH:
                                        aMeasure.setWindStrength(measureValue);
                                        break;
                                    case Params.TYPE_GUST_ANGLE:
                                        aMeasure.setGustAngle(measureValue);
                                        break;
                                    case Params.TYPE_GUST_STRENGTH:
                                        aMeasure.setGustStrength(measureValue);
                                        break;
                                    default:
                                }

                            }
                        }

                    }
                }

                myList.add(aMeasure);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return myList;
    }


    public void refresh() {
        refreshToken(getRefreshToken(), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                JSONObject jsonObject = null;
                try {
                    jsonObject = new JSONObject(response);
                    String access_token = jsonObject.getString("access_token");
                    String expires_in = jsonObject.getString("expires_in");
                    String refresh_token = jsonObject.getString("refresh_token");
                    SharedPreferences.Editor editor = mSharedPreferences.edit();
                    editor.putString(NetatmoUtils.KEY_REFRESH_TOKEN, refresh_token);
                    editor.putString(NetatmoUtils.KEY_ACCESS_TOKEN, access_token);
                    editor.putLong(NetatmoUtils.KEY_EXPIRES_AT, Long.valueOf(expires_in) * 1000 + System.currentTimeMillis());
                    editor.apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
    }
}

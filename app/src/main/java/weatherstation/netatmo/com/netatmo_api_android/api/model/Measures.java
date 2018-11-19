/*
 * Copyright 2013 Netatmo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package weatherstation.netatmo.com.netatmo_api_android.api.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.math.BigInteger;

public class Measures implements Parcelable {
    public static final String STRING_NO_DATA = "No data";
    public static final Creator<Measures> CREATOR = new Creator<Measures>() {
        @Override
        public Measures createFromParcel(Parcel in) {
            return new Measures(in);
        }

        @Override
        public Measures[] newArray(int size) {
            return new Measures[size];
        }
    };
    private long beginTime;
    private String temperature;
    private String humidity;
    private String pressure;
    private String noise;
    private String minTemp;
    private String maxTemp;
    private String rain;
    private String sum_rain_24;
    private String sum_rain_1;
    private String windAngle;
    private String windStrength;
    private String gustAngle;
    private String gustStrength;
    private String location;
    private String stationId;

    public Measures(int x) {
        beginTime = 0;
        temperature = STRING_NO_DATA;
        humidity = STRING_NO_DATA;
        pressure = STRING_NO_DATA;
        noise = STRING_NO_DATA;
        rain = STRING_NO_DATA;
        sum_rain_1 = STRING_NO_DATA;
        sum_rain_24 = STRING_NO_DATA;
        windAngle = STRING_NO_DATA;
        windStrength = STRING_NO_DATA;
        gustAngle = STRING_NO_DATA;
        gustStrength = STRING_NO_DATA;

        location = STRING_NO_DATA;
    }

    public Measures() {

    }

    protected Measures(Parcel in) {
        beginTime = in.readLong();
        temperature = in.readString();
        humidity = in.readString();
        pressure = in.readString();
        noise = in.readString();
        rain = in.readString();
        sum_rain_24 = in.readString();
        sum_rain_1 = in.readString();
        windAngle = in.readString();
        windStrength = in.readString();
        gustAngle = in.readString();
        gustStrength = in.readString();
        location = in.readString();
        stationId = in.readString();
    }

    public long getBeginTime() {
        return this.beginTime;
    }

    public void setBeginTime(long beginTime) {
        this.beginTime = beginTime;
    }

    public String getTemperature() {
        return temperature;
    }

    public void setTemperature(String temperature) {
        this.temperature = temperature;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getPressure() {
        return pressure;
    }

    public void setPressure(String pressure) {
        this.pressure = pressure;
    }

    public String getNoise() {
        return noise;
    }

    public void setNoise(String noise) {
        this.noise = noise;
    }

    public String getMinTemp() {
        return minTemp;
    }

    public void setMinTemp(String minTemp) {
        this.minTemp = minTemp;
    }

    public String getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(String maxTemp) {
        this.maxTemp = maxTemp;
    }

    public String getSum_rain_24() {
        return sum_rain_24;
    }

    public void setSum_rain_24(String sum_rain_24) {
        this.sum_rain_24 = sum_rain_24;
    }

    public String getSum_rain_1() {
        return sum_rain_1;
    }

    public void setSum_rain_1(String sum_rain_1) {
        this.sum_rain_1 = sum_rain_1;
    }

    public String getRain() {
        return rain;
    }

    public void setRain(String rain) {
        this.rain = rain;
    }

    public String getWindAngle() {
        return windAngle;
    }

    public void setWindAngle(String windAngle) {
        this.windAngle = windAngle;
    }

    public String getWindStrength() {
        return windStrength;
    }

    public void setWindStrength(String windStrength) {
        this.windStrength = windStrength;
    }

    public String getGustAngle() {
        return gustAngle;
    }

    public void setGustAngle(String gustAngle) {
        this.gustAngle = gustAngle;
    }

    public String getGustStrength() {
        return gustStrength;
    }

    public void setGustStrength(String gustStrength) {
        this.gustStrength = gustStrength;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Measures) {
            Measures measures = (Measures) obj;
            return location.equals(measures.getLocation());
        } else
            return false;
    }

    @Override
    public int hashCode() {
        StringBuilder locationName = new StringBuilder();
        for (int x = 0; x < location.length(); x++) {
            locationName.append((int) location.charAt(x));
        }
        BigInteger myLong = new BigInteger(locationName.toString());
        return myLong.intValue();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeLong(beginTime);
        dest.writeString(temperature);
        dest.writeString(humidity);
        dest.writeString(pressure);
        dest.writeString(noise);
        dest.writeString(rain);
        dest.writeString(sum_rain_1);
        dest.writeString(sum_rain_24);
        dest.writeString(windAngle);
        dest.writeString(windStrength);
        dest.writeString(gustAngle);
        dest.writeString(gustStrength);
        dest.writeString(location);
        dest.writeString(stationId);

    }
}
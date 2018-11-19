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
package weatherstation.netatmo.com.netatmo_api_android.weather_app;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import weatherstation.netatmo.com.netatmo_api_android.R;
import weatherstation.netatmo.com.netatmo_api_android.api.model.Measures;

public class MyAdapter extends ArrayAdapter<Measures> {
    private Context mContext;

    public MyAdapter(Context context, List<Measures> measures) {
        super(context, 0, measures);
        mContext = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        MyViewHolder myViewHolder;
        if (convertView == null) {
            myViewHolder = new MyViewHolder();
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.listview_row, parent, false);
            myViewHolder.temperature = (TextView) convertView.findViewById(R.id.text_temperature);
            myViewHolder.humidity = (TextView) convertView.findViewById(R.id.text_humidity);
            myViewHolder.rain_day = (TextView) convertView.findViewById(R.id.text_rain_day);
            myViewHolder.wind_strength = (TextView) convertView.findViewById(R.id.text_wind_strength);
            myViewHolder.wind_angle = (TextView) convertView.findViewById(R.id.text_wind_angle);
            myViewHolder.location = (TextView) convertView.findViewById(R.id.text_location);
            myViewHolder.label_rain = convertView.findViewById(R.id.label_rain);
            myViewHolder.label_wind = convertView.findViewById(R.id.label_wind);
            convertView.setTag(myViewHolder);

        }
        myViewHolder = (MyViewHolder) convertView.getTag();
        Measures measures = getItem(position);

        if (measures.getRain() == null || measures.getRain().equals(Measures.STRING_NO_DATA)) {
            myViewHolder.rain_day.setVisibility(View.GONE);
            myViewHolder.label_rain.setVisibility(View.GONE);
        } else {
            myViewHolder.rain_day.setVisibility(View.VISIBLE);
            myViewHolder.label_rain.setVisibility(View.VISIBLE);
        }
        if (measures.getWindStrength() == null || measures.getWindStrength().equals(Measures.STRING_NO_DATA)) {
            myViewHolder.wind_strength.setVisibility(View.GONE);
        } else {
            myViewHolder.wind_strength.setVisibility(View.VISIBLE);
        }
        if (measures.getWindAngle() == null || measures.getWindAngle().equals(Measures.STRING_NO_DATA)) {
            myViewHolder.wind_angle.setVisibility(View.GONE);
        } else {
            myViewHolder.wind_angle.setVisibility(View.VISIBLE);
        }
        if (measures.getWindStrength() == null || (measures.getWindAngle().equals(Measures.STRING_NO_DATA) && measures.getWindStrength().equals(Measures.STRING_NO_DATA))) {
            myViewHolder.label_wind.setVisibility(View.GONE);
        } else {
            myViewHolder.label_wind.setVisibility(View.VISIBLE);
        }
        myViewHolder.temperature.setText(mContext.getString(R.string.value_temperature, measures.getTemperature()));
        myViewHolder.humidity.setText(mContext.getString(R.string.value_humidity, measures.getHumidity()));
        myViewHolder.rain_day.setText(mContext.getString(R.string.value_rain, measures.getSum_rain_24()));
        myViewHolder.wind_strength.setText(mContext.getString(R.string.value_wind, measures.getWindStrength()));
        myViewHolder.wind_angle.setText(mContext.getString(R.string.value_angle, measures.getWindAngle()));
        myViewHolder.location.setText(measures.getLocation().split("/").length > 1 ? measures.getLocation().split("/")[1] : measures.getLocation());
        return convertView;
    }


    class MyViewHolder {
        TextView temperature, humidity, rain_day, wind_strength, wind_angle, location, label_rain, label_wind;

        public MyViewHolder() {

        }
    }
}
package com.example.android.sunshine.app;

import android.net.Uri;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Mike on 3/6/2016.
 */
public class ForecastAsyncTask extends AsyncTask<String, Void, List<String>> {

    private static final String LOG_TAG = ForecastAsyncTask.class.getName();

    private static final String FORECAST_URL_SCHEME = "http";

    private static final String FORECAST_URL_AUTHORITY = "api.openweathermap.org";

    private static final String FORECAST_URL_PATH = "/data/2.5/forecast/daily";

    private static final String APP_ID_PARAM_VAL = "bc5abbc1c54b0be78b4af3f961baee56";

    private static final String APP_ID_PARAM_NAME = "appid";

    private static final int NUM_OF_DAYS = 7;

    private static final String NUM_OF_DAYS_PARAM_NAME = "cnt";

    private static final String UNITS = "metric";

    private static final String UNITS_PARAM_NAME = "units";

    private static final String MODE = "json";

    private static final String MODE_PARAM_NAME = "mode";

    private static final String QUERY_PARAM_NAME = "q";

    @Override
    protected List<String> doInBackground(String... params) {

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            Log.d(LOG_TAG, "Refreshing the data with the following zip code: " + params[0]);
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.scheme(FORECAST_URL_SCHEME).authority(FORECAST_URL_AUTHORITY).path(FORECAST_URL_PATH)
                    .appendQueryParameter(QUERY_PARAM_NAME, params[0])
                    .appendQueryParameter(MODE_PARAM_NAME, MODE)
                    .appendQueryParameter(APP_ID_PARAM_NAME, APP_ID_PARAM_VAL)
                    .appendQueryParameter(NUM_OF_DAYS_PARAM_NAME, String.valueOf(NUM_OF_DAYS))
                    .appendQueryParameter(UNITS_PARAM_NAME, String.valueOf(UNITS))
                    .appendQueryParameter(APP_ID_PARAM_NAME, APP_ID_PARAM_VAL);

            Log.d(LOG_TAG, "Refreshing the data with the following url: " + uriBuilder.toString());

            URL url = new URL(uriBuilder.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            forecastJsonStr = buffer.toString();
            Log.d(LOG_TAG, forecastJsonStr);
            List<String> ret = getWeatherDataFromJson(forecastJsonStr, 7, "1".equalsIgnoreCase(params[1]));
            Log.d(LOG_TAG, "Returning " + ret.size() + " results.");
            return ret;
        } catch (IOException e) {
            Log.e("PlaceholderFragment", "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        } catch (JSONException jse) {
            Log.e(LOG_TAG, "JSON Parse Exception: ", jse);
            return null;
        }finally{
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
    }

    @Override
    protected void onPostExecute(List<String> strings) {
        super.onPostExecute(strings);
        ForecastFragment.arrayAdapter.clear();
        ForecastFragment.arrayAdapter.addAll(strings);
    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
            * so for convenience we're breaking it out into its own method now.
            */
    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low, boolean isImperialUnits) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        if (isImperialUnits) {
            roundedHigh = convertCelsiusToFarenheight(roundedHigh);
            roundedLow = convertCelsiusToFarenheight(roundedLow);
        }

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    private long convertCelsiusToFarenheight(long degreesCelsius) {
        return (9/5) * degreesCelsius + 32;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private List<String> getWeatherDataFromJson(String forecastJsonStr, int numDays, boolean isImperialUnits)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        // OWM returns daily forecasts based upon the local time of the city that is being
        // asked for, which means that we need to know the GMT offset to translate this data
        // properly.

        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // now we work exclusively in UTC
        dayTime = new Time();

        List<String> resultStrs = new ArrayList<String>(numDays);
        for(int i = 0; i < weatherArray.length(); i++) {
            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            long dateTime;
            // Cheating to convert this to UTC time, which is what we want anyhow
            dateTime = dayTime.setJulianDay(julianStartDay+i);
            day = getReadableDateString(dateTime);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high, low, isImperialUnits);
            resultStrs.add(day + " - " + description + " - " + highAndLow);
        }

        for (String s : resultStrs) {
            Log.v(LOG_TAG, "Forecast entry: " + s);
        }
        return resultStrs;

    }
}

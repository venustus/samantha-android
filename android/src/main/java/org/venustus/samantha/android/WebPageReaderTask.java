package org.venustus.samantha.android;

import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by venkat on 24/10/15.
 */
public class WebPageReaderTask extends AsyncTask<String, Integer, Void> {

    private static final String TAG = "WebPageReaderTask";
    private TextToSpeech ttsObj;

    public WebPageReaderTask(TextToSpeech ttsObj) {
        this.ttsObj = ttsObj;
    }

    @Override
    protected Void doInBackground(String... urls) {
        HttpURLConnection conn;
        try {
            URL url = new URL(urls[0]);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            Log.d(TAG, "Response code: " + conn.getResponseCode());
            InputStream inputStream = conn.getInputStream();
            BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStream, "utf-8"), 8);
            StringBuilder sBuilder = new StringBuilder();

            String line;
            while ((line = bReader.readLine()) != null) {
                sBuilder.append(line + "\n");
            }

            inputStream.close();
            String result = sBuilder.toString();
            JSONObject resultObj = new JSONObject(result);
            JSONArray speakablesArr = resultObj.getJSONArray("speakables");
            for(int i = 0; i < speakablesArr.length(); i++) {
                JSONObject speakable = speakablesArr.getJSONObject(i);
                String utteranceId = "speakable-" + i;
                if(i == speakablesArr.length() - 1) utteranceId = "speakable-last";
                ttsObj.speak(speakable.getString("text"), TextToSpeech.QUEUE_ADD, null, null);
                ttsObj.playSilentUtterance(500, TextToSpeech.QUEUE_ADD, utteranceId);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error converting result " + e.toString());
            ttsObj.speak("Sorry, I could not read this web page", TextToSpeech.QUEUE_ADD, null, "speakable-last");
        }
        return null;
    }
}

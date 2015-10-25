package org.venustus.samantha.android;

import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.twitter.sdk.android.Twitter;
import com.twitter.sdk.android.core.*;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.models.MentionEntity;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.models.UrlEntity;
import com.twitter.sdk.android.core.services.StatusesService;
import io.fabric.sdk.android.Fabric;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class Conversation extends ActionBarActivity {

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = "Wq0EtkXZBAT0GfWQKAC1NyhlP";
    private static final String TWITTER_SECRET = "PnWsBTPXgiqSqNJhqvjgV5i2V5JRGYUB6eUGmqkKRq5f0EUJhB";
    private static final String TAG = "ConversationActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new Twitter(authConfig));
        setContentView(R.layout.activity_conversation);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_conversation, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result to the fragment, which will then pass the result to the login
        // button.
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.container);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        TwitterLoginButton loginButton;
        Button readTweetsButton;
        private TextToSpeech ttsObj;
        private SpeechRecognizer speechRecognizer;

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.fragment_conversation, container, false);
            TwitterSession ts = TwitterCore.getInstance().getSessionManager().getActiveSession();
            loginButton = (TwitterLoginButton) rootView.findViewById(R.id.login_button);
            readTweetsButton = (Button) rootView.findViewById(R.id.read_tweets);
            final TweetReader tweetReader;

            ttsObj = new TextToSpeech(getActivity(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status == TextToSpeech.SUCCESS) {
                        Log.d(TAG, "Text to speech engine initialized");
                        ttsObj.setLanguage(Locale.US);
                    }
                    else {
                        Log.e(TAG, "Text to speech engine cannot be initialized");
                    }
                }
            }, "com.ivona.tts");
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getActivity());

            tweetReader = new TweetReader(ttsObj, speechRecognizer);

            if(ts == null) {
                readTweetsButton.setVisibility(View.INVISIBLE);
                loginButton.setCallback(new Callback<TwitterSession>() {
                    @Override
                    public void success(Result<TwitterSession> result) {
                        TwitterSession ts = result.data;
                        TwitterAuthToken tat = ts.getAuthToken();
                        Log.d(TAG, "Twitter token: " + tat.token + ", secret: " + tat.secret);
                        loginButton.setVisibility(View.INVISIBLE);
                        readTweetsButton.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void failure(TwitterException exception) {
                        Log.e(TAG, "Twitter exception: " + exception.getMessage(), exception);
                    }
                });
            }
            else {
                loginButton.setVisibility(View.INVISIBLE);
                readTweetsButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        TwitterApiClient twitterApiClient = TwitterCore.getInstance().getApiClient();
                        StatusesService statusesService = twitterApiClient.getStatusesService();
                        statusesService.homeTimeline(null, null, null, null, true, true, true, tweetReader);
                    }
                });
            }

            ttsObj.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {

                }

                @Override
                public void onDone(String utteranceId) {
                    if (utteranceId.equals(tweetReader.getLastTweetRead())) {
                        rootView.post(new Runnable() {
                            public void run() {
                                speechRecognizer.startListening(new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE));
                            }
                        });
                    }
                    else if(utteranceId.equals("speakable-last")) {
                        tweetReader.readNextTweet();
                    }
                }

                @Override
                public void onError(String utteranceId) {

                }
            });


            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                public void onBeginningOfSpeech() {
                    Log.d(TAG, "onBeginningOfSpeech");
                }

                public void onResults(Bundle results) {
                    Log.d(TAG, "onResults " + results);
                    List<String> data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    for (String part : data) {
                        Log.d(TAG, "result " + part);
                    }
                    String firstResult = data.get(0);
                    if (firstResult.equals("read the link")) {
                        tweetReader.followLink();
                    }
                    else {
                        tweetReader.readNextTweet();
                    }
                }

                public void onRmsChanged(float rmsDB) {
                }

                public void onEndOfSpeech() {
                    Log.d(TAG, "onEndOfSpeech");
                }

                public void onReadyForSpeech(Bundle bundle) {
                    Log.d(TAG, "onReadyForSpeech");
                }

                public void onEvent(int eventType, Bundle params) {

                }

                public void onError(int error) {
                    Log.d(TAG, "onError");
                    String mError = "";
                    switch (error) {
                        case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                            mError = " network timeout";
                            break;
                        case SpeechRecognizer.ERROR_NETWORK:
                            mError = " network";
                            //toast("Please check data bundle or network settings");
                            return;
                        case SpeechRecognizer.ERROR_AUDIO:
                            mError = " audio";
                            break;
                        case SpeechRecognizer.ERROR_SERVER:
                            mError = " server";
                            break;
                        case SpeechRecognizer.ERROR_CLIENT:
                            mError = " client";
                            break;
                        case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                            mError = " speech time out";
                            break;
                        case SpeechRecognizer.ERROR_NO_MATCH:
                            mError = " no match";
                            break;
                        case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                            mError = " recogniser busy";
                            break;
                        case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                            mError = " insufficient permissions";
                            break;
                    }
                    Log.i(TAG, "Error: " + error + " - " + mError);
                    tweetReader.readNextTweet();
                }

                public void onBufferReceived(byte[] buffer) {

                }

                public void onPartialResults(Bundle results) {

                }
            });

            return rootView;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            // Pass the activity result to the login button.
            loginButton.onActivityResult(requestCode, resultCode, data);
        }
    }


}

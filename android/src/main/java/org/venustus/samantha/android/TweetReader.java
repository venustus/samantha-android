package org.venustus.samantha.android;

import android.content.Context;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.LinearLayout;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.models.MentionEntity;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.models.UrlEntity;
import com.twitter.sdk.android.tweetui.TweetView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by venkat on 24/10/15.
 */
public class TweetReader extends Callback<List<Tweet>> {
    private TextToSpeech ttsObj;
    private SpeechRecognizer speechRecognizer;
    private List<Tweet> allTweets = new ArrayList<>();
    private Tweet lastTweetRead;
    private static final String TAG = "TweetReader";
    private LinearLayout layout;
    private Context context;

    public TweetReader(TextToSpeech ttsObj, SpeechRecognizer speechRecognizer,
                       LinearLayout layout, Context context) {
        this.ttsObj = ttsObj;
        this.speechRecognizer = speechRecognizer;
        this.layout = layout;
        this.context = context;
    }

    public String getLastTweetRead() {
        return lastTweetRead.idStr;
    }

    public void followLink() {
        if(lastTweetRead.entities.urls.size() > 0) {
            String url = lastTweetRead.entities.urls.get(0).expandedUrl;
            try {
                String readerUrl = "http://ec2-52-27-159-241.us-west-2.compute.amazonaws.com/utp/" + URLEncoder.encode(url, "UTF-8") + "?jsonp=false";
                WebPageReaderTask wprt = new WebPageReaderTask(ttsObj);
                wprt.execute(readerUrl);
            }
            catch(UnsupportedEncodingException uee) {
                Log.e(TAG, "Unsuppoerted encoding!");
            }
        }
    }

    public void readNextTweet() {
        if(allTweets.size() == 0) return;
        final Tweet nextTweet = allTweets.get(0);
        allTweets.remove(0);
        Log.d(TAG, "Retrieved tweet " + nextTweet.text + " posted by " + nextTweet.user.name);
        layout.removeViewAt(layout.getChildCount() - 1);
        layout.addView(new TweetView(context, nextTweet));
        String tweetText = nextTweet.text;
        if(nextTweet.entities.userMentions.size() > 0) {
            for(MentionEntity me: nextTweet.entities.userMentions) {
                tweetText = tweetText.replace("@" + me.screenName, me.name);
            }
        }
        if(nextTweet.entities.urls.size() > 0) {
            for(UrlEntity urlEntity: nextTweet.entities.urls) {
                tweetText = tweetText.replace(urlEntity.url, "");
            }
        }
        tweetText = tweetText.replaceAll("https?://[\\S]+", "");
        Log.d(TAG, "Reading tweet as " + tweetText + " posted by " + nextTweet.user.name);
        ttsObj.speak(tweetText, TextToSpeech.QUEUE_ADD, null, null);
        ttsObj.playSilentUtterance(300, TextToSpeech.QUEUE_ADD, null);
        ttsObj.speak("Posted by " + nextTweet.user.name, TextToSpeech.QUEUE_ADD, null, nextTweet.idStr);
        lastTweetRead = nextTweet;
    }

    @Override
    public void success(Result<List<Tweet>> result) {
        allTweets.addAll(result.data);

        readNextTweet();
    }

    public void failure(TwitterException exception) {
        Log.e(TAG, "Error fetching tweets: " + exception.getMessage(), exception);
    }
}
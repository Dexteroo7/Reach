package reach.project.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ashish on 14/10/15.
 */
public class InstallTrackersReceiver extends BroadcastReceiver {

    private final Pattern UTM_SOURCE_PATTERN = Pattern.compile("(^|&)utm_source=([^&#=]*)([#&]|$)");
    private final Pattern UTM_MEDIUM_PATTERN = Pattern.compile("(^|&)utm_medium=([^&#=]*)([#&]|$)");
    private final Pattern UTM_CAMPAIGN_PATTERN = Pattern.compile("(^|&)utm_campaign=([^&#=]*)([#&]|$)");
    private final Pattern UTM_CONTENT_PATTERN = Pattern.compile("(^|&)utm_content=([^&#=]*)([#&]|$)");
    private final Pattern UTM_TERM_PATTERN = Pattern.compile("(^|&)utm_term=([^&#=]*)([#&]|$)");

    @Override
    public void onReceive(Context context, Intent intent) {
        new com.appvirality.android.AppviralityInstallReferrerReceiver().onReceive(context, intent);
        new com.mixpanel.android.mpmetrics.InstallReferrerReceiver().onReceive(context, intent);
        new com.google.android.gms.analytics.CampaignTrackingReceiver().onReceive(context, intent);

        MixpanelAPI mixpanel = MixpanelAPI.getInstance(context, "7877f44b1ce4a4b2db7790048eb6587a");
        MixpanelAPI.People ppl = mixpanel.getPeople();
        Bundle extras = intent.getExtras();
        if(null != extras) {
            String referrer = extras.getString("referrer");
            if(null != referrer) {
                ppl.set("referrer", referrer);
                Matcher sourceMatcher = this.UTM_SOURCE_PATTERN.matcher(referrer);
                String source = this.find(sourceMatcher);
                if(null != source) {
                    ppl.set("utm_source", source);
                }

                Matcher mediumMatcher = this.UTM_MEDIUM_PATTERN.matcher(referrer);
                String medium = this.find(mediumMatcher);
                if(null != medium) {
                    ppl.set("utm_medium", medium);
                }

                Matcher campaignMatcher = this.UTM_CAMPAIGN_PATTERN.matcher(referrer);
                String campaign = this.find(campaignMatcher);
                if(null != campaign) {
                    ppl.set("utm_campaign", campaign);
                }

                Matcher contentMatcher = this.UTM_CONTENT_PATTERN.matcher(referrer);
                String content = this.find(contentMatcher);
                if(null != content) {
                    ppl.set("utm_content", content);
                }

                Matcher termMatcher = this.UTM_TERM_PATTERN.matcher(referrer);
                String term = this.find(termMatcher);
                if(null != term) {
                    ppl.set("utm_term", term);
                }
            }
        }
        // Now you can pass the same intent on to other services
    }

    private String find(Matcher matcher) {
        if(matcher.find()) {
            String encoded = matcher.group(2);
            if(null != encoded) {
                try {
                    return URLDecoder.decode(encoded, "UTF-8");
                } catch (UnsupportedEncodingException var4) {
                    Log.e("MixpanelAPI.InstRfrRcvr", "Could not decode a parameter into UTF-8");
                }
            }
        }
        return null;
    }
}
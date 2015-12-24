package reach.project.utils;

import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import reach.project.core.ReachApplication;


public class SendSMS {

    //  declaring class variable
    static String api_key;
    static String sender_id;
    static String api_url;
    static String start;
    static String method;

    String time;
    String mob_no;
    String message;
    String unicode;
    String dlr_url;
    String type;

    //function to set parameter import java.net.URLEncoder;
    public SendSMS(String api_url, String method, String api_key, String sender_id, String start) {

        this.api_key = api_key;
        this.sender_id = sender_id;
        this.start = start;
        this.api_url = api_url;
        this.method = method;
    }

    /*function to send sms
      @ Simple message : last two field are set to null
      @ Unicode message :set unicode parameter to one
      @ Scheduled message : give time in 'ddmmyyyyhhmm' format
      return group_id to track
    */
    public String process_sms(String mob_no, String message, String dlr_url, String unicode, String time) throws IOException, KeyManagementException, NoSuchAlgorithmException {

        //addSslCertificate();
        message = URLEncoder.encode(message, "UTF-8");

        final URL url = new URL(start + api_url + "/api/v3/?method=" + method +
                "&api_key=" + api_key +
                "&sender=" + sender_id +
                "&to=" + mob_no +
                "&message=" + message +
                "&unicode=" + (TextUtils.isEmpty(unicode) ? "0" : unicode) +
                "&time=" + (TextUtils.isEmpty(time) ? "" : URLEncoder.encode(time, "UTF-8")) +
                "&format=json");

        final Request request = new Request.Builder()
                .url(url)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .post(null).build(); //just make it post

        final Response response = ReachApplication.okHttpClient.newCall(request).execute();
        final JsonElement receivedData = new JsonParser().parse(response.body().string());

        if (receivedData == null || !receivedData.isJsonObject())
            return null;

        final JsonObject jsonObject = receivedData.getAsJsonObject();
        final JsonElement dataAsElement = jsonObject.get("data");

        if (dataAsElement == null || !dataAsElement.isJsonObject())
            return null;

        final JsonObject dataAsObject = dataAsElement.getAsJsonObject();
        return dataAsObject.get("group_id").getAsString();
    }

    //function for checking message delivery status
    public String messagedelivery_status(String groupid) throws IOException {

        URL url = new URL("http://" + api_url + "/api/v3/?method=" + method + ".groupstatus&api_key=" + api_key + "&groupid=" + groupid + "&format=json");
//				System.out.println("url look like " + url );		 

        final Request request = new Request.Builder()
                .url(url)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .post(null).build(); //just make it post

        final Response response = ReachApplication.okHttpClient.newCall(request).execute();
        final JsonElement receivedData = new JsonParser().parse(response.body().string());

        if (receivedData == null || !receivedData.isJsonObject())
            return null;

        final JsonObject jsonObject = receivedData.getAsJsonObject();
        final JsonElement dataAsElement = jsonObject.get("data");

        if (dataAsElement == null || !dataAsElement.isJsonObject())
            return null;

        final JsonObject dataAsObject = dataAsElement.getAsJsonObject();
        return dataAsObject.get("status").getAsString();
    }

    public String send_sms(String mob_no, String message, String dlr_url) throws NoSuchAlgorithmException, IOException, KeyManagementException {
        return process_sms(mob_no, message, dlr_url, unicode = null, time = null);
    }
}

package reach.project.utils;

import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import okhttp3.CacheControl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import reach.project.core.ReachApplication;
import reach.project.onBoarding.smsRelated.GroupStatus;
import reach.project.onBoarding.smsRelated.SendResponse;
import reach.project.onBoarding.smsRelated.Status;


public class SendSMS {

    @Nullable
    private static SendSMS sendSMS = null;
    public static SendSMS getInstance() {

        if (sendSMS == null)
            sendSMS = new SendSMS(
                    "alerts.sinfini.com", //api_url
                    "sms", //method
                    "A6f5d83ea6aa5984be995761f221c8a9a", //api_key
                    "REACHA", //sender_id
                    "https://"); //start

        return sendSMS;
    }

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

    private SendSMS() {
        //empty private
    }

    //function to set parameter import java.net.URLEncoder;
    public SendSMS(String api_url, String method, String api_key, String sender_id, String start) {

        SendSMS.api_key = api_key;
        SendSMS.sender_id = sender_id;
        SendSMS.start = start;
        SendSMS.api_url = api_url;
        SendSMS.method = method;
    }

    /*function to send sms
      @ Simple message : last two field are set to null
      @ Unicode message :set unicode parameter to one
      @ Scheduled message : give time in 'ddmmyyyyhhmm' format
      return group_id to track
    */
    private SendResponse process_sms(String mob_no, String message, String dlr_url, String unicode, String time) throws IOException, KeyManagementException, NoSuchAlgorithmException {

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
                .post(RequestBody.create(MediaType.parse("application/octet-stream"), new byte[0])).build(); //just make it post

        final Response response = ReachApplication.OK_HTTP_CLIENT.newCall(request).execute();
        final String responseString = response.body().string();
        Log.i("Ayush", "SMS send response : " + responseString);
        final JsonElement receivedData = new JsonParser().parse(responseString);

        if (receivedData == null || !receivedData.isJsonObject())
            return new SendResponse(); //false status

        final JsonObject responseObject = receivedData.getAsJsonObject();
        final JsonElement dataAsElement = responseObject.get("data");

        if (dataAsElement == null || !dataAsElement.isJsonObject())
            return new SendResponse(); //false status

        final JsonObject dataAsObject = dataAsElement.getAsJsonObject();

        final SendResponse sendResponse = new SendResponse();
        try {
            sendResponse.success = responseObject.get("status").getAsString().equals("OK");
            sendResponse.status = Status.parseStatus(dataAsObject.get("0").getAsJsonObject().get("status").getAsString());
            sendResponse.trackingId = dataAsObject.get("group_id").getAsString();
        } catch (Exception ignored) {
        }

        return sendResponse;
    }

    //function for checking message delivery status
    private GroupStatus messagedelivery_status(String groupid) throws IOException {

        URL url = new URL("http://" + api_url + "/api/v3/?method=" + method + ".groupstatus&api_key=" + api_key + "&groupid=" + groupid + "&format=json");
//				System.out.println("url look like " + url );		 

        final Request request = new Request.Builder()
                .url(url)
                .cacheControl(CacheControl.FORCE_NETWORK)
                .post(RequestBody.create(MediaType.parse("application/octet-stream"), new byte[0])).build(); //just make it post

        final Response response = ReachApplication.OK_HTTP_CLIENT.newCall(request).execute();
        final String statusResponseString = response.body().string();
        Log.i("Ayush", "Status response string " + statusResponseString);
        final JsonElement receivedData = new JsonParser().parse(statusResponseString);

        if (receivedData == null || !receivedData.isJsonObject())
            return new GroupStatus(); //false status

        final JsonObject responseObject = receivedData.getAsJsonObject();
        final JsonElement dataAsElement = responseObject.get("data");

        if (dataAsElement == null || !dataAsElement.isJsonArray())
            return new GroupStatus(); //false status

        final JsonArray dataAsObject = dataAsElement.getAsJsonArray();

        final GroupStatus groupStatus = new GroupStatus();
        try {
            groupStatus.success = responseObject.get("status").getAsString().equals("OK");
            groupStatus.status = Status.parseStatus(dataAsObject.get(0).getAsJsonObject().get("status").getAsString());
        } catch (Exception ignored) {
        }

        return groupStatus;
    }

    public GroupStatus check_status(String groupId) {

        try {
            return messagedelivery_status(groupId);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new GroupStatus();
    }

    public SendResponse send_sms(String mob_no, String message, String dlr_url) {

        try {
            return process_sms(mob_no, message, dlr_url, unicode = null, time = null);
        } catch (IOException | KeyManagementException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new SendResponse();
    }
}

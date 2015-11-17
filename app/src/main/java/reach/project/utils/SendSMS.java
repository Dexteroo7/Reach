package reach.project.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;


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

    // function to set sender id
    public static void setsender_id(String sid) {
        sender_id = sid;
    }

    // function to set api_key key
    public static void setapi_key(String apk) {
        // checking for valid working key
        api_key = apk;
    }

    // function to set method
    public static void setmethod(String mt) {
        // checking for valid working key
        method = mt;
    }


    // function to set Api url
    public static void setapi_url(String ap) {
        //checking for valid url format
        String check = ap;
        String str = check.substring(0, 7);
//			    System.out.println(""+str );
        String t = "http://";
        String s = "https:/";
        String st = "https://";
        if (str.equals(t)) {
            start = t;
            api_url = check.substring(7);
        } else if (check.substring(0, 8).equals(st)) {
            start = st;
            api_url = check.substring(8);
        } else if (str.equals(s)) {
            start = st;
            api_url = check.substring(7);
        } else {
            start = st;
            api_url = ap;
        }
    }

    //function to set parameter import java.net.URLEncoder;
    public void setparams(String ap, String mt, String apk, String sd) {
        setapi_key(apk);
        //System.out.println(wk);
        setsender_id(sd);
        setapi_url(ap);
        setmethod(mt);
    }

    /*function to send sms
      @ Simple message : last two field are set to null
      @ Unicode message :set unicode parameter to one
      @ Scheduled message : give time in 'ddmmyyyyhhmm' format
    */
    public String process_sms(String mob_no, String message, String dlr_url, String unicode, String time) throws IOException, KeyManagementException, NoSuchAlgorithmException {

//        addSslCertificate();
        message = URLEncoder.encode(message, "UTF-8");
        if (unicode == null)
            unicode = "0";
        unicode = "&unicode=" + unicode;
        if (time == null)
            time = "";
        else time = "&time=" + URLEncoder.encode(time, "UTF-8");
        URL url = new URL("" + start + api_url + "/api/v3/?method=" + method + "&api_key=" + api_key + "&sender=" + sender_id + "&to=" + mob_no + "&message=" + message + unicode + time + "&format=json");

//		        System.out.println("url look like " + url );
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.getOutputStream();
        con.getInputStream();
        BufferedReader rd;
        String line;
        String result = "";
        rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
        while ((line = rd.readLine()) != null) {
            result += line;
        }
        rd.close();
//		        System.out.println("Result is" + result);
        return result;


    }

    //function for checking message delivery status
    public String messagedelivery_status(String mid) throws Exception {

        URL url = new URL("http://" + api_url + "/api/v3/?method=" + method + ".groupstatus&api_key=" + api_key + "&groupid=" + mid + "&format=json");
//				System.out.println("url look like " + url );		 
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.getOutputStream();
        con.getInputStream();
        BufferedReader rd;
        String line;
        String result = "";
        rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
        while ((line = rd.readLine()) != null) {
            result += line;
        }
        rd.close();
//		        System.out.println("Result is" + result);
        return result;
    }

    //function for checking group delivery status
    public String groupdelivery_status(String gid) throws Exception {

        URL url = new URL("http://" + api_url + "/api/v3/?method=" + method + ".groupstatus&api_key=" + api_key + "&groupid=" + gid + "&format=xml");
//				System.out.println("url look like " + url );		
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        con.getOutputStream();
        con.getInputStream();
        BufferedReader rd;
        String line;
        String result = "";
        rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
        while ((line = rd.readLine()) != null) {
            result += line;
//					System.out.println("Result is" + result);
        }
        rd.close();
//		        System.out.println("Result is" + result);
        return result;
    }

    public void unicode_sms(String mob_no, String message, String dlr_url, String unicode) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        process_sms(mob_no, message, dlr_url, unicode, time = null);
    }

    public void send_sms(String mob_no, String message, String dlr_url) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        process_sms(mob_no, message, dlr_url, unicode = null, time = null);
    }

    public void schedule_sms(String mob_no, String message, String dlr_url, String unicode, String time) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        process_sms(mob_no, message, dlr_url, unicode, time);
    }

    public void schedule_sms(String mob_no, String message, String dlr_url, String time) throws IOException, KeyManagementException, NoSuchAlgorithmException {
        process_sms(mob_no, message, dlr_url, unicode = null, time);
    }
}

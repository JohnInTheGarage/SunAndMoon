/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package driverway.sunandmoondata;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Properties;
import org.json.JSONObject;



/** https://api.met.no/weatherapi/
 *  https://api.met.no/weatherapi/sunrise/3.0/sun?lat=59.933333&lon=10.716667&date=2025-05-24&offset=+01:00
 * 
 *
 *
 * @author john
 */
public class SunAndMoonData {

    private Properties samd;
    private final HttpClient client = HttpClient.newHttpClient();
    private static final String FAKE_JSON = "{properties: {moonphase: 0.2 }}";
    
    //=============================
    /*
    * Return sunrise and set as localtimes instead of Properties
    */
    public SunTimes getSunTimes(String baseURL, String timezone, String latitude, String longitude){
        samd = new Properties();
        ZoneId zoneId = ZoneId.of(timezone);

        ZoneOffset rulesOffset = zoneId.getRules().getOffset(Instant.now());
        int hoursOffset = rulesOffset.getTotalSeconds() / 3600;
        String offsetUTC = String.format("%+03d:00", hoursOffset);
        
        String sunJson = callAPI(baseURL, "sun", latitude, longitude, offsetUTC);
        extractFields(sunJson, FAKE_JSON);
        LocalTime rise = LocalTime.parse(samd.getProperty("sunrise"));
        LocalTime set = LocalTime.parse(samd.getProperty("sunset"));
        return new SunTimes(rise, set);
    }
    
    /*========================================
    * Return times of sunrise and sunset plus moon illumination %age
    */
    public Properties collectData(String baseURL, String timezone, String latitude, String longitude) {
        samd = new Properties();
        ZoneId zoneId = ZoneId.of(timezone);

        ZoneOffset rulesOffset = zoneId.getRules().getOffset(Instant.now());
        int hoursOffset = rulesOffset.getTotalSeconds() / 3600;
        String offsetUTC = String.format("%+03d:00", hoursOffset);
        
        String sunJson = callAPI(baseURL, "sun", latitude, longitude, offsetUTC);
        String moonJson = callAPI(baseURL, "moon", latitude, longitude, offsetUTC);
        extractFields(sunJson, moonJson);
            
        return samd;
    }

    //========================================
    private void extractFields(String sunJson, String moonJson) {
        
        JSONObject jo = new JSONObject(sunJson);
        JSONObject sun = jo.getJSONObject("properties");
        JSONObject rise = sun.getJSONObject("sunrise");
        JSONObject set = sun.getJSONObject("sunset");
        String time = (String)rise.get("time");
        samd.put("sunrise", time.substring(11, 16));
        
        time = (String)set.get("time");
        samd.put("sunset", time.substring(11, 16));
        
        jo = new JSONObject(moonJson);
        JSONObject moon = jo.getJSONObject("properties");
        BigDecimal angle = (BigDecimal)moon.getBigDecimal("moonphase");  
        // For angle it is (angle of rotation around the earth), even though named as phase.
        //double moonAge = 0.5 * (1 - Math.cos(DEGREES_TO_RADIANS * notPhase.doubleValue() ));
        
        samd.put("moonangle", String.valueOf(angle));
    }

    //========================================
    private String callAPI(String baseURL, String body, String latitude, String longitude, String offsetUTC) {
        try {
            
            String sunURL = baseURL.formatted(body, latitude, longitude, offsetUTC);
            //LOGGER.trace("calling :" + sunURL);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(sunURL))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
            
        } catch (IOException | InterruptedException e) {
            return "";
        }
        

    }
   
//    public static void main(String[] args) {
//        new SunAndMoonData().getSunTimes(
//            "https://api.met.no/weatherapi/sunrise/3.0/%s?lat=%s&lon=%s&date=2025-06-05&offset=+01:00",
//            "Europe/London", 
//            "52.0", 
//            "-0.7");
//       
//        new SunAndMoonData().collectData (
//            "https://api.met.no/weatherapi/sunrise/3.0/%s?lat=%s&lon=%s&date=2025-06-05&offset=+01:00",
//            "Europe/London", 
//            "52.10", 
//            "-0.71");
//    }
//    
    
}


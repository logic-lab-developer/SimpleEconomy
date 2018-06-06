package net.logic_lab.spigot.simpleeconomy;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ExchangeRateReader {

    public static int getRate() throws Exception {

        String response = null;

        try {
            // 指定されたURLにアクセス
            URL req = new URL("https://www.gaitameonline.com/rateaj/getrate");

            HttpURLConnection connection = (HttpURLConnection) req.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int code = connection.getResponseCode();
            if( code != 200 ){
                throw new Exception( "ResponseCode: " + code );
            }

            // 読み込み処理
            BufferedReader in = new BufferedReader( new InputStreamReader( connection.getInputStream() ) );
            String input_line;
            StringBuffer stringBuffer = new StringBuffer();

            while( ( input_line = in.readLine() ) != null ){
                stringBuffer.append(input_line);
            }
            in.close();

            response = stringBuffer.toString();
        }
        catch( IOException e ){
            e.printStackTrace();
        }

        if( response == null || response.equals("") ){
            return -1;
        }

        JSONObject json = new JSONObject( response );

        // 現在のレート
        int current_rate = -1;

        JSONArray list = (JSONArray) json.get("quotes");
        if( list != null ){
            for( int i=0 ; i<list.length() ; i++ ){
                JSONObject record = (JSONObject)list.get(i);
                if( record.getString("currencyPairCode").equalsIgnoreCase("USDJPY") ){
                    double rate = record.getDouble("bid");
                    current_rate = (int)(rate * 100);
                    break;
                }
            }
        }

        return current_rate;
    }


}

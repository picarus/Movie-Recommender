package mai.cn.film.auth;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PostAuthentication {

    public static void main(String[] args) {

        try {
            URL url = new URL ("http://www.filmaffinity.com/en/login.php");
            
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            
            connection.setDoOutput(true);
            connection.setRequestProperty  ("ok", "Send");
            connection.setRequestProperty  ("user", "picarus");
            connection.setRequestProperty  ("password", "8C8PPkEc");
            //connection.setRequestProperty  ("postback", "1");
            //connection.setRequestProperty  ("rp", "/en/myvotes.php");
            
            InputStream content = (InputStream)connection.getInputStream();
            BufferedReader in   = 
                new BufferedReader (new InputStreamReader (content));
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

}
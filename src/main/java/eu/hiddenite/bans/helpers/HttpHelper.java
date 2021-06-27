package eu.hiddenite.bans.helpers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpHelper {
    public static String get(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            try (InputStream responseStream = connection.getInputStream()) {
                char[] buffer = new char[1024];
                StringBuilder out = new StringBuilder();
                Reader reader = new InputStreamReader(responseStream, StandardCharsets.UTF_8);
                for (int count; (count = reader.read(buffer, 0, buffer.length)) > 0; ) {
                    out.append(buffer, 0, count);
                }
                return out.toString();
            }
        } catch (Exception ex) {
            return null;
        }
    }
}

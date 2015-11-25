package agh.edu.pl.youtube;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.youtube.YouTube;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by grzegorz.miejski on 13/11/15.
 */
public class YoutubeClient {

    private static final String PROPERTIES_FILENAME = "youtube.properties";

    private YouTube youtube;
    private Properties properties;

    public YoutubeClient() {

        Properties properties = new Properties();
        try {
            InputStream in = YoutubeSearch.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
            properties.load(in);
            this.properties = properties;
        } catch (IOException e) {
            System.err.println("There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause()
                    + " : " + e.getMessage());
            System.exit(1);
        }

        youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, new HttpRequestInitializer() {
            public void initialize(HttpRequest request) throws IOException {
            }
        }).setApplicationName("API Project").build();
    }


    public YouTube get() {
        return youtube;
    }

    public String apiKey() {
        return properties.getProperty("youtube.apikey");
    }

}

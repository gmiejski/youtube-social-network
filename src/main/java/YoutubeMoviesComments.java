import agh.edu.pl.youtube.YoutubeClient;

import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentThread;

import java.io.IOException;
import java.util.List;

/**
 * Created by grzegorz.miejski on 13/11/15.
 */
public class YoutubeMoviesComments {

    public String getCommentsForMovie(String movieId) throws IOException {


        YoutubeClient youtubeClient = new YoutubeClient();

        List<CommentThread> commentThreads = youtubeClient.get().commentThreads().list("snippet")
                .setVideoId(movieId)
                .setKey(youtubeClient.apiKey())
                .setMaxResults(10L)
                .execute().getItems();

        commentThreads.stream().forEach(System.out::println);

        return null;
    }


    public static void main(String[] args) throws IOException {


        YoutubeMoviesComments youtubeMoviesComments = new YoutubeMoviesComments();
        youtubeMoviesComments.getCommentsForMovie("KlujizeNNQM");

    }

}

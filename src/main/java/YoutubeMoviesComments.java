import agh.edu.pl.youtube.YoutubeClient;

import com.google.api.services.youtube.model.CommentListResponse;
import com.google.api.services.youtube.model.CommentThread;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by grzegorz.miejski on 13/11/15.
 */
public class YoutubeMoviesComments {

    public String getCommentsForMovie(String movieId) throws IOException {


        YoutubeClient youtubeClient = new YoutubeClient();
        List<CommentThread> commentThreads = getCommentsThreads(movieId, youtubeClient);

        getRepliesCOunt(youtubeClient, commentThreads.get(0).getId());

//        commentThreads.stream().map(x -> x.getReplies().getgetSnippet());
//        getRepliesCOunt(youtubeClient, commentThreads.get(0).getSnippet().getTopLevelComment().getId());
        List<Integer> collect = commentThreads.stream().map(x -> {
            try {
                return getRepliesCOunt(youtubeClient, x.getSnippet().getTopLevelComment().getId());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).collect(Collectors.toList());

//        commentThreads.get(0).getReplies().getComments();

        return null;
    }

    private List<CommentThread> getCommentsThreads(String movieId, YoutubeClient youtubeClient) throws IOException {
        return youtubeClient.get().commentThreads().list("snippet")
                .setVideoId(movieId)
                .setKey(youtubeClient.apiKey())
                .setMaxResults(100L)
                .execute().getItems();
    }

    private int getRepliesCOunt(YoutubeClient youtubeClient, String parentCommentId) throws IOException {
        CommentListResponse response = youtubeClient.get().comments().list("snippet")
                .setKey(youtubeClient.apiKey())
                .setParentId(parentCommentId)
                .setTextFormat("plainText")
                .setMaxResults(100L)
                .execute();


        return response.getItems().size();
    }


    public static void main(String[] args) throws IOException {


        YoutubeMoviesComments youtubeMoviesComments = new YoutubeMoviesComments();
        String movieId = "KlujizeNNQM";
        youtubeMoviesComments.getCommentsForMovie(movieId);

    }
}

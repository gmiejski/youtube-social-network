package agh.edu.pl.youtube;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class VideoComments {
	
	private YoutubeClient youtubeClient;
	
	public VideoComments(YoutubeClient youtube) {
		this.youtubeClient = youtube;
	}

	private CommentThreadListResponse call(String videoId, String nextToken) throws IOException {
		YouTube.CommentThreads.List partial = youtubeClient.get().commentThreads()
			.list("snippet")
			.setVideoId(videoId)
			.setKey(youtubeClient.apiKey())
			.setMaxResults(100L);
		
		if(nextToken != null)
			partial.setPageToken(nextToken);
		
		return partial.execute();
	}
	
    public List<Comment> getComments(String videoId) throws IOException {

    	List<Comment> comments = new LinkedList<>();
		String nextPageToken = null;
		
		do {
			CommentThreadListResponse results = call(videoId, nextPageToken);
			List<CommentThread> items = results.getItems();
			for(CommentThread thread: items) {
				comments.add(thread.getSnippet().getTopLevelComment());
			}
			
			nextPageToken = results.getNextPageToken();
			System.out.println(nextPageToken);
			
		} while(nextPageToken != null);
		
		return comments;
    }

}

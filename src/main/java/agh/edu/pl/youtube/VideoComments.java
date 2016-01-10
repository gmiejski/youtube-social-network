package agh.edu.pl.youtube;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Comments;
import com.google.api.services.youtube.model.Comment;
import com.google.api.services.youtube.model.CommentListResponse;
import com.google.api.services.youtube.model.CommentThread;
import com.google.api.services.youtube.model.CommentThreadListResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class VideoComments {
	
	private YoutubeClient youtubeClient;
	
	public VideoComments(YoutubeClient youtube) {
		this.youtubeClient = youtube;
	}
	
	private CommentListResponse getResponseComments(String parentId, String nextToken) throws IOException {
		Comments.List partial = youtubeClient.get().comments()
			.list("snippet")
			.setParentId(parentId)
			.setKey(youtubeClient.apiKey())
			.setMaxResults(100L);
		
		if(nextToken != null)
			partial.setPageToken(nextToken);
		
		return partial.execute();
	}

	private CommentThreadListResponse getTopLevelComments(String videoId, String nextToken) throws IOException {
		YouTube.CommentThreads.List partial = youtubeClient.get().commentThreads()
			.list("snippet")
			.setVideoId(videoId)
			.setKey(youtubeClient.apiKey())
			.setMaxResults(100L);
		
		if(nextToken != null)
			partial.setPageToken(nextToken);
		
		return partial.execute();
	}
	
    public List<Comment> getComments(String videoId, String channelId) throws IOException {

    	List<Comment> comments = new LinkedList<>();
		String nextPageToken = null;
		
		do {
			CommentThreadListResponse results = getTopLevelComments(videoId, nextPageToken);
			List<CommentThread> items = results.getItems();
			for(CommentThread thread: items) {
				comments.add(thread.getSnippet().getTopLevelComment().set("target", channelId));
				if(thread.getSnippet().getTotalReplyCount() > 0) {
					String nextToken = null;
					do {
						CommentListResponse response = getResponseComments(thread.getId(), nextToken);
						String topLevelAuthor = YoutubeClient.getAuthor(thread.getSnippet().getTopLevelComment());
						
						Map<String, String> authors = new HashMap<>();
						List<Comment> tree = response.getItems();
						for(Comment comment: tree) {
							authors.put(comment.getSnippet().getAuthorDisplayName(), YoutubeClient.getAuthor(comment));
						}
						for(Comment comment: tree) {
							String text = comment.getSnippet().getTextDisplay();
							if (text.startsWith("+")) {
								comment.set("target", findTarget(text, authors, topLevelAuthor));							
							} else { 
								// there are also Google+ ones, no dev time for them
								comment.set("target", topLevelAuthor);
							}
						}
						comments.addAll(response.getItems());
						nextToken = response.getNextPageToken();
					} while(nextToken != null);
				}	
			}
			
			nextPageToken = results.getNextPageToken();
//			System.out.println(nextPageToken);
			
		} while(nextPageToken != null);
		
		return comments;
    }

	private String findTarget(String text, Map<String, String> authors, String topLevelAuthor) {
		for(String prefix: authors.keySet()) {
			if(text.startsWith(prefix, 1)) {
				return authors.get(prefix);
			}
		}
		return topLevelAuthor;
	}

}

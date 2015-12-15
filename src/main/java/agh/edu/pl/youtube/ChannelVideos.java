package agh.edu.pl.youtube;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

public class ChannelVideos {

	private YoutubeClient youtubeClient;
	
	public ChannelVideos(YoutubeClient youtube) {
		this.youtubeClient = youtube;
	}
	
	private SearchListResponse call(String channelId, String nextToken) throws IOException {
		YouTube.Search.List partial = youtubeClient.get().search()
			.list("snippet")
			.setChannelId(channelId)
			.setKey(youtubeClient.apiKey())
			.setMaxResults(50L)
			.setType("video");
		
		if(nextToken != null)
			partial.setPageToken(nextToken);
		
		return partial.execute();
	}
	
	public ConcurrentLinkedQueue<String> getVideoIds(String channelId) throws IOException {
		ConcurrentLinkedQueue<String> videoIds = new ConcurrentLinkedQueue<>();
		String nextPageToken = null;
		
		do {
			SearchListResponse results = call(channelId, nextPageToken);
			List<SearchResult> items = results.getItems();
			for(SearchResult video: items) {
				videoIds.add(video.getId().getVideoId());
//				System.out.println(video.getId().getVideoId());
			}
			
			nextPageToken = results.getNextPageToken();
//			System.out.println(nextPageToken);
			
		} while(nextPageToken != null);
		
		return videoIds;
	}

}

package agh.edu.pl.youtube;

import java.io.IOException;

public class App {

	static String videoId = "KlujizeNNQM";
	static String channelId = "UCGaVdbSav8xWuFWTadK6loA"; //vlogbrothers
	
	public static void main(String[] args) throws IOException {
		YoutubeClient youtube = new YoutubeClient();
		
		try(Neo4jPersister db = new Neo4jPersister()) {
			ChannelVideos channelVids = new ChannelVideos(youtube);
			VideoComments youtubeMoviesComments = new VideoComments(youtube);
			for(String videoId: channelVids.getVideoIds(channelId)) {
				
				db.persist(youtubeMoviesComments.getComments(videoId));
				
			}
		}
	}

}
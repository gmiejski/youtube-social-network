package agh.edu.pl.youtube;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {

	// -2UU3f5lDZ8 4 comments
	// K2jQgHaK1rs 49 comments
	static String videoId = "RtNesQeAKiY";
	// UCGaVdbSav8xWuFWTadK6loA vlogbrothers
	// UCue2Utve3Cz8Cb2eIJzWGUQ nanokarrin
	static String channelId = "UCGaVdbSav8xWuFWTadK6loA"; 
	static int threads = 8;
	
	public static void main(String[] args) throws IOException {
		YoutubeClient youtube = new YoutubeClient();
		
		// for resuming
		Set<String> processedFully = Collections.synchronizedSet(new HashSet<String>());
		try {
			try(FileInputStream streamIn = new FileInputStream("/media/ark/Windows7/Linux-Shared/app-cache");
			ObjectInputStream objectinputstream = new ObjectInputStream(streamIn);) {
				@SuppressWarnings("unchecked")
				Set<String> readCase = (Set<String>) objectinputstream.readObject();
				processedFully.addAll(readCase);
			    System.out.println("Resumed after "+readCase.size()+" videos...");
			}
		} catch(FileNotFoundException nocache) {
			System.out.println("No Cache, going clean");
		} catch (Exception other) {
		    other.printStackTrace();
		}
			
		ChannelVideos channelVids = new ChannelVideos(youtube);
		VideoComments youtubeMoviesComments = new VideoComments(youtube);
		long prev = System.currentTimeMillis();
		ConcurrentLinkedQueue<String> allVideos = channelVids.getVideoIds(channelId);
		allVideos.removeAll(processedFully);
		System.out.println("Got Vids!"+" time: "+((double)(System.currentTimeMillis() - prev))/1000+" seconds");
		
		ExecutorService service = Executors.newFixedThreadPool(threads);
		for(int i = 1; i < threads; i++) {
			service.execute(new Runnable() {
				@Override
				public void run() {
					try(Neo4jPersister db = new Neo4jPersister()) {
						while(!allVideos.isEmpty()) {
							String videoId = allVideos.poll();
							long prev = System.currentTimeMillis();
							try {
								if(videoId != null) {
									db.persist(youtubeMoviesComments.getComments(videoId));
								}
								processedFully.add(videoId);
							} catch (IOException e) {
								e.printStackTrace();
							}
							System.out.println(Thread.currentThread().getName()+" time: "+((double)(System.currentTimeMillis() - prev))/1000+" seconds");
						}	
					}
				}
			});
		}
		service.shutdown();
		try {
			ScheduledExecutorService execService = Executors.newScheduledThreadPool(1);
			execService.scheduleAtFixedRate(new Runnable() {
				public void run() {
					try {
						Files.deleteIfExists(new File("/media/ark/Windows7/Linux-Shared/app-cache").toPath());
						try(FileOutputStream fout = new FileOutputStream("/media/ark/Windows7/Linux-Shared/app-cache", false);
						    ObjectOutputStream oos = new ObjectOutputStream(fout);) {
							oos.writeObject(processedFully);
						} 
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}, 10L, 200L, TimeUnit.SECONDS);
			service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			execService.shutdownNow();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}

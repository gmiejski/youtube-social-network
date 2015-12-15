package agh.edu.pl.youtube;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App {

	private static final String PROPERTIES_FILENAME = "store.properties";

	static String channelId = "UC9-y-6csu5WGm29I7JiwpnA"; 
	static int maxThreads = 8;
	static Properties properties = new Properties();
	
	public static void main(String[] args) throws IOException {
		String cacheDir, dbDir, user, password;
		
		try {
            InputStream in = YoutubeSearch.class.getResourceAsStream("/" + PROPERTIES_FILENAME);
            properties.load(in);
        } catch (IOException e) {
            System.err.println("There was an error reading " + PROPERTIES_FILENAME + ": " + e.getCause()  + " : " + e.getMessage());
            System.exit(1);
        }
		cacheDir = properties.getProperty("cache_dir", System.getProperty("user.home") + "/" + "app-cache");
        dbDir = properties.getProperty("db_dir");
        user = properties.getProperty("user", "neo4j");
        password = properties.getProperty("password");
        
		YoutubeClient youtube = new YoutubeClient();
		// for resuming
		Set<String> processedFully = Collections.synchronizedSet(new HashSet<String>());
		try {
			try(FileInputStream streamIn = new FileInputStream(cacheDir);
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
		
		ExecutorService service = Executors.newFixedThreadPool(maxThreads);
		
		for(int i = 1; i <= maxThreads; i++) {
			service.execute(new Runnable() {
				@Override
				public void run() {
					try(Neo4jPersister db = new Neo4jPersister(dbDir, user, password)) {
						while(!allVideos.isEmpty()) {
							String videoId = allVideos.poll();
							long prev = System.currentTimeMillis();
							try {
								if(videoId != null) {
									Integer howMany = db.persist(youtubeMoviesComments.getComments(videoId));	
									processedFully.add(videoId);
									StringBuilder buffer = new StringBuilder(Thread.currentThread().getName());
									buffer.append(" ").append(howMany.toString()).append(" in ");
									buffer.append(((double)(System.currentTimeMillis() - prev))/1000).append(" seconds");
									System.out.println(buffer);
								}		
							} catch (IOException e) {
								e.printStackTrace();
							}		
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
						Files.deleteIfExists(new File(cacheDir).toPath());
						try(FileOutputStream fout = new FileOutputStream(cacheDir, false);
						    ObjectOutputStream oos = new ObjectOutputStream(fout);) {
							oos.writeObject(processedFully);
						} 
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}, 10L, 60L, TimeUnit.SECONDS);
			service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			execService.shutdownNow();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}

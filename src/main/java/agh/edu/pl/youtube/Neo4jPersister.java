package agh.edu.pl.youtube;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.api.services.youtube.model.Comment;

public class Neo4jPersister implements Closeable {
	
	// don't forget CREATE INDEX ON :Person(name)
	private Connection connection;
	
	public Neo4jPersister(String dir, String user, String password) {
		try {
			Class.forName("org.neo4j.jdbc.Driver");
			connection = DriverManager.getConnection("jdbc:neo4j:file:"+dir, user, password);
//			connection.setAutoCommit(false);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
            throw new RuntimeException(e);
        }
	}
	
	@Override
	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public int persist(List<Comment> comments, String channelId) throws IOException {
		Set<String> authors = new HashSet<>();
		for(Comment comment : comments) {
			authors.add(YoutubeClient.getAuthor(comment));
//			System.out.println("Author: " +YoutubeClient.getAuthor(comment));
//			System.out.println("Target: " +comment.get("target"));
		}
		if(authors.isEmpty()) {
			return 0;
		}
		int retVal = authors.size();

		String personSearch = "MATCH (p:Person) WHERE p.name = {1} RETURN p.name as name";
		String personCreate = "CREATE (p:Person { name: {1} })";
		String upCount = "MATCH (p1:Person {name: {1}})-[r:RELATED]->(p2:Person {name: {2}})"
				+ " SET r.count = r.count + 1 RETURN r.count";

		String grouping = "MATCH (p1:Person {name: {1}}), (p2:Person {name: {2}})"
				+ " CREATE UNIQUE (p1)-[r:RELATED {count: 1}]->(p2)";	
		
		for(String author: authors) {
			try {
				try(PreparedStatement stmt = connection.prepareStatement(personSearch))
				{
					stmt.setString(1, author);
					try (ResultSet rs = stmt.executeQuery()) {
						if(!rs.next()) {
							PreparedStatement st = connection.prepareStatement(personCreate);
							st.setString(1, author);
							st.execute();
						}
					}
				}

				try(PreparedStatement stmt = connection.prepareStatement(upCount))
				{
					stmt.setString(1, author);
					stmt.setString(2, channelId);
					try (ResultSet rs = stmt.executeQuery()) {
						if(!rs.next()) {
							PreparedStatement st = connection.prepareStatement(grouping);
						    st.setString(1, author);
						    st.setString(2, channelId);
						    st.execute();
						}
					}
					
				}
				
//				connection.commit();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
		for(Comment comment : comments) {
			try {
				String author = YoutubeClient.getAuthor(comment);
				String respondingTo = (String) comment.get("target");
				try(PreparedStatement stmt = connection.prepareStatement(upCount))
				{
					stmt.setString(1, author);
					stmt.setString(2, respondingTo);
					try (ResultSet rs = stmt.executeQuery()) {
						if(!rs.next()) {
							PreparedStatement st = connection.prepareStatement(grouping);
						    st.setString(1, author);
						    st.setString(2, respondingTo);
						    st.execute();
						}
					}
					
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		return retVal;
	}

}

 
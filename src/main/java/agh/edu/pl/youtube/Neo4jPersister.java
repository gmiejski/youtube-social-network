package agh.edu.pl.youtube;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.api.services.youtube.model.Comment;

public class Neo4jPersister implements Closeable {
	
	private Connection connection;
	
	public Neo4jPersister() {
		try {
			Class.forName("org.neo4j.jdbc.Driver");
			connection = DriverManager.getConnection(
				"jdbc:neo4j:mem:graph.db",
				"neo4j",
				"test"
			);
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
	
	public void persist(List<Comment> comments) throws IOException {
		Set<String> authors = new HashSet<>();
		for(Comment comment : comments) {
			if(comment.getSnippet().getAuthorChannelId() == null) {
				authors.add(comment.getSnippet().getAuthorGoogleplusProfileUrl());
			} else {
				authors.add(comment.getSnippet().getAuthorChannelId().getValue());
			}
		}
		System.out.println("Comments: "+authors.size());
		
		String personSearch = "MATCH (p:Person) WHERE p.name = {1} RETURN p.name as name";
		String personCreate = "CREATE (p:Person { name: {1} })";
		String upCount = "MATCH (p1:Person {name: {1}})-[r:RELATED]-(p2:Person {name: {2}})"
				+ " SET r.count = r.count + 1 RETURN r.count";

		String grouping = "MATCH (p1:Person {name: {1}}), (p2:Person {name: {2}})"
				+ " CREATE UNIQUE (p1)-[r:RELATED {count: 1}]-(p2)";	
		
		Iterator<String> it = authors.iterator();
		do {	
			try {
				String author = it.next();
				it.remove();
//				System.out.println(author);
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

				for (String collegue: authors) {
//					System.out.println(collegue);
					try(PreparedStatement stmt = connection.prepareStatement(upCount))
					{
						stmt.setString(1, author);
						stmt.setString(2, collegue);
						try (ResultSet rs = stmt.executeQuery()) {
							if(!rs.next()) {
								PreparedStatement st = connection.prepareStatement(grouping);
							    st.setString(1, author);
							    st.setString(2, collegue);
							    st.execute();
							}
						}
						
					}
				}
//				connection.commit();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		} while (it.hasNext());
	}

}

 
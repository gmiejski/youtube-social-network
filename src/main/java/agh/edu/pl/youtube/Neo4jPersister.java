package agh.edu.pl.youtube;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.google.api.services.youtube.model.Comment;

public class Neo4jPersister implements Closeable {
	
	private Connection connection;
	
	public Neo4jPersister() {
		try {
			Class.forName("org.neo4j.jdbc.Driver");
			connection = DriverManager.getConnection(
				"jdbc:neo4j://localhost:7474/",
				"neo4j",
				"test"
			);
			connection.setAutoCommit(false);
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
	
	public void persist(List<Comment> comments) {
		System.out.println("Comments: "+comments.size());
		
		String personSearch = "MATCH (p:Person) WHERE p.name = {1} RETURN p.name as name";
		String personCreate = "CREATE (p:Person { name: {1} })";
		String grouping = "MATCH (p1:Person {name: {1}}), (p2:Person {name: {2}})"
				+ " CREATE UNIQUE (p1)-[:RELATED {count: {3}}]-(p2)";
		
		for(Comment comment : comments) {
			try(PreparedStatement stmt = connection.prepareStatement(personSearch))
			{
				String author = comment.getSnippet().getAuthorChannelId().toString();
				stmt.setString(1, author);
				try (ResultSet rs = stmt.executeQuery()) {
					if(!rs.next()) {
						PreparedStatement st = connection.prepareStatement(personCreate);
						st.setString(1, author);
						st.execute();
					}
				}
			} catch (SQLException e) {
	        	throw new RuntimeException(e);
	        }

			for(Comment collegue : comments) {
				if(comment == collegue)
					continue;
				
				PreparedStatement st;
				try {
					String author = comment.getSnippet().getAuthorChannelId().toString();
					String author2 = collegue.getSnippet().getAuthorChannelId().toString();
					st = connection.prepareStatement(grouping);
				    st.setString(1, author);
				    st.setString(2, author2);
				    st.setInt
				    (3, 1);
				    st.execute();
				} catch (SQLException e) {
					throw new RuntimeException(e);
				}
			}
			
			try {
				connection.commit();
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
	}

}

 
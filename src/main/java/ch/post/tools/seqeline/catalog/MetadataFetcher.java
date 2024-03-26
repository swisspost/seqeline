package ch.post.tools.seqeline.catalog;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Slf4j
public class MetadataFetcher {
    private final String jdbcUrl;
    private final String username;
    private final String password;

    public MetadataFetcher(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Error: Oracle JDBC driver not found.");
        }
    }

    @SneakyThrows
    public void fetchMetadata(InputStream queryInputStream, OutputStream outputStream) {
        log.info("Fetching metadata. May take time on big databases ...");
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             BufferedReader queryReader = new BufferedReader(new InputStreamReader(queryInputStream));
             Writer outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream))) {

            StringBuilder queryBuilder = new StringBuilder();
            String line;
            while ((line = queryReader.readLine()) != null) {
                queryBuilder.append(line).append("\n");
            }
            String query = queryBuilder.toString();

            try (PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    // Assuming the query returns a single CLOB column
                    java.sql.Clob clob = resultSet.getClob(1);
                    if (clob != null) {
                        try (BufferedReader clobReader = new BufferedReader(clob.getCharacterStream())) {
                            char[] buffer = new char[1024];
                            int charsRead;
                            while ((charsRead = clobReader.read(buffer)) != -1) {
                                outputWriter.write(buffer, 0, charsRead);
                            }
                        }
                    }
                }
            }
        }
        log.info("done.");
    }
}
package com.project.navi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DataSourceSmokeTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void connectsToConfiguredSqliteDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("SQLite");
            assertThat(connection.isValid(2)).isTrue();
        }
    }

    @Test
    void supportsBasicReadAndWrite() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS navi_smoke_test (id INTEGER PRIMARY KEY, label TEXT)");
            statement.execute("DELETE FROM navi_smoke_test");
            statement.executeUpdate("INSERT INTO navi_smoke_test (label) VALUES ('etapa-1')");

            try (ResultSet resultSet = statement.executeQuery("SELECT label FROM navi_smoke_test")) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("label")).isEqualTo("etapa-1");
            }
        }
    }
}

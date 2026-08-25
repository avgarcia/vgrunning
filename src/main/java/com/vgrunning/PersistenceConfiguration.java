package com.vgrunning;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;

/** Configura la única conexión PostgreSQL y el gestor transaccional canónico de la aplicación. */
@Configuration(proxyBeanMethods = false)
class PersistenceConfiguration {

    /** Construye el pool Hikari compartido por Flyway, Spring JDBC y jOOQ. */
    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /** Delimita todas las transacciones JDBC y jOOQ sobre el mismo {@link DataSource}. */
    @Bean
    JdbcTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }
}

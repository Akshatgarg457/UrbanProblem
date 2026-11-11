package com.urbanproblems.config;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.zaxxer.hikari.HikariDataSource;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DatabaseProperties.class)
@Profile("!test")
public class ProductionDatabaseConfiguration {

	@Bean
	DataSource dataSource(DatabaseProperties properties) {
		return DataSourceBuilder.create()
				.type(HikariDataSource.class)
				.url(properties.url())
				.username(properties.username())
				.password(properties.password())
				.build();
	}

	@Bean
	HibernatePropertiesCustomizer productionHibernatePropertiesCustomizer() {
		return properties -> properties.put("hibernate.hbm2ddl.auto", "validate");
	}
}

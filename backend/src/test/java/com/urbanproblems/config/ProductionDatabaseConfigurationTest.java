package com.urbanproblems.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ProductionDatabaseConfigurationTest {

	@Test
	void productionSchemaActionOverridesDestructiveConfiguration() {
		Map<String, Object> hibernateProperties = new HashMap<>();
		hibernateProperties.put("hibernate.hbm2ddl.auto", "create-drop");

		new ProductionDatabaseConfiguration()
				.productionHibernatePropertiesCustomizer()
				.customize(hibernateProperties);

		assertEquals("validate", hibernateProperties.get("hibernate.hbm2ddl.auto"));
	}
}

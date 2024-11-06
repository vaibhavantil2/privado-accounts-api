package ai.privado.demo.accounts.config;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayDataSource;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.privado.demo.accounts.apistubs.DataLoggerS;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "AccountsEntityManagerFactory", transactionManagerRef = "AccountsTransactionManager", basePackages = {
		"ai.privado.demo.accounts.service.repos" })
public class GeneralConfig {

	private final Environment env;

	@Autowired
	public GeneralConfig(Environment env) {
		super();
		this.env = env;
	}

	@Bean(name = "AccountsDataSource")
	@FlywayDataSource
	@Primary
	public DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(Objects.requireNonNull(env.getProperty("spring.datasource.driver-class-name")));
		dataSource.setUrl(env.getProperty("accounts.datasource.url"));
		dataSource.setPassword(env.getProperty("accounts.datasource.username"));
		dataSource.setPassword(env.getProperty("accounts.datasource.password"));

		return dataSource;
	}

	@Bean(name = "AccountsEntityManagerFactoryBuilder")
	public EntityManagerFactoryBuilder entityManagerFactoryBuilder() {
		return new EntityManagerFactoryBuilder(jpaVendorAdapter(), new HashMap<String, String>(), null);
	}

	@Bean(name = "AccountsJpaManagerFactory")
	public JpaVendorAdapter jpaVendorAdapter() {
		HibernateJpaVendorAdapter hibernateJpaVendorAdapter = new HibernateJpaVendorAdapter();
		return hibernateJpaVendorAdapter;
	}

	@Bean(name = "AccountsEntityManagerFactory")
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(
			@Qualifier("AccountsEntityManagerFactoryBuilder") EntityManagerFactoryBuilder builder,
			@Qualifier("AccountsDataSource") DataSource dataSource) {
		return builder.dataSource(dataSource).packages("ai.privado.demo.accounts.service.entity").build();
	}

	@Bean(name = "AccountsTransactionManager")
	public PlatformTransactionManager transactionManager(
			@Qualifier("AccountsEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}

	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
	public ModelMapper modelMapper() {
		return new ModelMapper();
	}

	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
	public ObjectMapper objectMapper() {
		return new ObjectMapper();
	}

	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
	public DataLoggerS dataLogger() {
		return new DataLoggerS();
	}

	@Bean(name = "ApiCaller")
	@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
	public ExecutorService apiCallerExecutor() {
		return Executors.newFixedThreadPool(3);
	}
}

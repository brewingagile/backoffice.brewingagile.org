package org.brewingagile.backoffice.application;

import java.io.IOException;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.DispatcherType;
import javax.sql.DataSource;

import org.brewingagile.backoffice.auth.AuthenticationFilter;
import org.brewingagile.backoffice.rest.api.RegistrationApiRestService;
import org.brewingagile.backoffice.rest.gui.*;
import org.brewingagile.backoffice.utils.EtcPropertyFile;
import org.brewingagile.backoffice.utils.PostgresConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.postgresql.ds.PGPoolingDataSource;


public class Main {
	private static AtomicReference<Configuration> atomic = new AtomicReference<>(null);
	private static AtomicReference<DataSource> dataSource = new AtomicReference<>(null);

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			System.err.println("Expected properties file as argument.");
			System.exit(1);
			return;
		}
		String contextPath = "/ba-backoffice";

		String externalForm = Main.class.getClassLoader().getResource("resources/index.html").toExternalForm();
		String resourceBase = externalForm.replace("/index.html", "/");
		Integer port = 9080;

		System.out.println("Serving " + resourceBase);
		Server server = new Server(port);

		Configuration config = Configuration.from(EtcPropertyFile.from(args[0]).right());
		atomic.set(config);

		PostgresConnector postgresConnector = new PostgresConnector(config.dbHost, config.dbPort, config.dbName, config.dbUsername, config.dbPassword);
		PGPoolingDataSource ds = postgresConnector.poolingDatasource();
		postgresConnector.testConnection(ds);
		runDbUpgrades(ds);
		dataSource.set(ds);
		
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(contextPath);

		Resource inJar = Resource.newResource(resourceBase);
		ResourceCollection resourceCollection;
		{
			if (args.length > 1 && "--dev".equals(args[1])) {
				Resource inFilesystem = Resource.newResource("/home/henrik/brewingagile/ba-backoffice/application/src/main/webapp/");
				resourceCollection = new ResourceCollection(inFilesystem, inJar);
				System.out.println("--- DEV MODE ---");
			} else {
				resourceCollection = new ResourceCollection(inJar);
			}
		}
		context.setBaseResource(resourceCollection);

		AuthenticationFilter authenticationFilter = new AuthenticationFilter();
		context.addFilter(new FilterHolder(authenticationFilter), "/*", EnumSet.allOf(DispatcherType.class));
		context.addServlet(DefaultServlet.class, "/*");

		//api
		context.addServlet(new ServletHolder(new ServletContainer(new ResourceConfig(
			RegistrationApiRestService.class
		))),"/api/*");

		context.addServlet(new ServletHolder(new ServletContainer(new ResourceConfig(
				//rest.gui
				LoggedInRestService.class,
				VersionNumberRestService.class,
				//rest.gui.co
				RegistrationsRestService.class,
				NameTagsRestService.class,
				BucketsRestService.class,
				ReportsRestService.class,
				EmailCsvRestService.class
				))),"/gui/*");

		server.setHandler(context);

		server.start();
		System.out.println("Application started on http://localhost:" + port + contextPath + "/");
		server.join();
	}

	public static Configuration getConfiguration() {
		return atomic.get();
	}

	public static DataSource getDatasource() {
		return dataSource .get();
	}
	
	private static void runDbUpgrades(PGPoolingDataSource ds) {
		try {
			new DbUpgrader().upgrade(ds);
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
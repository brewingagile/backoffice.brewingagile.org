package org.brewingagile.backoffice.application;

import com.hencjo.summer.migration.util.Charsets;
import fj.data.Either;
import fj.data.List;
import org.brewingagile.backoffice.application.CmdArgumentParser.CmdArguments;
import org.brewingagile.backoffice.auth.AuthenticationFilter;
import org.brewingagile.backoffice.utils.EtcPropertyFile;
import org.brewingagile.backoffice.utils.PostgresConnector;
import org.brewingagile.backoffice.utils.jersey.NeverCacheBindingFeature;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.postgresql.ds.PGPoolingDataSource;

import javax.servlet.DispatcherType;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.stream.Collectors;


public class Main {
	public static final class ExitCodeAndMessage {
		public final int exitStatus;
		public final String message;
		public ExitCodeAndMessage(int exitStatus, String message) {
			this.exitStatus = exitStatus;
			this.message = message;
		}
	}

	public static Either<ExitCodeAndMessage, Server> subMain(String[] rawArgs) throws Exception {
		Either<String, CmdArguments> eitherArgs = CmdArgumentParser.parse(rawArgs);
		if (eitherArgs.isLeft())
			return eitherArgs.bimap(l -> new ExitCodeAndMessage(1, "Illegal arguments: " + l), r -> null);

		CmdArguments args = eitherArgs.right().value();

		String contextPath = "/";

		Either<ExitCodeAndMessage, Configuration> bimap = EtcPropertyFile.from(new InputStreamReader(new FileInputStream(args.propertiesFile), Charsets.UTF8))
			.bimap(
				l -> new ExitCodeAndMessage(1, "Could not read/find application property file '" + args.propertiesFile + "': " + l.getMessage()),
				Configuration::from
			);

		if (bimap.isLeft()) return Either.left(bimap.left().value());

		Configuration config = bimap.right().value();

		PostgresConnector postgresConnector = new PostgresConnector(config.dbHost, config.dbPort, config.dbName, config.dbUsername, config.dbPassword);
		PGPoolingDataSource ds = postgresConnector.poolingDatasource();
		postgresConnector.testConnection(ds);
		runDbUpgrades(ds);

		Application application = new Application(config, ds);

		int webPort = 9080;
		Server server = new Server(webPort);
		server.setHandler(context(config, application, contextPath, args));
		server.start();
		System.out.println("Application started on http://localhost:" + webPort + contextPath + "/");
		return Either.right(server);
	}

	public static void main(String[] rawArgs) throws Exception {
		Either<ExitCodeAndMessage, Server> server = subMain(rawArgs);
		if (server.isLeft()) {
			ExitCodeAndMessage left = server.left().value();
			System.err.println(left.message);
			System.exit(left.exitStatus);
			return;
		}
		server.right().value().join();
	}

	private static ServletContextHandler context(Configuration configuration, Application application, String contextPath, CmdArguments args) throws IOException {
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(contextPath);
		context.getSessionHandler().setMaxInactiveInterval(3600);
		context.setBaseResource(resourceCollection(args));
		context.addFilter(new FilterHolder(new AuthenticationFilter(configuration)), "/*", EnumSet.allOf(DispatcherType.class));
		context.addFilter(new FilterHolder(new IndexHtmlVersionRewriteFilter(application.versionNumberProvider)), "/", EnumSet.allOf(DispatcherType.class));
		context.addServlet(DefaultServlet.class, "/*");
		addJerseyServlet("/api/*", context,
			List.list(MultiPartFeature.class, NeverCacheBindingFeature.class),
			application.apiRestServices.toJavaList()
		);
		addJerseyServlet("/gui/*", context,
			List.list(MultiPartFeature.class, NeverCacheBindingFeature.class),
			application.guiRestServices.toJavaList()
		);
		return context;
	}

	private static void addJerseyServlet(String pathSpec, ServletContextHandler context, List<Class<?>> features, java.util.List<Object> resources) {
		ResourceConfig resourceConfig = new ResourceConfig(features.toCollection().stream().collect(Collectors.toSet()));
		resourceConfig.registerInstances(resources.stream().collect(Collectors.toSet()));
		context.addServlet(new ServletHolder(new ServletContainer(resourceConfig)), pathSpec);
	}

	private static ResourceCollection resourceCollection(CmdArguments args) throws IOException {
		Resource inJar = Resource.newResource(findWebResourceBase(), true);
		if (args.devOverlayDirectory.isSome()) {
			System.out.println("!!! DEV MODE: Build overlaid with: " + args.devOverlayDirectory.some());
			return new ResourceCollection(Resource.newResource(args.devOverlayDirectory.some(), false), inJar);
		} else {
			return new ResourceCollection(inJar);
		}
	}

	private static void runDbUpgrades(PGPoolingDataSource ds) {
		try {
			new DbUpgrader().upgrade(ds);
		} catch (SQLException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static String findWebResourceBase() {
		return Main.class.getClassLoader().getResource("webapp/index.html").toExternalForm().replace("/index.html", "/");
	}
}
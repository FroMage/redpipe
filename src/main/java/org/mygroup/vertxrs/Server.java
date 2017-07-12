package org.mygroup.vertxrs;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.validation.ValidatorFactory;

import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.jboss.resteasy.cdi.ResteasyCdiExtension;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.weld.bean.builtin.BeanManagerProxy;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.vertx.VertxExtension;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.DefaultJaxrsConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.config.ConfigRetriever;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import rx.plugins.RxJavaHooks;

public class Server {
	
	private Vertx vertx;

	public Server(){
	}
	
	public void start(){
		start(null, null);
	}
	
	public void start(JsonObject config, Handler<AsyncResult<Void>> handler){
		setupLogging();
		setupCDI();
		VertxResteasyDeployment deployment = setupResteasy();
		setupSwagger(deployment);
		if(config == null)
			vertx = setupVertx(deployment, handler);
		else
			vertx = setupVertx(Vertx.vertx(), config, deployment, handler);
	}
	
	private Vertx setupVertx(Vertx vertx, JsonObject config, VertxResteasyDeployment deployment, Handler<AsyncResult<Void>> handler) {
		// Get a DB
		JDBCClient dbClient = JDBCClient.createNonShared(vertx, new JsonObject()
				.put("url", config.getString("db_url", "jdbc:hsqldb:file:db/wiki"))
				.put("driver_class", config.getString("db_driver", "org.hsqldb.jdbcDriver"))
				.put("max_pool_size", config.getInteger("max_pool", 30)));

		// Save our injected globals
		AppGlobals globals = CDI.current().select(AppGlobals.class).get();
		globals.init(config, dbClient);

		// Propagate the Resteasy context on RxJava
		RxJavaHooks.setOnSingleCreate(new ResteasyContextPropagatingOnSingleCreateAction());

		// Setup the Vertx-CDI integration
		VertxExtension vertxExtension = CDI.current().select(VertxExtension.class).get();
		BeanManager beanManager = CDI.current().getBeanManager();
		// has to be done in a blocking thread
		vertx.executeBlocking(future -> {
			vertxExtension.registerConsumers(vertx.getDelegate(), BeanManagerProxy.unwrap(beanManager).event());
			future.complete();
		}, res -> {});

		// Start the front end server using the Jax-RS controller
		vertx.createHttpServer()
		// CDI
		.requestHandler(new VertxCdiRequestHandler(vertx, deployment))
		// Non-CDI
		//		        .requestHandler(new VertxRequestHandler(vertx, deployment))
		.listen(config.getInteger("http_port", 9000), ar -> {
			if(handler != null)
				handler.handle((AsyncResult)ar);
			if(ar.failed()){
				ar.cause().printStackTrace();
			}
			System.out.println("Server started on port "+ ar.result().actualPort());
			// rx?
			vertx.eventBus().send("vertxrs.init", "Init motherfucker!");
		});
		return vertx;
	}
	
	private Vertx setupVertx(VertxResteasyDeployment deployment, Handler<AsyncResult<Void>> handler) {
		Vertx vertx = Vertx.vertx();

		ConfigStoreOptions fileStore = new ConfigStoreOptions()
				.setType("file")
				.setConfig(new JsonObject().put("path", "conf/config.json"));

		ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions()
				.addStore(fileStore);

		ConfigRetriever retriever = ConfigRetriever.create(vertx, configRetrieverOptions);
		retriever.rxGetConfig().subscribe(config -> {
			setupVertx(vertx, config, deployment, handler);
		}, err -> {
			throw new RuntimeException(err);
		});
		return vertx;
	}

	private void setupSwagger(VertxResteasyDeployment deployment) {
		// Swagger
		ServletConfig servletConfig = new ServletConfig(){

			@Override
			public String getServletName() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ServletContext getServletContext() {
				return RxVertxProvider.ServletContext;
			}

			@Override
			public String getInitParameter(String name) {
				if("scan.all.resources".equals(name))
					return "true";
				return null;
			}

			@Override
			public Enumeration<String> getInitParameterNames() {
				// TODO Auto-generated method stub
				return null;
			}
		};
		DefaultJaxrsConfig swaggerServlet = new DefaultJaxrsConfig();
		try {
			swaggerServlet.init(servletConfig);
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		BeanConfig swaggerConfig = new BeanConfig();
		swaggerConfig.setVersion("1.0");
		swaggerConfig.setSchemes(new String[]{"http"});
		// FIXME: port does not come from config
		swaggerConfig.setHost("localhost:9000");
		swaggerConfig.setBasePath("/");
		swaggerConfig.setResourcePackage("org.mygroup.vertxrs");
		swaggerConfig.setPrettyPrint(true);
		swaggerConfig.setScan(true);
		deployment.getRegistry().addPerInstanceResource(ApiListingResource.class);
		deployment.getProviderFactory().register(SwaggerSerializers.class);
	}

	private VertxResteasyDeployment setupResteasy() {
		// Build the Jax-RS hello world deployment
		VertxResteasyDeployment deployment = new VertxResteasyDeployment();
		// Non-CDI
		//	    deployment.getRegistry().addPerInstanceResource(MyResource.class);
		//	    deployment.getProviderFactory().register(RxVertxProvider.class);
		//	    deployment.getProviderFactory().register(MyExceptionMapper.class);
		//	    deployment.setInjectorFactoryClass(CdiInjectorFactory.class.getName());
		// CDI
		ResteasyCdiExtension cdiExtension = CDI.current().select(ResteasyCdiExtension.class).get();
		deployment.setActualResourceClasses(cdiExtension.getResources());
		deployment.setInjectorFactoryClass(CdiInjectorFactory.class.getName());
		deployment.getActualProviderClasses().addAll(cdiExtension.getProviders());
		deployment.start();
		return deployment;
	}

	private void setupCDI() {
		// CDI
		Weld weld = new Weld();
		weld.addInterceptor(MyCdiInterceptor.class);
		weld.addExtension(new VertxExtension());
		weld.initialize();

		// Set up Resteasy to build BV with CDI
		try {
			NamingManager.setInitialContextFactoryBuilder(new InitialContextFactoryBuilder() {
				@Override
				public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
					return new InitialContextFactory() {

						@Override
						public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
							Context ctx = new InitialContext(){
								@Override
								public Object lookup(String name) throws NamingException {
									if(name.equals("java:comp/ValidatorFactory"))
										return CDI.current().select(ValidatorFactory.class).get();
									return null;
								}
							};
							return ctx;
						}
					};
				}
			});
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private void setupLogging() {
//        final ConsoleHandler consoleHandler = new ConsoleHandler();
//        consoleHandler.setLevel(Level.FINEST);
//        consoleHandler.setFormatter(new SimpleFormatter());
//
//        final Logger app = Logger.getLogger("org.jboss.weld.vertx");
//        app.setLevel(Level.FINEST);
//        app.addHandler(consoleHandler);
	}

	public void close(Handler<AsyncResult<Void>> asyncAssertSuccess) {
		vertx.close(asyncAssertSuccess);
	}

	public Vertx getVertx() {
		return vertx;
	}

	public static void main(String[] args) {
		Server test = new Server();
		test.start();
	}
}

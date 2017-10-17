package org.vertxrs.engine.core;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.vertxrs.engine.dispatcher.VertxPluginRequestHandler;
import org.vertxrs.engine.resteasy.RxVertxProvider;
import org.vertxrs.engine.rxjava.ResteasyContextPropagatingOnSingleCreateAction;
import org.vertxrs.engine.spi.Plugin;
import org.vertxrs.engine.template.TemplateRenderer;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.DefaultJaxrsConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.config.ConfigRetriever;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Single;
import rx.plugins.RxJavaHooks;

public class Server {
	
	private Vertx vertx;
	protected List<Plugin> plugins;

	public Server(){
//		System.setProperty("co.paralleluniverse.fibers.verifyInstrumentation", "true");
	}
	
	public Single<Void> start(){
		return start(null);
	}
	
	public Single<Void> start(JsonObject defaultConfig){
		setupLogging();
		
		VertxOptions options = new VertxOptions();
		options.setWarningExceptionTime(Long.MAX_VALUE);
		vertx = Vertx.vertx(options);
		AppGlobals.init();
		AppGlobals.get().setVertx(vertx);

		// Propagate the Resteasy context on RxJava
		RxJavaHooks.setOnSingleCreate(new ResteasyContextPropagatingOnSingleCreateAction());
		
		return loadConfig(defaultConfig)
				.flatMap(config -> {
					return setupPlugins()
							.flatMap(v -> setupTemplateRenderers())
							.flatMap(v -> setupResteasy())
							.flatMap(deployment -> {
								setupSwagger(deployment);
								return setupVertx(config, deployment);
							});
				});
	}
	
	private Single<Void> setupPlugins() {
		loadPlugins();
		return doOnPlugins(plugin -> plugin.preInit());
	}

	protected void loadPlugins() {
		plugins = new ArrayList<Plugin>();
		for(Plugin plugin : ServiceLoader.load(Plugin.class))
			plugins.add(plugin);
	}

	private Single<Void> setupTemplateRenderers() {
		List<TemplateRenderer> renderers = new ArrayList<>();
		for(TemplateRenderer renderer : ServiceLoader.load(TemplateRenderer.class))
			renderers.add(renderer);
		AppGlobals.get().setTemplateRenderers(renderers);
		return Single.just(null);
	}

	private Single<Void> setupVertx(JsonObject config, VertxResteasyDeployment deployment) {
		// Get a DB
		JDBCClient dbClient = JDBCClient.createNonShared(vertx, new JsonObject()
				.put("url", config.getString("db_url", "jdbc:hsqldb:file:db/wiki"))
				.put("driver_class", config.getString("db_driver", "org.hsqldb.jdbcDriver"))
				.put("max_pool_size", config.getInteger("max_pool", 30)));

		Class<?> mainClass = null;
		for (Class<?> resourceClass : deployment.getActualResourceClasses()) {
			if(resourceClass.getAnnotation(MainResource.class) != null){
				mainClass = resourceClass;
				break;
			}
		}
		
		// Save our injected globals
		AppGlobals globals = AppGlobals.get();
		globals.setDbClient(dbClient);
		globals.setMainClass(mainClass);

		return doOnPlugins(plugin -> plugin.init())
			.flatMap(v -> startVertx(config, deployment));
	}
	
	private Single<Void> doOnPlugins(Function<Plugin, Single<Void>> operation){
		Single<Void> last = Single.just(null);
		for(Plugin plugin : plugins) {
			last = last.flatMap(v -> operation.apply(plugin));
		}
		return last;
	}
	
	private Single<Void> startVertx(JsonObject config, VertxResteasyDeployment deployment) {
		Router router = Router.router(vertx);
		AppGlobals.get().setRouter(router);
		
		VertxPluginRequestHandler resteasyHandler = new VertxPluginRequestHandler(vertx, deployment, plugins);
		
		return doOnPlugins(plugin -> plugin.preRoute())
				.map(v -> {
					router.route().handler(routingContext -> {
						ResteasyProviderFactory.pushContext(RoutingContext.class, routingContext);
						resteasyHandler.handle(routingContext.request());
					});
					return null;
				}).flatMap(v -> doOnPlugins(plugin -> plugin.postRoute()))
				.flatMap(v -> {
					return Single.<Void>create(sub -> {
						// Start the front end server using the Jax-RS controller
						vertx.createHttpServer()
						.requestHandler(router::accept)
						.listen(config.getInteger("http_port", 9000), ar -> {
							if(ar.failed()){
								ar.cause().printStackTrace();
								sub.onError(ar.cause());
							}else {
								System.out.println("Server started on port "+ ar.result().actualPort());
								sub.onSuccess(null);
							}
						});
					});
				});
	}

	private Single<JsonObject> loadConfig(JsonObject config) {
		if(config != null) {
			AppGlobals.get().setConfig(config);
			return Single.just(config);
		}
		ConfigStoreOptions fileStore = new ConfigStoreOptions()
				.setType("file")
				.setConfig(new JsonObject().put("path", "conf/config.json"));

		ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions()
				.addStore(fileStore);

		ConfigRetriever retriever = ConfigRetriever.create(vertx, configRetrieverOptions);
		return retriever.rxGetConfig().map(loadedConfig -> {
			AppGlobals.get().setConfig(loadedConfig);
			return loadedConfig;
		});
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
		// FIXME: resource should be detected
		swaggerConfig.setResourcePackage("org.mygroup.vertxrs");
		swaggerConfig.setPrettyPrint(true);
		swaggerConfig.setScan(true);
		deployment.getRegistry().addPerInstanceResource(ApiListingResource.class);
		deployment.getProviderFactory().register(SwaggerSerializers.class);
	}

	private Single<VertxResteasyDeployment> setupResteasy() {
		// Build the Jax-RS hello world deployment
		VertxResteasyDeployment deployment = new VertxResteasyDeployment();
		deployment.getDefaultContextObjects().put(Vertx.class, AppGlobals.get().getVertx());
		deployment.getDefaultContextObjects().put(AppGlobals.class, AppGlobals.get());
		
		return doOnPlugins(plugin -> plugin.deployToResteasy(deployment)).map(v -> {
			try {
				deployment.start();
			}catch(ExceptionInInitializerError err) {
				// rxjava behaves badly on LinkageError
				rethrow(err.getCause());
			}
			return deployment;
		}).doOnError(t -> t.printStackTrace());
	}

	private <T extends Throwable> void rethrow(Throwable cause) throws T {
		throw (T)cause;
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

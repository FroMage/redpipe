package net.redpipe.engine.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Function;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.ReaderConfigUtils;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.config.ConfigRetriever;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.auth.AuthProvider;
import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLClient;
import io.vertx.rxjava.ext.web.Cookie;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.RoutingContext;
import io.vertx.rxjava.ext.web.Session;
import io.vertx.rxjava.ext.web.handler.CookieHandler;
import io.vertx.rxjava.ext.web.handler.SessionHandler;
import io.vertx.rxjava.ext.web.sstore.LocalSessionStore;
import net.redpipe.engine.dispatcher.VertxPluginRequestHandler;
import net.redpipe.engine.resteasy.RxVertxProvider;
import net.redpipe.engine.rxjava.ResteasyContextPropagatingOnSingleCreateAction;
import net.redpipe.engine.spi.Plugin;
import net.redpipe.engine.template.TemplateRenderer;
import rx.Single;
import rx.plugins.RxJavaHooks;

public class Server {
	
	private Vertx vertx;
	protected List<Plugin> plugins;
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    protected String configFile = "conf/config.json";

	public Server(){
//		System.setProperty("co.paralleluniverse.fibers.verifyInstrumentation", "true");
	}
	
	public Single<Void> start(){
		return start((JsonObject)null);
	}

	public Single<Void> start(Class<?>... resourceOrProviderClasses){
		return start(null, resourceOrProviderClasses);
	}
	
	public Single<Void> start(JsonObject defaultConfig, Class<?>... resourceOrProviderClasses){
		setupLogging();
		
		VertxOptions options = new VertxOptions();
		options.setWarningExceptionTime(Long.MAX_VALUE);
		vertx = Vertx.vertx(options);
		AppGlobals.init();
		AppGlobals.get().setVertx(vertx);

		// Propagate the Resteasy context on RxJava
		RxJavaHooks.setOnSingleCreate(new ResteasyContextPropagatingOnSingleCreateAction());

		JsonObject config = loadFileConfig(defaultConfig);
        AppGlobals.get().setConfig(config);

        return initVertx(config)
                .flatMap(vertx -> {
                    this.vertx = vertx;
                    AppGlobals.get().setVertx(this.vertx);
                    return setupPlugins();
                })
                .flatMap(v -> setupTemplateRenderers())
                .flatMap(v -> setupResteasy(resourceOrProviderClasses))
                .flatMap(deployment -> {
                    setupSwagger(deployment);
                    return setupVertx(deployment);
                });
	}

    private Single<Vertx> initVertx(JsonObject config)
    {
        VertxOptions options;
        if (config != null)
        {
            options = new VertxOptions(config);
        }
        else
        {
            options = new VertxOptions();
        }
        options.setWarningExceptionTime(Long.MAX_VALUE);
        if (options.isClustered())
        {
            return Vertx.rxClusteredVertx(options);
        }
        else
        {
            vertx = Vertx.vertx(options);
            return Single.just(vertx);
        }
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

    private Single<Void> setupVertx(VertxResteasyDeployment deployment) {
		// Get a DB
        SQLClient dbClient = createDbClient(AppGlobals.get().getConfig());

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
		globals.setDeployment(deployment);

		return doOnPlugins(plugin -> plugin.init())
			.flatMap(v -> startVertx(deployment));
	}
	
	protected SQLClient createDbClient(JsonObject config) {
		return JDBCClient.createNonShared(vertx, new JsonObject()
				.put("url", config.getString("db_url", "jdbc:hsqldb:file:db/wiki"))
				.put("driver_class", config.getString("db_driver", "org.hsqldb.jdbcDriver"))
				.put("max_pool_size", config.getInteger("db_max_pool_size", 30)));
	}

	private Single<Void> doOnPlugins(Function<Plugin, Single<Void>> operation){
		Single<Void> last = Single.just(null);
		for(Plugin plugin : plugins) {
			last = last.flatMap(v -> operation.apply(plugin));
		}
		return last;
	}

    private Single<Void> startVertx(VertxResteasyDeployment deployment)
    {
        Router router = Router.router(vertx);
        AppGlobals.get().setRouter(router);

        VertxPluginRequestHandler resteasyHandler = new VertxPluginRequestHandler(vertx, deployment, plugins);

        return doOnPlugins(plugin -> plugin.preRoute())
                .map(v -> {
                    setupRoutes(router);
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
                                .listen(AppGlobals.get().getConfig().getInteger("http_port", 9000), ar -> {
                                    if (ar.failed())
                                    {
                                        ar.cause().printStackTrace();
                                        sub.onError(ar.cause());
                                    }
                                    else
                                    {
                                        System.out.println("Server started on port " + ar.result().actualPort());
                                        sub.onSuccess(null);
                                    }
                                });
                    });
                });
    }

	protected void setupRoutes(Router router) {
		router.route().handler(CookieHandler.create());
		
		// Workaround for https://github.com/vert-x3/vertx-web/pull/880
		router.route().handler(context -> {
			context.addHeadersEndHandler(v -> {
			      Session session = context.session();
			      if (!session.isDestroyed()) {
			        final int currentStatusCode = context.response().getStatusCode();
			        // Store the session (only and only if there was no error)
			        if (currentStatusCode < 200 || currentStatusCode >= 400) {
			        	String previousValue = context.get("__REDPIPE_SAVED_COOKIE");
			        	if(previousValue != null) {
			        		io.netty.handler.codec.http.cookie.Cookie nettyCookie = ClientCookieDecoder.LAX.decode(previousValue);
			        		Cookie newCookie = Cookie.newInstance(io.vertx.ext.web.Cookie.cookie(nettyCookie));
			        		context.addCookie(newCookie);
			        	}
			        }
			      }
			});
			
			context.next();
		});
		SessionHandler sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));
		router.route().handler(sessionHandler);

		AuthProvider auth = setupAuthenticationRoutes();
		
		router.route().handler(context -> {
			
			// Workaround for https://github.com/vert-x3/vertx-web/pull/880
			context.addHeadersEndHandler(v -> {
			      Session session = context.session();
			      if (!session.isDestroyed()) {
			        final int currentStatusCode = context.response().getStatusCode();
			        // Store the session (only and only if there was no error)
			        if (currentStatusCode < 200 || currentStatusCode >= 400) {
			        	Cookie cookie = context.getCookie(io.vertx.ext.web.handler.SessionHandler.DEFAULT_SESSION_COOKIE_NAME);
			        	context.put("__REDPIPE_SAVED_COOKIE", cookie.encode());
			        }
			      }
			});
			
			ResteasyProviderFactory.pushContext(AuthProvider.class, auth);
			ResteasyProviderFactory.pushContext(User.class, context.user());
			ResteasyProviderFactory.pushContext(Session.class, context.session());
			context.next();
		});
	}

	protected AuthProvider setupAuthenticationRoutes() {
		return null;
	}

    private JsonObject loadFileConfig(JsonObject config)
    {
        if (config != null)
        {
            return config;
        }
        try
        {
            File current = new File(".").getCanonicalFile();
            // We need to use the canonical file. Without the file name is .
            System.setProperty("vertx.cwd", current.getAbsolutePath());
        }
        catch (Exception e)
        {
            // Ignore it.
        }

        String confArg = this.configFile;
        File file = new File(confArg);
        System.out.println(file.getAbsolutePath());
        try (Scanner scanner = new Scanner(new File(confArg)).useDelimiter("\\A"))
        {
            String sconf = scanner.next();
            try
            {
                return new JsonObject(sconf);
            }
            catch (DecodeException e)
            {
                log.error("Configuration file " + sconf + " does not contain a valid JSON object");
                // empty config
                return new JsonObject();
            }
        }
        catch (FileNotFoundException e)
        {
            return new JsonObject();
        }
    }

	private Single<JsonObject> loadConfig(JsonObject config) {
		if(config != null) {
			AppGlobals.get().setConfig(config);
			return Single.just(config);
		}
		
		String path = "conf/config.json";
		return vertx.fileSystem().rxExists(path)
				.flatMap(exists -> {
					if(exists) {
						ConfigStoreOptions fileStore = new ConfigStoreOptions()
								.setType("file")
								.setConfig(new JsonObject().put("path", path));

						ConfigRetrieverOptions configRetrieverOptions = new ConfigRetrieverOptions()
								.addStore(fileStore);

						ConfigRetriever retriever = ConfigRetriever.create(vertx, configRetrieverOptions);
						return retriever.rxGetConfig().map(loadedConfig -> {
							AppGlobals.get().setConfig(loadedConfig);
							return loadedConfig;
						});
					} else {
						// empty config
						JsonObject emptyConfig = new JsonObject();
						AppGlobals.get().setConfig(emptyConfig);
						return Single.just(emptyConfig);
					}
				});
	}

	private void setupSwagger(VertxResteasyDeployment deployment) {
		// Swagger
		ServletConfig servletConfig = new ServletConfig(){

			@Override
			public String getServletName() {
				return "pretend-servlet";
			}

			@Override
			public ServletContext getServletContext() {
				return RxVertxProvider.ServletContext;
			}

			@Override
			public String getInitParameter(String name) {
				return getServletContext().getInitParameter(name);
			}

			@Override
			public Enumeration<String> getInitParameterNames() {
				return getServletContext().getInitParameterNames();
			}
		};
		AppGlobals.get().setGlobal(ServletConfig.class, servletConfig);

		ReaderConfigUtils.initReaderConfig(servletConfig);

		BeanConfig swaggerConfig = new BeanConfig();
		swaggerConfig.setVersion("1.0");
		swaggerConfig.setSchemes(new String[]{"http"});
		swaggerConfig.setHost("localhost:"+AppGlobals.get().getConfig().getInteger("http_port", 9000));
		swaggerConfig.setBasePath("/");
		Set<String> resourcePackages = new HashSet<>();
		for (Class<?> klass : deployment.getActualResourceClasses()) {
			resourcePackages.add(klass.getPackage().getName());
		}
		swaggerConfig.setResourcePackage(String.join(",", resourcePackages));
		swaggerConfig.setPrettyPrint(true);
		swaggerConfig.setScan(true);
		
		deployment.getRegistry().addPerInstanceResource(ApiListingResource.class);
		deployment.getProviderFactory().register(SwaggerSerializers.class);
	}

	protected Single<VertxResteasyDeployment> setupResteasy(Class<?>... resourceOrProviderClasses) {
		// Build the Jax-RS hello world deployment
		VertxResteasyDeployment deployment = new VertxResteasyDeployment();
		deployment.getDefaultContextObjects().put(Vertx.class, AppGlobals.get().getVertx());
		deployment.getDefaultContextObjects().put(AppGlobals.class, AppGlobals.get());
		
		return doOnPlugins(plugin -> plugin.deployToResteasy(deployment)).map(v -> {
			for(Class<?> klass : resourceOrProviderClasses) {
				if(klass.isAnnotationPresent(Path.class))
					deployment.getActualResourceClasses().add(klass);
				if(klass.isAnnotationPresent(Provider.class))
					deployment.getActualProviderClasses().add(klass);
			}
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

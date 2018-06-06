package net.redpipe.engine.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.converter.ModelConverters;
import io.swagger.jaxrs.ext.SwaggerExtensions;
import io.swagger.models.Model;
import io.swagger.models.properties.Property;
import net.redpipe.engine.swagger.RxModelConverter;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.netty.handler.codec.http.cookie.ClientCookieDecoder;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.plugins.RxJavaPlugins;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.ReaderConfigUtils;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetServerOptions;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.AuthProvider;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLClient;
import io.vertx.reactivex.ext.web.Cookie;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.Session;
import io.vertx.reactivex.ext.web.handler.CookieHandler;
import io.vertx.reactivex.ext.web.handler.SessionHandler;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import net.redpipe.engine.dispatcher.VertxPluginRequestHandler;
import net.redpipe.engine.resteasy.RxVertxProvider;
import net.redpipe.engine.rxjava.ResteasyContextPropagatingOnObservableCreateAction;
import net.redpipe.engine.rxjava.ResteasyContextPropagatingOnSingleCreateAction;
import net.redpipe.engine.rxjava2.ContextPropagatorOnCompletableAssemblyAction;
import net.redpipe.engine.rxjava2.ContextPropagatorOnCompletableCreateAction;
import net.redpipe.engine.rxjava2.ContextPropagatorOnFlowableAssemblyAction;
import net.redpipe.engine.rxjava2.ContextPropagatorOnFlowableCreateAction;
import net.redpipe.engine.rxjava2.ContextPropagatorOnMaybeAssemblyAction;
import net.redpipe.engine.rxjava2.ContextPropagatorOnMaybeCreateAction;
import net.redpipe.engine.rxjava2.ContextPropagatorOnObservableAssemblyAction;
import net.redpipe.engine.rxjava2.ContextPropagatorOnObservableCreateAction;
import net.redpipe.engine.rxjava2.ContextPropagatorOnSingleAssemblyAction;
import net.redpipe.engine.rxjava2.ContextPropagatorOnSingleCreateAction;
import net.redpipe.engine.spi.Plugin;
import net.redpipe.engine.template.TemplateRenderer;
import rx.plugins.RxJavaHooks;

public class Server {
	
	private Vertx vertx;
	protected List<Plugin> plugins;
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    protected String configFile = "conf/config.json";

	public Server(){
//		System.setProperty("co.paralleluniverse.fibers.verifyInstrumentation", "true");
	}
	
	public Completable start(){
		return start((JsonObject)null);
	}

	public Completable start(Class<?>... resourceOrProviderClasses){
		return start(null, resourceOrProviderClasses);
	}
	
	public Completable start(JsonObject defaultConfig, Class<?>... resourceOrProviderClasses){
		return Single.<JsonObject>create(s -> {
			setupLogging();

			VertxOptions options = new VertxOptions();
			options.setWarningExceptionTime(Long.MAX_VALUE);
			AppGlobals.init();

			// Propagate the Resteasy context on RxJava1
			RxJavaHooks.setOnSingleCreate(new ResteasyContextPropagatingOnSingleCreateAction());
			RxJavaHooks.setOnObservableCreate(new ResteasyContextPropagatingOnObservableCreateAction());

			// Propagate the Resteasy context on RxJava2
			RxJavaPlugins.setOnSingleSubscribe(new ContextPropagatorOnSingleCreateAction());
			RxJavaPlugins.setOnCompletableSubscribe(new ContextPropagatorOnCompletableCreateAction());
			RxJavaPlugins.setOnFlowableSubscribe(new ContextPropagatorOnFlowableCreateAction());
			RxJavaPlugins.setOnMaybeSubscribe(new ContextPropagatorOnMaybeCreateAction());
			RxJavaPlugins.setOnObservableSubscribe(new ContextPropagatorOnObservableCreateAction());
			
			RxJavaPlugins.setOnSingleAssembly(new ContextPropagatorOnSingleAssemblyAction());
			RxJavaPlugins.setOnCompletableAssembly(new ContextPropagatorOnCompletableAssemblyAction());
			RxJavaPlugins.setOnFlowableAssembly(new ContextPropagatorOnFlowableAssemblyAction());
			RxJavaPlugins.setOnMaybeAssembly(new ContextPropagatorOnMaybeAssemblyAction());
			RxJavaPlugins.setOnObservableAssembly(new ContextPropagatorOnObservableAssemblyAction());

			JsonObject config = loadFileConfig(defaultConfig);
			AppGlobals.get().setConfig(config);
			s.onSuccess(config);
		})
				.flatMap(this::initVertx)
				.flatMapCompletable(vertx -> {
                    this.vertx = vertx;
                    AppGlobals.get().setVertx(this.vertx);
                    return setupPlugins();
                })
                .concatWith(setupTemplateRenderers())
                .andThen(setupResteasy(resourceOrProviderClasses))
                .flatMapCompletable(deployment -> {
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
	
	private Completable setupPlugins() {
		return Completable.defer(() -> {
			loadPlugins();
			return doOnPlugins(plugin -> plugin.preInit());
		});
	}

	protected void loadPlugins() {
		plugins = new ArrayList<Plugin>();
		for(Plugin plugin : ServiceLoader.load(Plugin.class))
			plugins.add(plugin);
	}

	private Completable setupTemplateRenderers() {
		return Completable.defer(() -> {
			List<TemplateRenderer> renderers = new ArrayList<>();
			for(TemplateRenderer renderer : ServiceLoader.load(TemplateRenderer.class))
				renderers.add(renderer);
			AppGlobals.get().setTemplateRenderers(renderers);
			return Completable.complete();
		});
	}

    private Completable setupVertx(VertxResteasyDeployment deployment) {
    	return Completable.defer(() -> {
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
    				.concatWith(startVertx(deployment));
    	});
	}
	
	protected SQLClient createDbClient(JsonObject config) {
		return JDBCClient.createNonShared(vertx, new JsonObject()
				.put("url", config.getString("db_url", "jdbc:hsqldb:file:db/wiki"))
				.put("driver_class", config.getString("db_driver", "org.hsqldb.jdbcDriver"))
				.put("max_pool_size", config.getInteger("db_max_pool_size", 30)));
	}

	private Completable doOnPlugins(Function<Plugin, Completable> operation){
		return Completable.defer(() -> {
			Completable last = Completable.complete();
			for(Plugin plugin : plugins) {
				last = last.concatWith(operation.apply(plugin));
			}
			return last;
		});
	}

    private Completable startVertx(VertxResteasyDeployment deployment)
    {
    	return Completable.defer(() -> {
    		Router router = Router.router(vertx);
    		AppGlobals globals = AppGlobals.get();
    		globals.setRouter(router);

    		VertxPluginRequestHandler resteasyHandler = new VertxPluginRequestHandler(vertx, deployment, plugins);

    		return doOnPlugins(plugin -> plugin.preRoute())
    				.doOnComplete(() -> {
    					setupRoutes(router);
    					router.route().handler(routingContext -> {
    						ResteasyProviderFactory.pushContext(RoutingContext.class, routingContext);
    						ResteasyProviderFactory.pushContext(io.vertx.rxjava.ext.web.RoutingContext.class, 
    								io.vertx.rxjava.ext.web.RoutingContext.newInstance(routingContext.getDelegate()));
    						resteasyHandler.handle(routingContext.request());
    					});
    				}).concatWith(doOnPlugins(plugin -> plugin.postRoute()))
    				.concatWith(Completable.defer(() -> {
    					// Start the front end server using the Jax-RS controller
    					int port = globals.getConfig().getInteger("http_port", 9000);
    					String host = globals.getConfig().getString("http_host", NetServerOptions.DEFAULT_HOST);
    					return vertx.createHttpServer()
    							.requestHandler(router::accept)
    							.rxListen(port, host)
    							.doOnSuccess(server -> System.out.println("Server started on port " + server.actualPort()))
    							.doOnError(t -> t.printStackTrace())
    							.ignoreElement();
    				}));
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
			
			// rx2
			ResteasyProviderFactory.pushContext(AuthProvider.class, auth);
			ResteasyProviderFactory.pushContext(User.class, context.user());
			ResteasyProviderFactory.pushContext(Session.class, context.session());
			// rx1
			ResteasyProviderFactory.pushContext(io.vertx.rxjava.ext.auth.AuthProvider.class, 
					auth != null ? io.vertx.rxjava.ext.auth.AuthProvider.newInstance(auth.getDelegate()) : null);
			ResteasyProviderFactory.pushContext(io.vertx.rxjava.ext.auth.User.class, 
					context.user() != null ? io.vertx.rxjava.ext.auth.User.newInstance(context.user().getDelegate()) : null);
			ResteasyProviderFactory.pushContext(io.vertx.rxjava.ext.web.Session.class, 
					context.session() != null ? io.vertx.rxjava.ext.web.Session.newInstance(context.session().getDelegate()) : null);
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
		ModelConverters.getInstance().addConverter(new RxModelConverter());
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

		BeanConfig swaggerConfig = new MyBeanConfig();
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
		return Single.defer(() -> {
			// Build the Jax-RS hello world deployment
			VertxResteasyDeployment deployment = new VertxResteasyDeployment();
			deployment.getDefaultContextObjects().put(Vertx.class, AppGlobals.get().getVertx());
			deployment.getDefaultContextObjects().put(AppGlobals.class, AppGlobals.get());

			return doOnPlugins(plugin -> plugin.deployToResteasy(deployment)).toSingle(() -> {
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
		});
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

	public Completable close() {
		return doOnPlugins(plugin -> plugin.shutdown())
				.concatWith(vertx.rxClose());
	}

	public Vertx getVertx() {
		return vertx;
	}

	public static void main(String[] args) {
		Server test = new Server();
		test.start();
	}
}

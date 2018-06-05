package net.redpipe.example.wiki.keycloakJooq;

import org.jooq.Configuration;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultConfiguration;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.core.Server;
import net.redpipe.engine.spi.Plugin;
import net.redpipe.example.wiki.keycloakJooq.jooq.tables.daos.PagesDao;

import com.github.rjeschke.txtmark.Processor;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.asyncsql.AsyncSQLClient;
import io.vertx.reactivex.ext.asyncsql.PostgreSQLClient;
import io.vertx.reactivex.ext.auth.AuthProvider;
import io.vertx.reactivex.ext.auth.jwt.JWTAuth;
import io.vertx.reactivex.ext.auth.oauth2.OAuth2Auth;
import io.vertx.reactivex.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.reactivex.ext.sql.SQLClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.JWTAuthHandler;
import io.vertx.reactivex.ext.web.handler.OAuth2AuthHandler;
import io.vertx.reactivex.ext.web.handler.UserSessionHandler;
import io.vertx.reactivex.ext.web.handler.sockjs.SockJSHandler;

public class WikiServer extends Server {

	@Override
	protected AuthProvider setupAuthenticationRoutes() {
		JsonObject keycloackConfig = AppGlobals.get().getConfig().getJsonObject("keycloack");
		OAuth2Auth authWeb = KeycloakAuth.create(AppGlobals.get().getVertx(), keycloackConfig);
		OAuth2Auth authApi = KeycloakAuth.create(AppGlobals.get().getVertx(), OAuth2FlowType.PASSWORD, keycloackConfig);
		
		// FIXME: URL
		OAuth2AuthHandler authHandler = OAuth2AuthHandler.create((OAuth2Auth) authWeb, "http://localhost:9000/callback");
		Router router = AppGlobals.get().getRouter();
		// FIXME: crazy!!
		AuthProvider authProvider = AuthProvider.newInstance(authWeb.getDelegate());
		router.route().handler(UserSessionHandler.create(authProvider));

		authHandler.setupCallback(router.get("/callback"));
		
		JWTAuth jwtAuth = JWTAuth.create(AppGlobals.get().getVertx(), new JWTAuthOptions(new JsonObject()
				.put("keyStore", AppGlobals.get().getConfig().getJsonObject("keystore"))));
		AppGlobals.get().setGlobal(JWTAuth.class, jwtAuth);
		
		JWTAuthHandler jwtAuthHandler = JWTAuthHandler.create(jwtAuth, "/wiki/api/token");

		// FIXME: just use different routers
		router.route().handler(ctx -> {
			if(!ctx.request().uri().startsWith("/wiki/api/"))
				authHandler.handle(ctx);
			else
				jwtAuthHandler.handle(ctx);
		});
		
		return AuthProvider.newInstance(authApi.getDelegate());
	}
	
	@Override
	protected void loadPlugins() {
		super.loadPlugins();
		plugins.add(new Plugin() {
			@Override
			public Completable preRoute() {
				return Completable.defer(() -> {
					AppGlobals globals = AppGlobals.get();

					SockJSHandler sockJSHandler = SockJSHandler.create(globals.getVertx());
					BridgeOptions bridgeOptions = new BridgeOptions()
							.addInboundPermitted(new PermittedOptions().setAddress("app.markdown"))
							.addOutboundPermitted(new PermittedOptions().setAddress("page.saved"));
					sockJSHandler.bridge(bridgeOptions);
					globals.getRouter().route("/eventbus/*").handler(sockJSHandler);

					return super.preRoute();
				});
			}
		});
	}

	@Override
	protected SQLClient createDbClient(JsonObject config) {
		JsonObject myConfig = new JsonObject();
		if(config.containsKey("db_host"))
			myConfig.put("host", config.getString("db_host"));
		if(config.containsKey("db_port"))
			myConfig.put("port", config.getInteger("db_port"));
		if(config.containsKey("db_user"))
			myConfig.put("username", config.getString("db_user"));
		if(config.containsKey("db_pass"))
			myConfig.put("password", config.getString("db_pass"));
		if(config.containsKey("db_name"))
			myConfig.put("database", config.getString("db_name"));
		myConfig.put("max_pool_size", config.getInteger("db_max_pool_size", 30));
		
		Vertx vertx = AppGlobals.get().getVertx();
		AsyncSQLClient dbClient = PostgreSQLClient.createNonShared(vertx, myConfig);

		Configuration configuration = new DefaultConfiguration();
		configuration.set(SQLDialect.POSTGRES);

		PagesDao dao = new PagesDao(configuration, dbClient);
		
		AppGlobals.get().setGlobal("dao", dao);
		
		return dbClient;
	}
	
	@Override
	public Completable start(JsonObject defaultConfig, Class<?>... resourcesOrProviders) {
		return super.start(defaultConfig, resourcesOrProviders)
				.doOnComplete(() -> {
					AppGlobals globals = AppGlobals.get();
					globals.getVertx().eventBus().<String>consumer("app.markdown", msg -> {
						  String html = Processor.process(msg.body());
						  msg.reply(html);
						});
				});
	}
}

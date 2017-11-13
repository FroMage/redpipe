package org.vertxrs.example.wiki.shiroJdbc;

import javax.ws.rs.core.HttpHeaders;

import org.vertxrs.engine.core.AppGlobals;
import org.vertxrs.engine.core.Server;
import org.vertxrs.engine.db.SQLUtil;
import org.vertxrs.engine.spi.Plugin;

import com.github.rjeschke.txtmark.Processor;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.rxjava.ext.auth.AuthProvider;
import io.vertx.rxjava.ext.auth.jwt.JWTAuth;
import io.vertx.rxjava.ext.auth.shiro.ShiroAuth;
import io.vertx.rxjava.ext.web.handler.JWTAuthHandler;
import io.vertx.rxjava.ext.web.handler.UserSessionHandler;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import rx.Single;

public class WikiServer extends Server {

	@Override
	protected AuthProvider setupAuthenticationRoutes() {
		AppGlobals globals = AppGlobals.get();
		AuthProvider auth = ShiroAuth.create(globals.getVertx(), new ShiroAuthOptions()
				.setType(ShiroAuthRealmType.PROPERTIES)
				.setConfig(new JsonObject()
						.put("properties_path", globals.getConfig().getString("security_definitions"))));
		
		globals.getRouter().route().handler(UserSessionHandler.create(auth));

		
		JsonObject keyStoreOptions = new JsonObject().put("keyStore", globals.getConfig().getJsonObject("keystore"));
		
		// attempt to load a Key file
		JWTAuth jwtAuth = JWTAuth.create(globals.getVertx(), new JWTAuthOptions(keyStoreOptions));
		JWTAuthHandler jwtAuthHandler = JWTAuthHandler.create(jwtAuth);

		globals.setGlobal(JWTAuth.class, jwtAuth);
		globals.getRouter().route().handler(context -> {
			// only filter if we have a header, otherwise it will try to force auth, regardless if whether
			// we want auth
			if(context.request().getHeader(HttpHeaders.AUTHORIZATION) != null)
				jwtAuthHandler.handle(context);
			else
				context.next();
		});

		return auth;
	}
	
	@Override
	protected void loadPlugins() {
		super.loadPlugins();
		plugins.add(new Plugin() {
			@Override
			public Single<Void> preRoute() {
				AppGlobals globals = AppGlobals.get();
				
				SockJSHandler sockJSHandler = SockJSHandler.create(globals.getVertx());
				BridgeOptions bridgeOptions = new BridgeOptions()
				  .addInboundPermitted(new PermittedOptions().setAddress("app.markdown"))
				  .addOutboundPermitted(new PermittedOptions().setAddress("page.saved"));
				sockJSHandler.bridge(bridgeOptions);
				globals.getRouter().route("/eventbus/*").handler(sockJSHandler);

				return super.preRoute();
			}
		});
	}

	@Override
	public Single<Void> start(JsonObject defaultConfig, Class<?>... resourcesOrProviders) {
		return super.start(defaultConfig, resourcesOrProviders)
				.map(v -> {
					AppGlobals globals = AppGlobals.get();
					globals.getVertx().eventBus().<String>consumer("app.markdown", msg -> {
						  String html = Processor.process(msg.body());
						  msg.reply(html);
						});
					return null;
				})
				.flatMap(v -> SQLUtil.doInConnection(conn -> conn.rxExecute(SQL.SQL_CREATE_PAGES_TABLE)));
	}
}

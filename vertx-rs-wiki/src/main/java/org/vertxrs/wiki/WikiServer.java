package org.vertxrs.wiki;

import org.vertxrs.engine.core.AppGlobals;
import org.vertxrs.engine.core.Server;
import org.vertxrs.engine.db.SQLUtil;
import org.vertxrs.engine.spi.Plugin;

import com.github.rjeschke.txtmark.Processor;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.rxjava.ext.web.handler.sockjs.SockJSHandler;
import rx.Single;

public class WikiServer extends Server {
	
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
	public Single<Void> start(JsonObject defaultConfig) {
		return super.start(defaultConfig)
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

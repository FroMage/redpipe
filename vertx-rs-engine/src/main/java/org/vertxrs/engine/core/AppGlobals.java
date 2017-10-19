package org.vertxrs.engine.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.vertxrs.engine.template.TemplateRenderer;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import io.vertx.rxjava.ext.web.Router;
import rx.Single;

public class AppGlobals {
	
	private static ThreadLocal<AppGlobals> global = new ThreadLocal<AppGlobals>();

	static void init() {
		global.set(new AppGlobals());
	}

	public static AppGlobals get() {
		return global.get();
	}

	public static AppGlobals set(AppGlobals globals) {
		AppGlobals old = global.get();
		global.set(globals);
		return old;
	}

	private JsonObject config;
	private JDBCClient dbClient;
	private Class<?> mainClass;
	private Vertx vertx;
	private Router router;
	private List<TemplateRenderer> templateRenderers;
	private Map<String, Object> globals = new HashMap<>();
	
	public JsonObject getConfig() {
		return config;
	}

	void setConfig(JsonObject config) {
		this.config = config;
	}

	public JDBCClient getDbClient() {
		return dbClient;
	}

	void setDbClient(JDBCClient dbClient) {
		this.dbClient = dbClient;
	}

	public Class<?> getMainClass() {
		return mainClass;
	}
	
	void setMainClass(Class<?> mainClass) {
		this.mainClass = mainClass;
	}

	public Single<SQLConnection> getDbConnection(){
		return dbClient.rxGetConnection();
	}

	void setVertx(Vertx vertx) {
		this.vertx = vertx;
	}

	public Vertx getVertx() {
		return vertx;
	}

	void setRouter(Router router) {
		this.router = router;
	}
	
	public Router getRouter() {
		return router;
	}

	void setTemplateRenderers(List<TemplateRenderer> renderers) {
		this.templateRenderers = renderers;
	}
	
	public TemplateRenderer getTemplateRenderer(String name) {
		for(TemplateRenderer renderer : templateRenderers) {
			if(renderer.supportsTemplate(name))
				return renderer;
		}
		return null;
	}

	public void setGlobal(String key, Object value) {
		globals.put(key, value);
	}
	
	public Object getGlobal(String key) {
		return globals.get(key);
	}
}

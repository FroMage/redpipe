package org.vertxrs.engine.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.vertxrs.engine.template.TemplateRenderer;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.sql.SQLClient;
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
	private SQLClient dbClient;
	private Class<?> mainClass;
	private Vertx vertx;
	private Router router;
	private List<TemplateRenderer> templateRenderers;
	private Map<String, Object> namedGlobals = new HashMap<>();
	private Map<Class<?>, Object> typedGlobals = new HashMap<>();
	
	public JsonObject getConfig() {
		return config;
	}

	void setConfig(JsonObject config) {
		this.config = config;
	}

	public SQLClient getDbClient() {
		return dbClient;
	}

	void setDbClient(SQLClient dbClient) {
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
		namedGlobals.put(key, value);
	}
	
	public Object getGlobal(String key) {
		return namedGlobals.get(key);
	}
	
	public <T> void setGlobal(Class<T> klass, T value){
		typedGlobals.put(klass, value);
	}

	public <T> T getGlobal(Class<T> klass){
		return (T) typedGlobals.get(klass);
	}
	
	public void injectGlobals() {
		for (Entry<Class<?>, Object> entry : typedGlobals.entrySet()) {
			ResteasyProviderFactory.pushContext((Class)entry.getKey(), entry.getValue());
		}
	}
}

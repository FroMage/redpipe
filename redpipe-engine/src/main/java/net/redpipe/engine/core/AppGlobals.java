package net.redpipe.engine.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import net.redpipe.engine.mail.Mailer;
import net.redpipe.engine.mail.MockMailer;
import net.redpipe.engine.mail.ProdMailer;
import net.redpipe.engine.template.TemplateRenderer;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.sql.SQLClient;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.ext.web.Router;

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
	private VertxResteasyDeployment deployment;
	private Mailer mailer;
	private Mode mode = Mode.DEV;
	
	public JsonObject getConfig() {
		return config;
	}

	void setConfig(JsonObject config) {
		this.config = config;
		mode = Mode.valueOf(config.getString("mode", "dev").toUpperCase());
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
		// FIXME: inject using the more precise Type using the new resteasy injection API
		for (Entry<Class<?>, Object> entry : typedGlobals.entrySet()) {
			ResteasyProviderFactory.pushContext((Class)entry.getKey(), entry.getValue());
		}
	}

	public void setDeployment(VertxResteasyDeployment deployment) {
		this.deployment = deployment;
	}
	
	public VertxResteasyDeployment getDeployment() {
		return deployment;
	}

	public Mailer getMailer() {
		if(mailer == null) {
			if(mode == Mode.PROD)
				mailer = new ProdMailer(vertx, config);
			else
				mailer = new MockMailer();
		}
		return mailer;
	}
}

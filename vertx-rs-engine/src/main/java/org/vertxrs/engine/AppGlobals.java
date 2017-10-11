package org.vertxrs.engine;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
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
}

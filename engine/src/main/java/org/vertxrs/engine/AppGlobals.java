package org.vertxrs.engine;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Single;

@ApplicationScoped
public class AppGlobals {

	private JsonObject config;
	private JDBCClient dbClient;
	private Class<?> mainClass;
	
	public void init(JsonObject config, JDBCClient dbClient, Class<?> mainClass) {
		this.config = config;
		this.dbClient = dbClient;
		this.mainClass = mainClass;
	}

	@Produces @Config
	public JsonObject getConfig() {
		return config;
	}

	@Produces
	public JDBCClient getDbClient() {
		return dbClient;
	}

	@Produces
	public Class getMainClass() {
		return mainClass;
	}

	@Produces
	public Single<SQLConnection> getDbConnection(){
		return dbClient.rxGetConnection();
	}
}

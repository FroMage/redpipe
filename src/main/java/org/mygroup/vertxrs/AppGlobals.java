package org.mygroup.vertxrs;

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
	
	public void init(JsonObject config, JDBCClient dbClient) {
		this.config = config;
		this.dbClient = dbClient;
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
	public Single<SQLConnection> getDbConnection(){
		return dbClient.rxGetConnection();
	}
}

package net.redpipe.engine.db;

import net.redpipe.engine.core.AppGlobals;

import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Single;
import rx.functions.Func1;

public class SQLUtil {
	public static Single<SQLConnection> getConnection(){
		return AppGlobals.get().getDbConnection();
	}
	
	public static <T> Single<T> doInConnection(Func1<? super SQLConnection, ? extends Single<T>> func){
		Single<SQLConnection> connection = getConnection();
		return connection.flatMap(conn -> {
			return func.call(conn).doAfterTerminate(() -> {
				conn.close();
			});
		});
	}

}

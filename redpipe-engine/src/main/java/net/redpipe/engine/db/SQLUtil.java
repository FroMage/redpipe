package net.redpipe.engine.db;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.reactivex.ext.sql.SQLConnection;
import net.redpipe.engine.core.AppGlobals;
import rx.functions.Func1;


public class SQLUtil {
	public static Single<SQLConnection> getConnection(){
		return AppGlobals.get().getDbConnection();
	}
	
	public static <T> Single<T> doInConnection(Func1<? super SQLConnection, ? extends Single<T>> func){
		return Single.defer(() -> {
			Single<SQLConnection> connection = getConnection();
			return connection.flatMap(conn -> {
				return func.call(conn).doAfterTerminate(() -> {
					conn.close();
				});
			});
		});
	}

	public static Completable doInConnectionCompletable(Func1<? super SQLConnection, ? extends Completable> func){
		return Completable.defer(() -> {
			Single<SQLConnection> connection = getConnection();
			return connection.flatMapCompletable(conn -> {
				return func.call(conn).doAfterTerminate(() -> {
					conn.close();
				});
			});
		});
	}

}

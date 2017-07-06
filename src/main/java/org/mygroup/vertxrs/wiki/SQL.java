package org.mygroup.vertxrs.wiki;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;

import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Single;
import rx.functions.Func1;

public class SQL {
	static final String SQL_CREATE_PAGES_TABLE = "create table if not exists Pages (Id integer identity primary key, Name varchar(255) unique, Content clob)";
	static final String SQL_GET_PAGE = "select Id, Content from Pages where Name = ?";
	static final String SQL_CREATE_PAGE = "insert into Pages values (NULL, ?, ?)";
	static final String SQL_SAVE_PAGE = "update Pages set Content = ? where Id = ?";
	static final String SQL_ALL_PAGES = "select Name from Pages";
	static final String SQL_DELETE_PAGE = "delete from Pages where Id = ?";

	static <T> Single<T> doInConnection(Func1<? super SQLConnection, ? extends Single<T>> func){
		Single<SQLConnection> connection = CDI.current().select(new TypeLiteral<Single<SQLConnection>>(){}).get();
		return connection.flatMap(conn -> {
			return func.call(conn).doAfterTerminate(() -> {
				System.err.println("Closed connection");
				conn.close();
			});
		});
	}

}

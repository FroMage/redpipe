package org.vertxrs.wiki;

import org.vertxrs.db.SQLUtil;
import org.vertxrs.engine.Server;

public class Main {
	public static void main(String[] args) {
		Server test = new Server();
		// FIXME: this really needs to be done before we start serving pages
		test.start()
			.flatMap(v -> SQLUtil.doInConnection(conn -> conn.rxExecute(SQL.SQL_CREATE_PAGES_TABLE)))
			.subscribe(yes -> System.err.println("INIT done"), 
				x -> {
					x.printStackTrace();
				});
	}
}

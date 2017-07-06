package org.mygroup.vertxrs.wiki;

import javax.enterprise.event.Observes;

import org.jboss.weld.vertx.VertxConsumer;
import org.jboss.weld.vertx.VertxEvent;

public class InitListener {
	
	void onInit(@Observes @VertxConsumer("vertxrs.init") VertxEvent event) {
		System.err.println("INIT!!");
		SQL.doInConnection(conn -> conn.rxExecute(SQL.SQL_CREATE_PAGES_TABLE));
    }
	
}

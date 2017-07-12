package org.mygroup.vertxrs.wiki;

import javax.enterprise.event.Observes;

import org.jboss.weld.vertx.VertxConsumer;
import org.jboss.weld.vertx.VertxEvent;

public class InitListener {
	
	void onInit(@Observes @VertxConsumer("vertxrs.init") VertxEvent event) {
		System.err.println("INIT!!");
		// FIXME: this really needs to be doen before we start serving pages
		SQL.doInConnection(conn -> conn.rxExecute(SQL.SQL_CREATE_PAGES_TABLE))
		.subscribe(yes -> System.err.println("INIT done"), 
				x -> {
					x.printStackTrace();
				});
    }
	
}

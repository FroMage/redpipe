package org.vertxrs.wiki;

import org.vertxrs.engine.core.Server;

public class Main {
	public static void main(String[] args) {
		Server test = new WikiServer();
		// FIXME: this really needs to be done before we start serving pages
		test.start()
			.subscribe(yes -> System.err.println("INIT done"), 
				x -> {
					x.printStackTrace();
				});
	}
}

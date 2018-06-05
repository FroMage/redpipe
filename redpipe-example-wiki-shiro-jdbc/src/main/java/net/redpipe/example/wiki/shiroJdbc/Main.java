package net.redpipe.example.wiki.shiroJdbc;

import net.redpipe.engine.core.Server;

public class Main {
	public static void main(String[] args) {
		Server test = new WikiServer();
		// FIXME: this really needs to be done before we start serving pages
		test.start()
			.subscribe(() -> System.err.println("INIT done"), 
				x -> {
					x.printStackTrace();
				});
	}
}

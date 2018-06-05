package net.redpipe.engine.spi;

import java.io.IOException;

import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.resteasy.spi.HttpRequest;

import io.reactivex.Completable;


public abstract class Plugin {
	public Completable preInit(){ return Completable.complete(); }
	
	public Completable init(){ return Completable.complete(); }
	public Completable shutdown(){ return Completable.complete(); }

	public Completable deployToResteasy(VertxResteasyDeployment deployment){ return Completable.complete(); }
	
	// FIXME: Looks out of place
	public void aroundRequest(HttpRequest req, RunnableWithException<IOException> continuation) throws IOException{
		continuation.run();
	}

	public Completable preRoute() { return Completable.complete(); }
	public Completable postRoute() { return Completable.complete(); }
}

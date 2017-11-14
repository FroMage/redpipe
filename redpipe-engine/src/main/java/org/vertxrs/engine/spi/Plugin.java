package org.vertxrs.engine.spi;

import java.io.IOException;

import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.resteasy.spi.HttpRequest;

import rx.Single;

public abstract class Plugin {
	public Single<Void> preInit(){ return Single.just(null); }
	
	public Single<Void> init(){ return Single.just(null); }

	public Single<Void> deployToResteasy(VertxResteasyDeployment deployment){ return Single.just(null); }
	
	// FIXME: Looks out of place
	public void aroundRequest(HttpRequest req, RunnableWithException<IOException> continuation) throws IOException{
		continuation.run();
	}

	public Single<Void> preRoute() { return Single.just(null); }
	public Single<Void> postRoute() { return Single.just(null); }
}

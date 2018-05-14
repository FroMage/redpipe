package net.redpipe.discovery;

import java.io.File;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.kubernetes.KubernetesServiceImporter;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.spi.Plugin;
import rx.Single;

public class DiscoveryPlugin extends Plugin {
	@Override
	public Single<Void> init() {
		ServiceDiscovery discovery = ServiceDiscovery.create(AppGlobals.get().getVertx());
		if(new File("/var/run/secrets/kubernetes.io/serviceaccount/token").exists())
			discovery.getDelegate().registerServiceImporter(new KubernetesServiceImporter(), new JsonObject());
		AppGlobals.get().setGlobal(ServiceDiscovery.class, discovery);
		return super.init();
	}
	
	@Override
	public Single<Void> preRoute() {
		// TODO Auto-generated method stub
		return super.preRoute();
	}
}

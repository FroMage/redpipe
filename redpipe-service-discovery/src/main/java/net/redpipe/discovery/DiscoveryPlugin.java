package net.redpipe.discovery;

import java.io.File;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.kubernetes.KubernetesServiceImporter;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.spi.Plugin;

public class DiscoveryPlugin extends Plugin {
	@Override
	public Completable init() {
		return Completable.defer(() -> {
			ServiceDiscovery discovery = ServiceDiscovery.create(AppGlobals.get().getVertx());
			if(new File("/var/run/secrets/kubernetes.io/serviceaccount/token").exists())
				discovery.getDelegate().registerServiceImporter(new KubernetesServiceImporter(), new JsonObject());
			AppGlobals.get().setGlobal(ServiceDiscovery.class, discovery);
			return super.init();
		});
	}
}

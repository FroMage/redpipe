package net.redpipe.discovery;

import java.io.File;
import java.lang.reflect.Method;

import javax.ws.rs.Path;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetServerOptions;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.types.HttpEndpoint;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.kubernetes.KubernetesServiceImporter;
import net.redpipe.engine.core.AppGlobals;
import net.redpipe.engine.spi.Plugin;

public class DiscoveryPlugin extends Plugin {
	@Override
	public Completable init() {
		return Completable.defer(() -> {
			AppGlobals globals = AppGlobals.get();
			ServiceDiscovery discovery = ServiceDiscovery.create(globals.getVertx());
			if(new File("/var/run/secrets/kubernetes.io/serviceaccount/token").exists())
				discovery.getDelegate().registerServiceImporter(new KubernetesServiceImporter(), new JsonObject());
			globals.setGlobal(ServiceDiscovery.class, discovery);

			// FIXME: DRY
			int port = globals.getConfig().getInteger("http_port", 9000);
			String host = globals.getConfig().getString("http_host", NetServerOptions.DEFAULT_HOST);
			// that's a useless host name to publish
			if("0.0.0.0".equals(host))
				host = "localhost"; // only slightly less useless
			Completable records = Completable.complete();
			for (Class<?> klass : globals.getDeployment().getActualResourceClasses()) {
				ServiceName serviceName = klass.getAnnotation(ServiceName.class);
				Path path = klass.getAnnotation(Path.class);
				if(serviceName != null) {
					Record record = HttpEndpoint.createRecord(serviceName.value(), host, port, path.value());
					records = records.andThen(discovery.rxPublish(record).ignoreElement());
				}
				for (Method method : klass.getDeclaredMethods()) {
					ServiceName methodServiceName = method.getAnnotation(ServiceName.class);
					Path methodPath = method.getAnnotation(Path.class);
					if(methodServiceName != null) {
						String methodPathString = path.value();
						if(methodPath != null) {
							if(!methodPathString.endsWith("/"))
								methodPathString += "/";
							methodPathString += methodPath.value();
						}
						Record record = HttpEndpoint.createRecord(methodServiceName.value(), host, port, methodPathString);
						records = records.andThen(discovery.rxPublish(record).ignoreElement());
					}
				}
			}
			return records.andThen(super.init());
		});
	}
}

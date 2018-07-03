package net.redpipe.discovery;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ContextInjector;

import io.reactivex.Single;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import net.redpipe.engine.core.AppGlobals;

@Provider
public class DiscoveryWebClientInjector implements ContextInjector<Single<WebClient>,WebClient> {

	@Override
	public Single<WebClient> resolve(Class<? extends Single<WebClient>> rawType, Type genericType, Annotation[] annotations) {
		ServiceDiscovery discovery = AppGlobals.get().getGlobal(ServiceDiscovery.class);
		for (Annotation annotation : annotations) {
			if(annotation.annotationType() == ServiceName.class) {
				String serviceName = ((ServiceName) annotation).value();
				// FIXME: release client
				return discovery.rxGetRecord(record -> record.getName().equals(serviceName))
						.map(record -> discovery.getReference(record).getAs(WebClient.class));
			}
		}
		return null;
	}

}

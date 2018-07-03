package net.redpipe.discovery;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ContextInjector;

import io.reactivex.Single;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.Record;
import net.redpipe.engine.core.AppGlobals;

@Provider
public class DiscoveryRecordInjector implements ContextInjector<Single<Record>,Record> {

	@Override
	public Single<Record> resolve(Class<? extends Single<Record>> rawType, Type genericType, Annotation[] annotations) {
		ServiceDiscovery discovery = AppGlobals.get().getGlobal(ServiceDiscovery.class);
		for (Annotation annotation : annotations) {
			if(annotation.annotationType() == ServiceName.class) {
				String serviceName = ((ServiceName) annotation).value();
				return discovery.rxGetRecord(record -> record.getName().equals(serviceName));
			}
		}
		return null;
	}

}

package net.redpipe.cdi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.eventbus.EventBus;
import net.redpipe.engine.core.AppGlobals;

@ApplicationScoped
public class CdiGlobalProvider {

	@Produces
	public EventBus bus() {
		return AppGlobals.get().getVertx().eventBus();
	}

	@Produces
	public Vertx vertx() {
		return AppGlobals.get().getVertx();
	}

	@Produces
	public JsonObject config() {
		return AppGlobals.get().getConfig();
	}

	@Produces
	public AppGlobals appGlobals() {
		return AppGlobals.get();
	}
}

package org.vertxrs.security;

import java.lang.annotation.Annotation;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.vertx.rxjava.ext.auth.User;
import io.vertx.rxjava.ext.web.RoutingContext;
import rx.Single;

public abstract class AuthorizingAnnotationHandler {

	protected User getUser() {
		RoutingContext ctx = ResteasyProviderFactory.getContextData(RoutingContext.class);
		return ctx.user();
	}

	public abstract Single<Boolean> assertAuthorized(Annotation authzSpec);

}

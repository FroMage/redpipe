package org.mygroup.vertxrs.security;

import java.lang.annotation.Annotation;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

public abstract class AuthorizingAnnotationHandler {

	protected User getUser() {
		return ResteasyProviderFactory.getContextData(User.class);
	}

	public abstract void assertAuthorized(Annotation authzSpec);

}

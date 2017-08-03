package org.mygroup.vertxrs.security;

import org.apache.shiro.authz.aop.AuthenticatedAnnotationHandler;
import org.apache.shiro.subject.Subject;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class VertxAuthenticatedAnnotationHandler extends AuthenticatedAnnotationHandler {

	@Override
	protected Subject getSubject() {
		return ResteasyProviderFactory.getContextData(Subject.class);
	}

}

package org.mygroup.vertxrs.security;

import org.apache.shiro.authz.aop.UserAnnotationHandler;
import org.apache.shiro.subject.Subject;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class VertxUserAnnotationHandler extends UserAnnotationHandler {

	@Override
	protected Subject getSubject() {
		return ResteasyProviderFactory.getContextData(Subject.class);
	}

}

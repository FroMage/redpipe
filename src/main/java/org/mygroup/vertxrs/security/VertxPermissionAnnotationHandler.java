package org.mygroup.vertxrs.security;

import org.apache.shiro.authz.aop.PermissionAnnotationHandler;
import org.apache.shiro.subject.Subject;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class VertxPermissionAnnotationHandler extends PermissionAnnotationHandler {

	@Override
	protected Subject getSubject() {
		return ResteasyProviderFactory.getContextData(Subject.class);
	}
}

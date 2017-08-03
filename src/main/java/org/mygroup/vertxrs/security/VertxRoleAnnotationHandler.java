package org.mygroup.vertxrs.security;

import org.apache.shiro.authz.aop.RoleAnnotationHandler;
import org.apache.shiro.subject.Subject;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class VertxRoleAnnotationHandler extends RoleAnnotationHandler {

	@Override
	protected Subject getSubject() {
		return ResteasyProviderFactory.getContextData(Subject.class);
	}

}

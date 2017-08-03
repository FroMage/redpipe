package org.mygroup.vertxrs.security;

import org.apache.shiro.authz.aop.GuestAnnotationHandler;
import org.apache.shiro.subject.Subject;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class VertxGuestAnnotationHandler extends GuestAnnotationHandler {

	@Override
	protected Subject getSubject() {
		return ResteasyProviderFactory.getContextData(Subject.class);
	}

}

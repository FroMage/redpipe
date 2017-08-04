package org.mygroup.vertxrs.security;

import java.lang.annotation.Annotation;

public class VertxUserAnnotationHandler extends AuthorizingAnnotationHandler {

	@Override
	public void assertAuthorized(Annotation authzSpec) {
		if(authzSpec instanceof RequiresUser){
			User user = getUser();
			if(user == null)
				throw new AuthenticationException("User required");
		}
	}
}

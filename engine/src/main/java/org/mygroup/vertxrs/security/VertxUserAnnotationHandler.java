package org.mygroup.vertxrs.security;

import java.lang.annotation.Annotation;

import io.vertx.rxjava.ext.auth.User;
import rx.Single;

public class VertxUserAnnotationHandler extends AuthorizingAnnotationHandler {

	@Override
	public Single<Boolean> assertAuthorized(Annotation authzSpec) {
		if(authzSpec instanceof RequiresUser){
			User user = getUser();
			if(user == null)
				return Single.error(new AuthenticationException("User required"));
		}
		return Single.just(true);
	}
}

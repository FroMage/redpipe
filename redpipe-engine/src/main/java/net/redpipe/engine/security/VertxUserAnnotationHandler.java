package net.redpipe.engine.security;

import java.lang.annotation.Annotation;

import io.reactivex.Single;
import io.vertx.reactivex.ext.auth.User;

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

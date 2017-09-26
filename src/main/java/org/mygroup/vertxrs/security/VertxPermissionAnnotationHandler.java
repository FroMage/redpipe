package org.mygroup.vertxrs.security;

import java.lang.annotation.Annotation;

import io.vertx.rxjava.ext.auth.User;
import rx.Single;

public class VertxPermissionAnnotationHandler extends AuthorizingAnnotationHandler {

	@Override
	public Single<Boolean> assertAuthorized(Annotation authzSpec) {
		if(authzSpec instanceof RequiresPermissions){
			User user = getUser();
			if(user == null)
				return Single.error(new AuthorizationException("User required"));
			Single<Boolean> ret = Single.just(true);
			for(String perm : ((RequiresPermissions) authzSpec).value()){
				ret = user.rxIsAuthorised(perm).zipWith(ret, (a, b) -> a && b);
			}
			return ret;
		}
		return Single.just(true);
	}
}

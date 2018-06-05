package net.redpipe.engine.security;

import java.lang.annotation.Annotation;

import io.reactivex.Single;
import io.vertx.reactivex.ext.auth.User;

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

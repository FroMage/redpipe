package org.mygroup.vertxrs.security;

import java.lang.annotation.Annotation;

public class VertxPermissionAnnotationHandler extends AuthorizingAnnotationHandler {

	@Override
	public void assertAuthorized(Annotation authzSpec) {
		if(authzSpec instanceof RequiresPermissions){
			User user = getUser();
			if(user == null)
				throw new AuthorizationException("User required");
			for(String perm : ((RequiresPermissions) authzSpec).value()){
				if(!user.isAuthorisedBlocking(perm))
					throw new AuthorizationException("Permission denied");
			}
		}
	}
}

package org.mygroup.vertxrs.security;

import org.apache.shiro.subject.Subject;

import io.vertx.rxjava.core.Vertx;
import rx.Single;

public class ShiroUser implements User {

	private Subject subject;
	private Vertx vertx;

	public ShiroUser(Vertx vertx, Subject subject) {
		this.subject = subject;
		this.vertx = vertx;
	}

	@Override
	public Single<Void> checkAuthorised(String permission) {
		return isAuthorised(permission).map(ok -> {
			if(!ok)
				throw new AuthorizationException("Not authorized");
			return null;
		});
	}
	
	@Override
	public Single<Boolean> isAuthorised(String permission) {
		return vertx.rxExecuteBlocking(fut -> fut.complete(subject.isPermitted(permission)));
	}
	
	@Override
	public boolean isAuthorisedBlocking(String permission) {
		return subject.isPermitted(permission);
	}

	@Override
	public String getUsername() {
		return subject.getPrincipal().toString();
	}
	
}

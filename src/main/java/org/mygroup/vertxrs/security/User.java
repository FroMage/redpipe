package org.mygroup.vertxrs.security;

import rx.Single;

public interface User {
	public Single<Boolean> isAuthorised(String permission);
	public String getUsername();
}

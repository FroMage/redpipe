package org.mygroup.vertxrs.security;

import rx.Single;

public interface User {
	public Single<Boolean> isAuthorised(String permission);
	public boolean isAuthorisedBlocking(String permission);
	public String getUsername();
}

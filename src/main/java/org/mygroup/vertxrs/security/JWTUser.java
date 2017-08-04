package org.mygroup.vertxrs.security;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import rx.Single;

public class JWTUser implements User {

	private JsonObject payload;

	public JWTUser(JsonObject payload) {
		this.payload = payload;
	}

	@Override
	public boolean isAuthorisedBlocking(String permission) {
		if(permission == null)
			return false;
		JsonObject perms = payload.getJsonObject("permissions");
		if(perms != null){
			Boolean isGranted = perms.getBoolean(permission);
			if(isGranted != null && isGranted)
				return true;
		}
		return false;
	}
	
	@Override
	public Single<Boolean> isAuthorised(String permission) {
		return Single.just(isAuthorisedBlocking(permission));
	}

	@Override
	public String getUsername() {
		return payload.getString("username");
	}

}

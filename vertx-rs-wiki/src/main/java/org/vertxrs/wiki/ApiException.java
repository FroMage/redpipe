package org.vertxrs.wiki;

import javax.ws.rs.core.Response.Status;

@SuppressWarnings("serial")
public class ApiException extends RuntimeException {
	private Status status;

	public ApiException(Throwable cause){
		super(cause);
		status = Status.INTERNAL_SERVER_ERROR;
	}

	public ApiException(Status status, String message) {
		super(message);
		this.status = status;
	}
	
	public Status getStatus() {
		return status;
	}
}

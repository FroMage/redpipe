package org.mygroup.vertxrs.cdi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;
import javax.ws.rs.container.CompletionCallback;

import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.plugins.server.vertx.RequestDispatcher;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.weld.context.bound.BoundRequestContext;

import io.vertx.core.Context;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class CdiRequestDispatcher extends RequestDispatcher {

	public CdiRequestDispatcher(SynchronousDispatcher dispatcher, ResteasyProviderFactory providerFactory,
			SecurityDomain domain) {
		super(dispatcher, providerFactory, domain);
	}

	@Override
	public void service(Context context, HttpServerRequest req, HttpServerResponse resp, HttpRequest vertxReq,
			HttpResponse vertxResp, boolean handleNotFound) throws IOException {
        BoundRequestContext cdiContext = CDI.current().select(BoundRequestContext.class).get();
        Map<String,Object> contextMap = new HashMap<String,Object>();
        cdiContext.associate(contextMap);
        cdiContext.activate();
        try{
        	// FIXME: associate CDI thread context on thread change, like Resteasy context?
    		super.service(context, req, resp, vertxReq, vertxResp, handleNotFound);
    		if(vertxReq.getAsyncContext().isSuspended())
    			vertxReq.getAsyncContext().getAsyncResponse().register((CompletionCallback)(t) -> {
        			cdiContext.invalidate();
        			cdiContext.deactivate();
        			cdiContext.dissociate(contextMap);
    			});
        }finally{
    		if(!vertxReq.getAsyncContext().isSuspended()) {
    			cdiContext.invalidate();
    			cdiContext.deactivate();
    			cdiContext.dissociate(contextMap);
    		}
        }

	}
}

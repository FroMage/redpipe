package org.vertxrs.engine.dispatcher;

import java.io.IOException;
import java.util.List;

import org.jboss.resteasy.core.SynchronousDispatcher;
import org.jboss.resteasy.plugins.server.embedded.SecurityDomain;
import org.jboss.resteasy.plugins.server.vertx.RequestDispatcher;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.vertxrs.engine.spi.Plugin;

import io.vertx.core.Context;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

public class PluginRequestDispatcher extends RequestDispatcher {

	private List<Plugin> plugins;

	public PluginRequestDispatcher(SynchronousDispatcher dispatcher, ResteasyProviderFactory providerFactory,
			SecurityDomain domain, List<Plugin> plugins) {
		super(dispatcher, providerFactory, domain);
		this.plugins = plugins;
	}

	@Override
	public void service(Context context, HttpServerRequest req, HttpServerResponse resp, HttpRequest vertxReq,
			HttpResponse vertxResp, boolean handleNotFound) throws IOException {
		service(0, context, req, resp, vertxReq, vertxResp, handleNotFound);
	}

	private void service(int i, Context context, HttpServerRequest req, HttpServerResponse resp, HttpRequest vertxReq,
			HttpResponse vertxResp, boolean handleNotFound) throws IOException {
		if(i < plugins.size())
			plugins.get(i).aroundRequest(vertxReq, () -> service(i+1, context, req, resp, vertxReq, vertxResp, handleNotFound));
		else
			super.service(context, req, resp, vertxReq, vertxResp, handleNotFound);
	}
}

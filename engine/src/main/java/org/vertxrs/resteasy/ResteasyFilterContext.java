package org.vertxrs.resteasy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;

import org.jboss.resteasy.core.interception.jaxrs.PreMatchContainerRequestContext;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.LanguageHeader;
import io.vertx.ext.web.Locale;
import io.vertx.ext.web.ParsedHeaderValues;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

public class ResteasyFilterContext implements RoutingContext {

	private RoutingContext delegate;
	private PreMatchContainerRequestContext requestContext;

	public ResteasyFilterContext(ContainerRequestContext requestContext) {
		super();
		delegate = ResteasyProviderFactory.getContextData(io.vertx.rxjava.ext.web.RoutingContext.class).getDelegate();
		this.requestContext = (PreMatchContainerRequestContext)requestContext;
		this.requestContext.suspend();
	}

	@Override
	public void next() {
//		System.err.println("Resume");
		requestContext.resume();
	}

	@Override
	public void fail(int statusCode) {
//		System.err.println("Fail: "+statusCode);
		// FIXME: give back to RE?
		delegate.fail(statusCode);
	}

	@Override
	public void fail(Throwable throwable) {
//		System.err.println("Fail");
		throwable.printStackTrace();
		// FIXME: give back to RE?
		delegate.fail(throwable);
	}

	@Override
	public Cookie getCookie(String name) {
		return delegate.getCookie(name);
	}

	@Override
	public RoutingContext addCookie(Cookie cookie) {
		return delegate.addCookie(cookie);
	}

	@Override
	public Cookie removeCookie(String name) {
		return delegate.removeCookie(name);
	}

	@Override
	public int cookieCount() {
		return delegate.cookieCount();
	}

	@Override
	public Set<Cookie> cookies() {
		return delegate.cookies();
	}

	@Override
	public <T> T get(String key) {
		return delegate.get(key);
	}

	@Override
	public String mountPoint() {
		return delegate.mountPoint();
	}

	@Override
	public Route currentRoute() {
		return delegate.currentRoute();
	}

	@Override
	public String normalisedPath() {
		return delegate.normalisedPath();
	}

	@Override
	public String getBodyAsString() {
		return delegate.getBodyAsString();
	}

	@Override
	public String getBodyAsString(String encoding) {
		return delegate.getBodyAsString(encoding);
	}

	@Override
	public JsonObject getBodyAsJson() {
		return delegate.getBodyAsJson();
	}

	@Override
	public JsonArray getBodyAsJsonArray() {
		return delegate.getBodyAsJsonArray();
	}

	@Override
	public Buffer getBody() {
		return delegate.getBody();
	}

	@Override
	public Set<FileUpload> fileUploads() {
		return delegate.fileUploads();
	}

	@Override
	public Throwable failure() {
		return delegate.failure();
	}

	@Override
	public String getAcceptableContentType() {
		return delegate.getAcceptableContentType();
	}

	@Override
	public int addHeadersEndHandler(Handler<Void> handler) {
		return delegate.addHeadersEndHandler(handler);
	}

	@Override
	public int addBodyEndHandler(Handler<Void> handler) {
		return delegate.addBodyEndHandler(handler);
	}

	@Override
	public boolean failed() {
		return delegate.failed();
	}

	@Override
	public void clearUser() {
		delegate.clearUser();
	}

	@Override
	public List<Locale> acceptableLocales() {
		return delegate.acceptableLocales();
	}

	@Override
	public List<LanguageHeader> acceptableLanguages() {
		return delegate.acceptableLanguages();
	}

	@Override
	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public HttpServerRequest request() {
		return delegate.request();
	}

	@Override
	public HttpServerResponse response() {
		return delegate.response();
	}

	@Override
	public RoutingContext put(String key, Object obj) {
		return delegate.put(key, obj);
	}

	@Override
	public <T> T remove(String key) {
		return delegate.remove(key);
	}

	@Override
	public Vertx vertx() {
		return delegate.vertx();
	}

	@Override
	public Session session() {
		return delegate.session();
	}

	@Override
	public User user() {
		return delegate.user();
	}

	@Override
	public int statusCode() {
		return delegate.statusCode();
	}

	@Override
	public ParsedHeaderValues parsedHeaders() {
		return delegate.parsedHeaders();
	}

	@Override
	public boolean removeHeadersEndHandler(int handlerID) {
		return delegate.removeHeadersEndHandler(handlerID);
	}

	@Override
	public boolean removeBodyEndHandler(int handlerID) {
		return delegate.removeBodyEndHandler(handlerID);
	}

	@Override
	public void setBody(Buffer body) {
		delegate.setBody(body);
	}

	@Override
	public void setSession(Session session) {
//		System.err.println("setSession: "+session);
		ResteasyProviderFactory.pushContext(io.vertx.rxjava.ext.web.Session.class, io.vertx.rxjava.ext.web.Session.newInstance(session));
		delegate.setSession(session);
	}

	@Override
	public void setUser(User user) {
//		System.err.println("setUser: "+user);
		ResteasyProviderFactory.pushContext(io.vertx.rxjava.ext.auth.User.class, io.vertx.rxjava.ext.auth.User.newInstance(user));
		delegate.setUser(user);
	}

	@Override
	public void setAcceptableContentType(String contentType) {
		delegate.setAcceptableContentType(contentType);
	}

	@Override
	public void reroute(String path) {
		delegate.reroute(path);
	}

	@Override
	public void reroute(HttpMethod method, String path) {
		delegate.reroute(method, path);
	}

	@Override
	public Locale preferredLocale() {
		return delegate.preferredLocale();
	}

	@Override
	public LanguageHeader preferredLanguage() {
		return delegate.preferredLanguage();
	}

	@Override
	public Map<String, String> pathParams() {
		return delegate.pathParams();
	}

	@Override
	public String pathParam(String name) {
		return delegate.pathParam(name);
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	@Override
	public Map<String, Object> data() {
		return delegate.data();
	}
}

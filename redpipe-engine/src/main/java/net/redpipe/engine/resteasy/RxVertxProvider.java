package net.redpipe.engine.resteasy;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Priority;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

@Priority(0)
@Provider
@PreMatching
public class RxVertxProvider implements ContainerRequestFilter {

	public static final ServletContext ServletContext = new ServletContext(){

		private Map<String, Object> attributes = new HashMap<>();
		private Map<String, String> initParameters = new HashMap<>();
		{
			initParameters.put("scan.all.resources", "true");
		}

		@Override
		public Dynamic addFilter(String arg0, String arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Dynamic addFilter(String arg0, Filter arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Dynamic addFilter(String arg0, Class<? extends Filter> arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addListener(String arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public <T extends EventListener> void addListener(T arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void addListener(Class<? extends EventListener> arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, String arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Servlet arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public javax.servlet.ServletRegistration.Dynamic addServlet(String arg0, Class<? extends Servlet> arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T extends Filter> T createFilter(Class<T> arg0) throws ServletException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T extends EventListener> T createListener(Class<T> arg0) throws ServletException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public <T extends Servlet> T createServlet(Class<T> arg0) throws ServletException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void declareRoles(String... arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public Object getAttribute(String arg0) {
			return attributes.get(arg0);
		}

		@Override
		public Enumeration<String> getAttributeNames() {
			return Collections.enumeration(attributes.keySet());
		}

		@Override
		public ClassLoader getClassLoader() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public javax.servlet.ServletContext getContext(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getContextPath() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getEffectiveMajorVersion() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getEffectiveMinorVersion() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public FilterRegistration getFilterRegistration(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getInitParameter(String arg0) {
			return initParameters.get(arg0);
		}

		@Override
		public Enumeration<String> getInitParameterNames() {
			return Collections.enumeration(initParameters.keySet());
		}

		@Override
		public JspConfigDescriptor getJspConfigDescriptor() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getMajorVersion() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String getMimeType(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getMinorVersion() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public RequestDispatcher getNamedDispatcher(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getRealPath(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public RequestDispatcher getRequestDispatcher(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public URL getResource(String arg0) throws MalformedURLException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public InputStream getResourceAsStream(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Set<String> getResourcePaths(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getServerInfo() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Servlet getServlet(String arg0) throws ServletException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getServletContextName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Enumeration<String> getServletNames() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ServletRegistration getServletRegistration(String arg0) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<String, ? extends ServletRegistration> getServletRegistrations() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Enumeration<Servlet> getServlets() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public SessionCookieConfig getSessionCookieConfig() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getVirtualServerName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void log(String arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void log(Exception arg0, String arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void log(String arg0, Throwable arg1) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void removeAttribute(String arg0) {
			attributes.remove(arg0);
		}

		@Override
		public void setAttribute(String arg0, Object arg1) {
			attributes .put(arg0, arg1);
		}

		@Override
		public boolean setInitParameter(String arg0, String arg1) {
			initParameters.put(arg0, arg1);
			return false;
		}

		@Override
		public void setSessionTrackingModes(Set<SessionTrackingMode> arg0) {
			// TODO Auto-generated method stub
			
		}};

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		Vertx vertx = ResteasyProviderFactory.getContextData(io.vertx.core.Vertx.class);
		ResteasyProviderFactory.pushContext(io.vertx.reactivex.core.Vertx.class, io.vertx.reactivex.core.Vertx.newInstance(vertx));
        HttpServerRequest req = ResteasyProviderFactory.getContextData(HttpServerRequest.class);
		ResteasyProviderFactory.pushContext(io.vertx.reactivex.core.http.HttpServerRequest.class, io.vertx.reactivex.core.http.HttpServerRequest.newInstance(req));
        HttpServerResponse resp = ResteasyProviderFactory.getContextData(HttpServerResponse.class);
		ResteasyProviderFactory.pushContext(io.vertx.reactivex.core.http.HttpServerResponse.class, io.vertx.reactivex.core.http.HttpServerResponse.newInstance(resp));

		ResteasyProviderFactory.pushContext(ServletContext.class, ServletContext);
	}

}

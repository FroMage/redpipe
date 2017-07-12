package org.mygroup.vertxrs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.WriterInterceptor;

import org.jboss.resteasy.core.NoMessageBodyWriterFoundFailure;
import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.interception.jaxrs.AbstractWriterInterceptorContext;
import org.jboss.resteasy.core.interception.jaxrs.ServerWriterInterceptorContext;
import org.jboss.resteasy.plugins.server.vertx.VertxHttpResponse;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.ResteasyAsynchronousResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import io.vertx.core.http.HttpServerResponse;
import rx.Observable;
import rx.Single;
import rx.Subscription;

public class AsyncValidator implements ConstraintValidator<Async, Object>{

	@Inject
	private InjectedBean bean;
	
	@Override
	public void initialize(Async arg0) {
	}

	@Override
	public boolean isValid(Object ret, ConstraintValidatorContext arg1) {
		if(ret == null)
			return true;
		// glue: 
		HttpRequest req = ResteasyProviderFactory.getContextData(org.jboss.resteasy.spi.HttpRequest.class);
		if(req.getAsyncContext().isSuspended()){
			return true;
		}
		ResteasyAsynchronousResponse asyncResponse = req.getAsyncContext().suspend();
		handleReturnValue(ret, req, asyncResponse);
		return true;
	}
	
	public void handleReturnValue(Object ret, HttpRequest req, ResteasyAsynchronousResponse asyncResponse){
		Map<Class<?>, Object> contextData = ResteasyProviderFactory.getContextDataMap();
		ResourceMethodInvoker invoker =  (ResourceMethodInvoker)req.getAttribute(ResourceMethodInvoker.class.getName());
		invoker.initializeAsync(asyncResponse);

		// FIXME: allow cancelation even of single results?
		if(ret instanceof CompletionStage){
			((CompletionStage<?>) ret).thenAccept(resp -> {
				resumeResponse(contextData, asyncResponse, req, resp);
			}).exceptionally((error) -> {
				if(error instanceof CompletionException){
					error = error.getCause();
				}
				resumeError(contextData, asyncResponse, req, error);
				return null;
			});
		}else if(ret instanceof Single<?>){
			((Single<?>) ret).subscribe(resp -> {
				resumeResponse(contextData, asyncResponse, req, resp);
			}, error -> {
				resumeError(contextData, asyncResponse, req, error);
			});
		}else if(ret instanceof Observable<?>){
			boolean noStreaming = isNoStreaming(asyncResponse);
			HttpResponse response = ResteasyProviderFactory.getContextData(org.jboss.resteasy.spi.HttpResponse.class);
			HttpServerResponse vertxResponse = null;
			// FIXME: let me access this
			try {
				Field responseField = VertxHttpResponse.class.getDeclaredField("response");
				responseField.setAccessible(true);
				vertxResponse = (HttpServerResponse) responseField.get(response);
			} catch (NoSuchFieldException | SecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Subscription subscription;
			if(noStreaming){
				subscription = ((Observable<?>) ret).toList().subscribe(resp -> {
					resumeResponse(contextData, asyncResponse, req, resp);
				}, error -> {
					resumeError(contextData, asyncResponse, req, error);
				});
			}else{
				boolean chunked = isChunked(asyncResponse);
				if(!chunked){
					response.getOutputHeaders().add("Content-Type", "text/event-stream");
					response.getOutputHeaders().add("Cache-Control", "no-cache");
					response.getOutputHeaders().add("Connection", "keep-alive");
				}

				OutputStream os;
				try {
					os = response.getOutputStream();
					// sends headers and make chunked
					os.flush();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				subscription = ((Observable<?>) ret).subscribe(resp -> {
					try {
						// FIXME: id, encoding of \n\n
						ResteasyProviderFactory.pushContextDataMap(contextData);
						byte[] bytes = serialise(req, asyncResponse.getMethod(), getBuiltResponse(asyncResponse, req, resp));
						ResteasyProviderFactory.removeContextDataLevel();
						if(chunked){
							os.write(bytes);
						}else{
							// FIXME: probably really depends on how we serialised
							String data = new String(bytes, "utf-8");
							os.write(("data: "+data+"\n\n").getBytes("UTF-8"));
						}
						os.flush();
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}, error -> {
					// FIXME: what to do here, really?
					// FIXME: id, encoding of \n\n
					try {
						if(!chunked){
							os.write(("event: error\n").getBytes("UTF-8"));
							os.write(("data: "+error.getMessage()+"\n\n").getBytes("UTF-8"));
							os.flush();
						}
						// if we get an error, the stream is done anyway
						// FIXME: this doesn't really report the error to the client, it just closes the connection
						ResteasyProviderFactory.pushContextDataMap(contextData);
						asyncResponse.resume(error);
						ResteasyProviderFactory.removeContextDataLevel();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
			}
				
			if(vertxResponse != null && subscription != null){
				vertxResponse.closeHandler(nada -> {
					subscription.unsubscribe();
				});
			}

		}else{
			System.err.println("No idea what to do with return value: "+ret);
		}
	}

	private void resumeError(Map<Class<?>, Object> contextData, 
			ResteasyAsynchronousResponse asyncResponse,
			HttpRequest req, 
			Throwable error) {
		WithErrorMapper withErrorMapper = getAnnotation(asyncResponse, WithErrorMapper.class);
		if(withErrorMapper != null){
			// FIXME: CDI, @Context injection, all that?
			try {
				ErrorMapper errorMapper = withErrorMapper.value().newInstance();
				resumeResponse(contextData, asyncResponse, req, errorMapper.toErrorResponse(error));
			} catch (InstantiationException | IllegalAccessException e) {
				ResteasyProviderFactory.pushContextDataMap(contextData);
				asyncResponse.resume(e);
				ResteasyProviderFactory.removeContextDataLevel();
			}
		}else{
			ResteasyProviderFactory.pushContextDataMap(contextData);
			asyncResponse.resume(error);
			ResteasyProviderFactory.removeContextDataLevel();
		}
	}

	private void resumeResponse(Map<Class<?>, Object> contextData, 
			ResteasyAsynchronousResponse asyncResponse,
			HttpRequest req, 
			Object resp) {
		ResteasyProviderFactory.pushContextDataMap(contextData);
		resume(asyncResponse, req, resp);
		ResteasyProviderFactory.removeContextDataLevel();
		
	}

	private <T extends Annotation> T getAnnotation(ResteasyAsynchronousResponse asyncResponse, Class<? extends T> annotationType) {
		for (Annotation annotation : asyncResponse.getMethod().getMethodAnnotations()) {
			if(annotation.annotationType() == annotationType){
				return (T) annotation;
			}
		}
		for (Annotation annotation : asyncResponse.getMethod().getResourceClass().getAnnotations()){
			if(annotation.annotationType() == annotationType){
				return (T) annotation;
			}
		}
		return null; 
	}

	private boolean isNoStreaming(ResteasyAsynchronousResponse asyncResponse) {
		return getAnnotation(asyncResponse, CollectUntilComplete.class) != null;
	}

	private boolean isChunked(ResteasyAsynchronousResponse asyncResponse) {
		return getAnnotation(asyncResponse, Chunked.class) != null;
	}

	private BuiltResponse getBuiltResponse(ResteasyAsynchronousResponse asyncResponse, HttpRequest request, Object entity) {
		// it knows what to do for those
		if(entity == null || entity instanceof Response)
			asyncResponse.resume(entity);
		ResourceMethodInvoker method = asyncResponse.getMethod();
		// let it crash itself if it must (it handles that case)
		if(method == null)
			asyncResponse.resume(entity);
		// substitute our own generic return type
        MediaType type = method.resolveContentType(request, entity);
        BuiltResponse jaxrsResponse = (BuiltResponse)Response.ok(entity, type).build();
        Type returnType = method.getGenericReturnType();
        if(returnType instanceof ParameterizedType){
        	Type rawType = ((ParameterizedType) returnType).getRawType();
        	// FIXME: error handling
        	if(rawType == Single.class)
        		returnType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
        	else if(rawType == CompletionStage.class)
        		returnType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
        	else if(rawType == Observable.class)
        		returnType = ((ParameterizedType) returnType).getActualTypeArguments()[0];
        }
        if(isNoStreaming(asyncResponse)){
        	Type elementType = returnType;
        	// we want a List<returnType>
        	returnType = new ParameterizedType(){

				@Override
				public Type[] getActualTypeArguments() {
					return new Type[]{elementType};
				}

				@Override
				public Type getOwnerType() {
					return null;
				}

				@Override
				public Type getRawType() {
					return List.class;
				}
        		// FIXME: equals/hashCode/toString?
        	};
        }
        jaxrsResponse.setGenericType(returnType);
        jaxrsResponse.addMethodAnnotations(method.getMethodAnnotations());
        return jaxrsResponse;
	}
	
	private void resume(ResteasyAsynchronousResponse asyncResponse, HttpRequest request, Object entity) {
        asyncResponse.resume(getBuiltResponse(asyncResponse, request, entity));
	}

	private byte[] serialise(HttpRequest request, ResourceMethodInvoker method, BuiltResponse jaxrsResponse) throws WebApplicationException, IOException{
		Class<?> type = jaxrsResponse.getEntityClass();
		Object ent = jaxrsResponse.getEntity();
		Type generic = jaxrsResponse.getGenericType();
		Annotation[] annotations = jaxrsResponse.getAnnotations();
		ResteasyProviderFactory providerFactory = ResteasyProviderFactory.getInstance();
		@SuppressWarnings(value = "unchecked")
		MessageBodyWriter<?> writer = providerFactory.getMessageBodyWriter(
				type, generic, annotations, jaxrsResponse.getMediaType());
		if (writer!=null)
			LogMessages.LOGGER.debugf("MessageBodyWriter: %s", writer.getClass().getName());

		if (writer == null)
		{
			throw new NoMessageBodyWriterFoundFailure(type, jaxrsResponse.getMediaType());
		}

		ByteArrayOutputStream os = new ByteArrayOutputStream();

		WriterInterceptor[] writerInterceptors = null;
		if (method != null)
		{
			writerInterceptors = method.getWriterInterceptors();
		}
		else
		{
			writerInterceptors = providerFactory.getServerWriterInterceptorRegistry().postMatch(null, null);
		}

		AbstractWriterInterceptorContext writerContext =  new ServerWriterInterceptorContext(writerInterceptors,
				providerFactory, ent, type, generic, annotations, jaxrsResponse.getMediaType(),
				jaxrsResponse.getMetadata(), os, request);
		writerContext.proceed();
		
		return os.toByteArray();
	}
}

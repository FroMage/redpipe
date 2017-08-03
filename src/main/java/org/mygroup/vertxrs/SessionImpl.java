package org.mygroup.vertxrs;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.NewCookie;

import org.apache.commons.codec.binary.Hex;

public class SessionImpl implements Session {

	private static final String SESSION_COOKIE = "VERTXRS_SESSION";
	private final static String SECRET_KEY = "6df5a5d160b141454d9cf38a2a88ffdd30a7dc80876698f53a4fbf071a2e16c4";
	private static final String TIMESTAMP = "__TS";
	private static final long EXPIRE = 1000 /*1 sec*/ * 60 /*1 min*/ * 60 /*1 hour*/ * 24 /*1 day*/ * 7 /* 1 week */;

	private boolean changed;
	private Map<String, String> data;

	public SessionImpl(Map<String, String> data) {
		this.data = data;
	}

	public SessionImpl() {
		this(new HashMap<>());
	}

	public void save(ContainerResponseContext responseContext) {
		if(changed){
			data.put(TIMESTAMP, String.valueOf(System.currentTimeMillis() + EXPIRE));
			String newData = encodeCookie(data);
			String sign = sign(newData);
			NewCookie cookie = new NewCookie(SESSION_COOKIE, newData+"-"+sign, "/", null, Cookie.DEFAULT_VERSION, null, (int)EXPIRE, false);
			responseContext.getHeaders().add(HttpHeaders.SET_COOKIE, cookie);
		}
	}

	@Override
	public String toString() {
		return "Session: "+data+", changed: "+changed;
	}

	public void put(String key, String value) {
		if(value == null)
			data.remove(key);
		else
			data.put(key, value);
		change();
	}

	public String get(String key) {
		return data.get(key);
	}

	public void remove(String key) {
		data.remove(key);
		change();
	}

	private void change() {
		changed = true;
	}

	public void clear() {
		data.clear();
		change();
	}

	//
	// Utils
	
	private static String sign(String data){
		try{
			Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
			SecretKeySpec secret_key = new SecretKeySpec(SECRET_KEY.getBytes("UTF-8"), "HmacSHA256");
			sha256_HMAC.init(secret_key);

			return Hex.encodeHexString(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
		}catch(Exception x){
			throw new RuntimeException(x);
		}
	}

	public static SessionImpl restore(ContainerRequestContext requestContext) {
		Cookie cookie = requestContext.getCookies().get(SESSION_COOKIE);
		SessionImpl session = null;
		if(cookie != null){
			String value = cookie.getValue();
			if(value != null){
				int dash = value.lastIndexOf('-');
				if(dash > 0){
					String data = value.substring(0, dash);
					String signature = value.substring(dash+1);
					if(!signature.isEmpty() && !data.isEmpty() && sign(data).equals(signature)){
						Map<String, String> values = decodeCookie(data);
						String ts = values.get(TIMESTAMP);
						if(ts != null && Long.valueOf(ts) >= System.currentTimeMillis()){
							values.remove(TIMESTAMP);
							session = new SessionImpl(values);
						}
					}
				}
			}
		}
		if(session == null){
			session = new SessionImpl();
		}
		return session;
	}

	private static Map<String,String> decodeCookie(String data) {
		try {
			Map<String,String> ret = new HashMap<>();
			for(String part : data.split("&")){
				String[] mapping = part.split("=");
				if(mapping.length == 2){
					ret.put(URLDecoder.decode(mapping[0], "utf-8"), 
							URLDecoder.decode(mapping[1], "utf-8"));
				}
			}
			return ret;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private static String encodeCookie(Map<String, String> data) {
		try{
			StringBuffer strbuf = new StringBuffer();
			for(Map.Entry<String, String> entry : data.entrySet()){
				if(strbuf.length() != 0)
					strbuf.append("&");
				strbuf.append(URLEncoder.encode(entry.getKey(), "utf-8"));
				strbuf.append("=");
				strbuf.append(URLEncoder.encode(entry.getValue(), "utf-8"));
			}
			return strbuf.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

	}
}

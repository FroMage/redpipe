package net.redpipe.engine.util;

public class RedpipeUtil {
	public static <T extends Throwable, Ret> Ret rethrow(Throwable cause) throws T {
		throw (T)cause;
	}
}

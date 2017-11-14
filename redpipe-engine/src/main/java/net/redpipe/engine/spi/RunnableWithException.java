package net.redpipe.engine.spi;

@FunctionalInterface
public interface RunnableWithException<T extends Throwable> {
	void run() throws T;
}

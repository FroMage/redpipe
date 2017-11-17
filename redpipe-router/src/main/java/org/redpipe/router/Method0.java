package org.redpipe.router;

@FunctionalInterface
public interface Method0<Target> extends MethodFinder {
	Object method(Target target);
}

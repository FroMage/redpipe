package net.redpipe.engine.context;

import io.reactiverse.reactivecontexts.core.ContextProvider;
import net.redpipe.engine.core.AppGlobals;

public class AppGlobalsContextProvider implements ContextProvider<AppGlobals> {

	@Override
	public AppGlobals install(AppGlobals state) {
		return AppGlobals.set(state);
	}

	@Override
	public void restore(AppGlobals previousState) {
		AppGlobals.set(previousState);
	}

	@Override
	public AppGlobals capture() {
		return AppGlobals.get();
	}
}

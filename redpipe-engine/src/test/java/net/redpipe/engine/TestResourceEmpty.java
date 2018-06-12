package net.redpipe.engine;

import io.reactivex.Completable;
import io.reactivex.Maybe;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/empty")
public class TestResourceEmpty {

    @GET
    @Path("/completable1")
    public Completable returnCompletable() {
        return Completable.complete(); // should be 204
    }

    @GET
    @Path("/completable2")
    public rx.Completable returnCompletable2() {
        return rx.Completable.complete(); // should be 204
    }

    @GET
    @Path("/maybe/empty")
    public Maybe<String> returnEmptyMaybe() {
        return Maybe.empty(); // should be 404
    }

    @GET
    @Path("/maybe/fulfilled")
    public Maybe<String> returnFulfilledMaybe() {
        return Maybe.just("something"); // should be 404
    }

}

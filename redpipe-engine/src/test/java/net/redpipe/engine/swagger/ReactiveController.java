package net.redpipe.engine.swagger;

import io.reactivex.Maybe;
import rx.Completable;
import rx.Observable;
import rx.Single;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class ReactiveController {

    private static class Dog {
        private String name;

        public Dog(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

    private final Dog SNOOPY = new Dog("Snoopy");

    @GET
    public Dog getDogSync() {
        return SNOOPY;
    }

    @GET
    @Path("/single")
    public Single<Dog> getSingleDog() {
        return Single.just(SNOOPY);
    }

    @GET
    @Path("/single2")
    public io.reactivex.Single<Dog> getSingleDog2() {
        return io.reactivex.Single.just(SNOOPY);
    }

    @GET
    @Path("/observable")
    public Observable<Dog> getSingleObservable() {
        return Observable.just(SNOOPY);
    }

    @GET
    @Path("/observable2")
    public io.reactivex.Observable<Dog> getSingleObservable2() {
        return io.reactivex.Observable.just(SNOOPY);
    }

    @GET
    @Path("/completable")
    public Completable getCompletable() {
        return Completable.complete();
    }

    @GET
    @Path("/completable2")
    public io.reactivex.Completable getCompletable2() {
        return io.reactivex.Completable.complete();
    }

    @GET
    @Path("/maybe")
    public Maybe<Dog> getMaybe() {
        return Maybe.just(SNOOPY);
    }

}

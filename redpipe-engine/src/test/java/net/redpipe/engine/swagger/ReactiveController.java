package net.redpipe.engine.swagger;

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
    @Path("/observable")
    public Observable<Dog> getSingleObservable() {
        return Observable.just(SNOOPY);
    }

}

package org.vertxrs.example.helloworld;

import org.vertxrs.engine.core.Server;

public class Main {
    public static void main( String[] args ){
    	new Server()
    		.start(HelloResource.class)
    		.subscribe(v -> System.err.println("Server started"),
    				   x -> x.printStackTrace());
    }
}

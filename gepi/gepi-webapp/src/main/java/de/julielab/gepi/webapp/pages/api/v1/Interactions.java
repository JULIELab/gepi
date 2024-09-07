package de.julielab.gepi.webapp.pages.api.v1;

import org.apache.tapestry5.annotations.RequestBody;

public class Interactions {

//    @RestInfo(consumes = "application/json")
    Object onHttpPost(@RequestBody() String interactionRequest) {
//        System.out.println(interactionRequest.getLista());

//        return new TextStreamResponse(ContentType.TEXT_PLAIN.getMimeType(), "Antwort");
        return null;
    }

//    TextStreamResponse onHttpGet() {
//        return new TextStreamResponse(ContentType.TEXT_PLAIN.getMimeType(), "");
//    }
}

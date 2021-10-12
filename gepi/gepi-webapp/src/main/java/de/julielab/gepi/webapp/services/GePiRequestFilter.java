package de.julielab.gepi.webapp.services;

import de.julielab.gepi.webapp.state.GePiSessionState;
import org.apache.tapestry5.http.services.Request;
import org.apache.tapestry5.http.services.RequestFilter;
import org.apache.tapestry5.http.services.RequestHandler;
import org.apache.tapestry5.http.services.Response;
import org.apache.tapestry5.services.*;
import org.slf4j.Logger;

import java.io.IOException;

public class GePiRequestFilter implements RequestFilter {

    private Logger log;
    private ApplicationStateManager asm;

    public GePiRequestFilter(Logger log, ApplicationStateManager asm) {
        this.log = log;
        this.asm = asm;
    }

    @Override
    public boolean service(Request request, Response response, RequestHandler handler) throws IOException {
        GePiSessionState sessionState = asm.getIfExists(GePiSessionState.class);
        log.warn("sessionState: {}",sessionState);
        if (sessionState != null) {
            sessionState.setActiveTabFromRequest(request);
        }
        return handler.service(request, response);
    }
}

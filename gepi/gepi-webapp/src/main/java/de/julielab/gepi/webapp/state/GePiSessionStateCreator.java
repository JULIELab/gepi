package de.julielab.gepi.webapp.state;

import org.apache.tapestry5.ioc.LoggerSource;
import org.apache.tapestry5.services.ApplicationStateCreator;
import org.slf4j.Logger;

public class GePiSessionStateCreator implements ApplicationStateCreator<GePiSessionState> {

    private final Logger log;
    private LoggerSource loggerSource;

    public GePiSessionStateCreator(LoggerSource loggerSource) {
        this.loggerSource = loggerSource;
        this.log = loggerSource.getLogger(GePiSessionStateCreator.class);
    }

    @Override
    public GePiSessionState create() {
        log.trace("Creating new GepiSessionState.");
        return new GePiSessionState(loggerSource.getLogger(GePiSessionState.class));
    }
}

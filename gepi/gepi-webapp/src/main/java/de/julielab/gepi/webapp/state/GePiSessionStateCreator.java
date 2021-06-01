package de.julielab.gepi.webapp.state;

import org.apache.tapestry5.ioc.LoggerSource;
import org.apache.tapestry5.services.ApplicationStateCreator;

public class GePiSessionStateCreator implements ApplicationStateCreator<GePiSessionState> {

    private LoggerSource loggerSource;

    public GePiSessionStateCreator(LoggerSource loggerSource) {
        this.loggerSource = loggerSource;
    }

    @Override
    public GePiSessionState create() {
        return new GePiSessionState(loggerSource.getLogger(GePiSessionState.class));
    }
}

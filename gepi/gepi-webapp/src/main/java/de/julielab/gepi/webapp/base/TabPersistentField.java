package de.julielab.gepi.webapp.base;

import de.julielab.gepi.webapp.state.GePiSessionState;
import org.apache.tapestry5.internal.services.PersistentFieldChangeImpl;
import org.apache.tapestry5.ioc.internal.util.InternalUtils;
import org.apache.tapestry5.services.ApplicationStateManager;
import org.apache.tapestry5.services.PersistentFieldChange;
import org.apache.tapestry5.services.PersistentFieldStrategy;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


public class TabPersistentField implements PersistentFieldStrategy {
    private Logger log;
    public static final String TAB = "tab";
    private ApplicationStateManager asm;


    public TabPersistentField(Logger log, ApplicationStateManager asm) {
        this.log = log;
        this.asm = asm;
    }


    @Override
    public Collection<PersistentFieldChange> gatherFieldChanges(String pageName) {
        // We first check if there was a session state to begin with purely for logging
        GePiSessionState sessionState = asm.getIfExists(GePiSessionState.class);
        if (sessionState == null) {
            log.debug("No state exists for this session yet. Creating one to store the current changes.");
            sessionState = asm.get(GePiSessionState.class);
        }

        log.trace("Gathering tab-persistent field changes for page {}.", pageName, sessionState);
        if (sessionState == null)
            return Collections.emptyList();

        List<PersistentFieldChange> result = new ArrayList<>();

        int activeTab = sessionState.getActiveTabIndex();

        String fullPrefix = TAB + activeTab + ":" + pageName + ":";
        log.trace("Persistence field prefix is: {}", fullPrefix);
        for (String name : sessionState.getAttributeNames(fullPrefix)) {
            Object persistedValue = sessionState.getAttribute(name);

            PersistentFieldChange change = buildChange(name, persistedValue);

            result.add(change);
        }

        return result;
    }

    @Override
    public void discardChanges(String pageName) {
        GePiSessionState sessionState = asm.getIfExists(GePiSessionState.class);

        if (sessionState == null)
            return;

        int activeTab = sessionState.getActiveTabIndex();

        String fullPrefix = TAB + activeTab + ":" + pageName + ":";

        for (String name : sessionState.getAttributeNames(fullPrefix)) {
            sessionState.removeAttribute(name);
        }
    }

    @Override
    public void postChange(String pageName, String componentId, String fieldName, Object newValue) {
        log.debug("Posting tab-persistent changes.");
        GePiSessionState sessionState = asm.getIfExists(GePiSessionState.class);

        assert InternalUtils.isNonBlank(pageName);
        assert InternalUtils.isNonBlank(fieldName);
        Object persistedValue = newValue;

        StringBuilder builder = new StringBuilder(TAB);
        if (sessionState != null)
            builder.append(sessionState.getActiveTabIndex());
        builder.append(":");
        builder.append(pageName);

        builder.append(':');

        if (componentId != null)
            builder.append(componentId);

        builder.append(':');
        builder.append(fieldName);

        if (sessionState != null) {
            sessionState.setAttribute(builder.toString(), persistedValue);
        }
    }

    private PersistentFieldChange buildChange(String name, Object newValue) {
        String[] chunks = name.split(":");

        // Will be empty string for the root component
        String componentId = chunks[2];
        String fieldName = chunks[3];

        return new PersistentFieldChangeImpl(componentId, fieldName, newValue);
    }



}

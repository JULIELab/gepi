package de.julielab.gepi.webapp.state;

import org.apache.tapestry5.services.Request;
import org.slf4j.Logger;

import java.util.*;

public class GePiSessionState {
    public static final String PARAM_ACTIVE_TAB = "tab";

    private Map<String, Object> persistentAttributes;
    private int activeTabIndex;
    private List<GePiTab> tabs;
    private Logger log;

    public GePiSessionState(Logger log) {
        this.log = log;
        this.tabs = new ArrayList<>();
        this.persistentAttributes = new HashMap<>();
    }

    public Object getAttribute(String name) {
        return persistentAttributes.get(name);
    }

    public void setAttribute(String name, Object object) {
        persistentAttributes.put(name, object);
    }

    public void removeAttribute(String name) {
        persistentAttributes.remove(name);
    }

    public Collection<String> getAttributeNames(String fullPrefix) {
        List<String> matchedKeys = new ArrayList<>();
        for (String key : persistentAttributes.keySet()) {
            if (key.startsWith(fullPrefix)) {
                matchedKeys.add(key);
            }
        }
        return matchedKeys;
    }

    public int getActiveTabIndex() {
        return activeTabIndex;
    }

    public void setActiveTabIndex(int activeTabIndex) {
        this.activeTabIndex = activeTabIndex;
    }

    public GePiTab setActiveTabFromRequest(Request request) {
        // We want to keep the active tab index around as a query parameter. We
        // do this as to enable the browser back button in
        // a useful way within the application. If we would NOT have the query
        // parameter and we would change from tab1
        // to tab2, for example, and then hit the browser 'back' button, we
        // would still be at tab2 because the session
        // state was changed to tab2 on the selection of tab2. With a query
        // parameter, the back button will return to
        // the state where tab1 was set to be active, this is read at this place
        // here as set into the session.
log.warn("HIER");
        if (request.isXHR()) {
            log.debug(
                    "Request was XHR enabled, most probably an AJAX call, it is not tried to read the active tab from it.");
            return getActiveTab();
        }

        // Initialize the active tab with -1 which equals "not set".
        int activeTabIndex = -1;
        // Get the active tab index from the request, if it is set there.
        String activeTabParam = request.getParameter(PARAM_ACTIVE_TAB);
        log.debug("Trying to receive active tab index from request.");
        if (null != activeTabParam) {
            try {
                activeTabIndex = Integer.parseInt(activeTabParam);
                log.debug("Read active tab index {} from query parameter.", activeTabIndex);
            } catch (NumberFormatException e) {
                log.error("Active tab query parameter named {} was not an integer. Value was: {}", PARAM_ACTIVE_TAB,
                        activeTabParam);
            }
        } else {
            log.debug("Request parameter {} is not set.", PARAM_ACTIVE_TAB);
        }
        if (activeTabIndex == -1) {
            if (getTabs().size() == 1) {
                log.debug("There is only one tab present in the session, setting active index to 0.");
                activeTabIndex = 0;
            } else {
                // This is the fallback the default behavior: Just use the the
                // value that is stored in the session.
                activeTabIndex = getActiveTabIndex();
                log.debug("Did not find tab index query parameter. Fallback to session state: Active tab index is {}.",
                        activeTabIndex);
            }
        }
        log.debug("Active tab set to {}.", activeTabIndex);
        // Up to now we will have determined SOME active tab. Set it into the
        // session to reflected in the rendering.
        return setActiveTab(activeTabIndex);
    }

    public GePiTab getActiveTab() {
        if (tabs.isEmpty())
            return null;
        return tabs.get(activeTabIndex);
    }

    public GePiTab setActiveTab(int tabIndex) {
        activeTabIndex = tabIndex;
        return getActiveTab();
    }

    /**
     * Returns all tabs of this session.
     *
     * @return All tabs session objects.
     */
    public List<GePiTab> getTabs() {
        return tabs;
    }

}

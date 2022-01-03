package de.julielab.gepi.webapp.state;

public class GePiTab {
    public enum TabType {
        BATCH_TAB, DETAIL_TAB
    }

    protected String name;
    private String activePageName;
    protected final TabType tabType;
    private int tabIndex;

    public GePiTab(String name, int tabIndex, TabType tabType) {
        this.name = name;
        this.tabIndex = tabIndex;
        this.tabType = tabType;
        this.activePageName = getStartPageName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getActivePageName() {
        return activePageName;
    }

    public void setActivePageName(String activePageName) {
        this.activePageName = activePageName;
    }

    public String getStartPageName() {
        return "Index";}

    public TabType getTabType() {
        return tabType;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    public String getTabIndexAsString() {
        return String.valueOf(tabIndex);
    }
}

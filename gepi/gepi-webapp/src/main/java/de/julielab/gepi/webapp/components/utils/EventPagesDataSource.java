package de.julielab.gepi.webapp.components.utils;

import org.apache.tapestry5.grid.GridDataSource;
import org.apache.tapestry5.grid.SortConstraint;

import java.util.List;

public class EventPagesDataSource implements GridDataSource {
    @Override
    public int getAvailableRows() {
        return 0;
    }

    @Override
    public void prepare(int i, int i1, List<SortConstraint> list) {

    }

    @Override
    public Object getRowValue(int i) {
        return null;
    }

    @Override
    public Class getRowType() {
        return null;
    }
}

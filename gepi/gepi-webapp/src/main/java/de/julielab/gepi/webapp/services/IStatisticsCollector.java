package de.julielab.gepi.webapp.services;

import de.julielab.gepi.webapp.data.GepiEventStatistics;

public interface IStatisticsCollector extends Runnable {
    GepiEventStatistics getStats();
}

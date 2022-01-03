package de.julielab.gepi.core.services;

import org.apache.tapestry5.commons.OrderedConfiguration;
import org.apache.tapestry5.ioc.annotations.Autobuild;
import org.apache.tapestry5.ioc.annotations.ImportModule;
import org.apache.tapestry5.ioc.annotations.Startup;
import org.apache.tapestry5.ioc.services.SymbolProvider;

@ImportModule(GepiCoreModule.class)
public class GePiCoreTestModule {
	@Startup
	public static void initMyApplication() {
		System.setProperty(ConfigurationSymbolProvider.CONFIG_FILE_PROPERTY,
				"src/test/resources/testconfiguration.properties");
	}

	public static void contributeSymbolSource(@Autobuild ConfigurationSymbolProvider symbolProvider,
			final OrderedConfiguration<SymbolProvider> configuration) {
		configuration.add("GePiConfigurationSymbols", symbolProvider, "before:ApplicationDefaults");
	}
}

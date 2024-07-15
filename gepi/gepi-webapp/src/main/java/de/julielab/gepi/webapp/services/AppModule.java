package de.julielab.gepi.webapp.services;

import de.julielab.gepi.core.GepiCoreSymbolConstants;
import de.julielab.gepi.core.services.ConfigurationSymbolProvider;
import de.julielab.gepi.core.services.GepiCoreModule;
import de.julielab.gepi.webapp.base.TabPersistentField;
import de.julielab.gepi.webapp.state.GePiSessionState;
import de.julielab.gepi.webapp.state.GePiSessionStateCreator;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.MetaDataConstants;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.commons.MappedConfiguration;
import org.apache.tapestry5.commons.OrderedConfiguration;
import org.apache.tapestry5.http.Link;
import org.apache.tapestry5.http.services.*;
import org.apache.tapestry5.ioc.LoggerSource;
import org.apache.tapestry5.ioc.ServiceBinder;
import org.apache.tapestry5.ioc.annotations.*;
import org.apache.tapestry5.ioc.services.ApplicationDefaults;
import org.apache.tapestry5.ioc.services.ParallelExecutor;
import org.apache.tapestry5.ioc.services.SymbolProvider;
import org.apache.tapestry5.ioc.services.cron.IntervalSchedule;
import org.apache.tapestry5.ioc.services.cron.PeriodicExecutor;
import org.apache.tapestry5.services.*;
import org.apache.tapestry5.services.javascript.JavaScriptStack;
import org.apache.tapestry5.services.javascript.StackExtension;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;

import static de.julielab.gepi.core.services.GePiDataService.GEPI_EXCEL_FILE_PREFIX_NAME;
import static de.julielab.gepi.core.services.GePiDataService.GEPI_TMP_DIR_NAME;

/**
 * This module is automatically included as part of the Tapestry IoC Registry, it's a good place to
 * configure and extend Tapestry, or to place your own service definitions.
 */
@ImportModule(GepiCoreModule.class)
public class AppModule {


    public static void contributeSymbolSource(@Autobuild ConfigurationSymbolProvider symbolProvider,
                                              final OrderedConfiguration<SymbolProvider> configuration) {
        configuration.add("GePiConfigurationSymbols", symbolProvider, "before:ApplicationDefaults");
    }

    public static void bind(ServiceBinder binder) {
        // binder.bind(MyServiceInterface.class, MyServiceImpl.class);

        // Make bind() calls on the binder object to define most IoC services.
        // Use service builder methods (example below) when the implementation
        // is provided inline, or requires more initialization than simply
        // invoking the constructor.
        binder.bind(IStatisticsCollector.class, StatisticsCollector.class);
        binder.bind(ITempFileCleaner.class, TempFileCleaner.class);
    }

    public static void contributeFactoryDefaults(
            MappedConfiguration<String, Object> configuration) {
        // The values defined here (as factory default overrides) are themselves
        // overridden with application defaults by DevelopmentModule and QaModule.

        // The application version is primarily useful as it appears in
        // any exception reports (HTML or textual).
        configuration.override(SymbolConstants.APPLICATION_VERSION, "1.0.3-SNAPSHOT");
        // Avoid Ajax-requests waiting for each other. This would make asynchronous lading of
        // dashboard elements impossible
        configuration.override(SymbolConstants.SESSION_LOCKING_ENABLED, false);
        // This works as if all pages would have a @Secure annotation. Despite this seeming as if "security" would
        // be disabled, this is actually the way to use when only HTTPS should be used.
        // See https://tapestry.apache.org/configuration.html
        configuration.override(SymbolConstants.SECURE_ENABLED, false);

        // This is something that should be removed when going to production, but is useful
        // in the early stages of development.
        configuration.override(SymbolConstants.PRODUCTION_MODE, false);
    }

    public static void contributeApplicationDefaults(
            MappedConfiguration<String, Object> configuration) {
        // Contributions to ApplicationDefaults will override any contributions to
        // FactoryDefaults (with the same key). Here we're restricting the supported
        // locales to just "en" (English). As you add localised message catalogs and other assets,
        // you can extend this list of locales (it's a comma separated series of locale names;
        // the first locale name is the default when there's no reasonable match).
        configuration.add(SymbolConstants.SUPPORTED_LOCALES, "en");

        // You should change the passphrase immediately; the HMAC passphrase is used to secure
        // the hidden field data stored in forms to encrypt and digitally sign client-side data.
        configuration.add(SymbolConstants.HMAC_PASSPHRASE, "juliegepipassphrase");

        configuration.add(SymbolConstants.ERRORS_CLOSE_BUTTON_CSS_CLASS, "btn-close position-absolute top-0 end-0");

        configuration.add(GepiCoreSymbolConstants.GEPI_TMP_DIR, Path.of(System.getProperty("java.io.tmpdir"), GEPI_TMP_DIR_NAME));
        configuration.add(GepiCoreSymbolConstants.GEPI_EXCEL_FILE_PREFIX, GEPI_EXCEL_FILE_PREFIX_NAME);
    }

    /**
     * Use annotation or method naming convention: <code>contributeApplicationDefaults</code>
     */
    @Contribute(SymbolProvider.class)
    @ApplicationDefaults
    public static void setupEnvironment(MappedConfiguration<String, Object> configuration) {
        // Support for jQuery is new in Tapestry 5.4 and will become the only supported
        // option in 5.5.
        configuration.add(SymbolConstants.JAVASCRIPT_INFRASTRUCTURE_PROVIDER, "jquery");
        configuration.add(SymbolConstants.BOOTSTRAP_ROOT, "context:bootstrap-5.2.2-dist");
        configuration.add(SymbolConstants.MINIFICATION_ENABLED, false);
    }

    @Core
    @Contribute(JavaScriptStack.class)
    public static void overrideJquery(OrderedConfiguration<StackExtension> conf) {
        conf.override("jquery-library", StackExtension.library("classpath:META-INF/assets/jquery-3.6.0.min.js"));
    }

    @Contribute(RequestHandler.class)
    public static void contributeRequestFilters(final OrderedConfiguration<RequestFilter> filters) {
        filters.addInstance(GePiRequestFilter.class.getSimpleName(), GePiRequestFilter.class, "after:ErrorFilter");
    }

    @Startup
    public static void scheduleJobs(ParallelExecutor pExecutor, PeriodicExecutor executor, IStatisticsCollector statisticsCollector, ITempFileCleaner tempFileCleaner) {
        // this was meant to collection current interaction statistics once a day (the lower time given here was for
        // development purposes)
        // Could still be done, removed it for now due to time constraints
//         executor.addJob(new IntervalSchedule(60000),
//         "Event Statistics Calculation Job",
//         statisticsCollector);
        executor.addJob(new IntervalSchedule(Duration.ofDays(1).toMillis()), "Temp file deletion job", statisticsCollector);
    }

    public void contributeMetaDataLocator(MappedConfiguration<String, String> configuration) {
        configuration.add(MetaDataConstants.SECURE_PAGE, "true");
    }

    /**
     * This is a service definition, the service will be named "TimingFilter". The interface,
     * RequestFilter, is used within the RequestHandler service pipeline, which is built from the
     * RequestHandler service configuration. Tapestry IoC is responsible for passing in an
     * appropriate Logger instance. Requests for static resources are handled at a higher level, so
     * this filter will only be invoked for Tapestry related requests.
     * <p>
     * <p>
     * Service builder methods are useful when the implementation is inline as an inner class
     * (as here) or require some other kind of special initialization. In most cases,
     * use the static bind() method instead.
     * <p>
     * <p>
     * If this method was named "build", then the service id would be taken from the
     * service interface and would be "RequestFilter".  Since Tapestry already defines
     * a service named "RequestFilter" we use an explicit service id that we can reference
     * inside the contribution method.
     */
    @ServiceId("timingFilter")
    public RequestFilter buildTimingFilter(final Logger log) {
        return new RequestFilter() {
            public boolean service(Request request, Response response, RequestHandler handler)
                    throws IOException {
                long startTime = System.currentTimeMillis();

                try {
                    // The responsibility of a filter is to invoke the corresponding method
                    // in the handler. When you chain multiple filters together, each filter
                    // received a handler that is a bridge to the next filter.

                    return handler.service(request, response);
                } finally {
                    long elapsed = System.currentTimeMillis() - startTime;

                    log.info("Request time: {} ms", elapsed);
                }
            }
        };
    }

    @ServiceId("sessionCheckFilter")
    public RequestFilter buildSessionCheckFilter(final Logger log, PageRenderLinkSource pageRenderLinkSource) {
        return (request, response, handler) -> {
            Session session = request.getSession(false);
//            log.debug("Session is {}", session);
            if (session != null) {
                for (String name : session.getAttributeNames()) {
                    log.debug("Session attribute {} has value {}", name, session.getAttribute(name));
                }
                log.debug("dataSessionId is {}", session.getAttribute("dataSessionId"));
            }
//            Link linkToRequestedPage = pageRenderLinkSource.createPageRenderLink(Index.class.getSimpleName());
//            boolean targetsIndex = request.getPath().contains(Index.class.getSimpleName());
//            if (!targetsIndex && session == null) {
//                log.debug("Sending redirect to Index page because the session is null.");
//                response.sendRedirect(linkToRequestedPage);
//            } else if (!targetsIndex) {
//                Object dataSessionId = session.getAttribute("dataSessionId");
//                if (dataSessionId == null || ((long) dataSessionId) == 0) {
//                    log.debug("Sending redirect to Index page because dataSessionId is 0.");
//                    response.sendRedirect(linkToRequestedPage);
//                }
//            }
            return handler.service(request, response);
        };
    }

    /**
     * This is a contribution to the RequestHandler service configuration. This is how we extend
     * Tapestry using the timing filter. A common use for this kind of filter is transaction
     * management or security. The @Local annotation selects the desired service by type, but only
     * from the same module.  Without @Local, there would be an error due to the other service(s)
     * that implement RequestFilter (defined in other modules).
     */
    @Contribute(RequestHandler.class)
    public void addTimingFilter(OrderedConfiguration<RequestFilter> configuration,
                                @InjectService("timingFilter")
                                        RequestFilter filter,
                                @InjectService("sessionCheckFilter") RequestFilter sessionCheckFilter) {
        // Each contribution to an ordered configuration has a name, When necessary, you may
        // set constraints to precisely control the invocation order of the contributed filter
        // within the pipeline.

//        configuration.add("Timing", filter);
//        configuration.add("SessionCheck", sessionCheckFilter);
    }

    /**
     * This sets up the custom "tab" state persistence strategy.
     *
     * @param configuration Service configuration.
     * @param asm           Tapestry's {@link ApplicationStateManager}.
     */
    public void contributePersistentFieldManager(MappedConfiguration<String, PersistentFieldStrategy> configuration,
                                                 ApplicationStateManager asm, LoggerSource loggerSource) {
        configuration.add(TabPersistentField.TAB, new TabPersistentField(loggerSource.getLogger(TabPersistentField.class), asm));
    }

    public void contributeApplicationStateManager(
            MappedConfiguration<Class<?>, ApplicationStateContribution> configuration, @Inject Request request,
            @Autobuild GePiSessionStateCreator sessionStateCreator) {
        configuration.add(GePiSessionState.class, new ApplicationStateContribution("session", sessionStateCreator));
    }

    /**
     * Redirect the user to the intended page when browsing through
     * tapestry forms through browser history or over-eager autocomplete
     */
    public RequestExceptionHandler decorateRequestExceptionHandler(
            final ComponentSource componentSource,
            final Response response,
            final RequestExceptionHandler oldHandler) {
        return new RequestExceptionHandler() {
            @Override
            public void handleRequestException(Throwable exception) throws IOException {
                if (exception.getMessage() == null || !exception.getMessage().contains("Forms require that the request method be POST and that the t:formdata query parameter have values")) {
                    oldHandler.handleRequestException(exception);
                    return;
                }
                ComponentResources cr = componentSource.getActivePage().getComponentResources();
                Link link = cr.createEventLink("");
                String uri = link.toRedirectURI().replaceAll(":", "");
                response.sendRedirect(uri);
            }
        };
    }
}

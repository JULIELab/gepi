package de.julielab.gepi.webapp.components;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.tapestry5.BindingConstants;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.Link;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Log;
import org.apache.tapestry5.annotations.Parameter;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.annotations.SupportsInformalParameters;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;
import de.julielab.gepi.webapp.pages.Index;

@Import(stylesheet = { "context:css-components/gepiwidgetlayout.css" })
@SupportsInformalParameters
public class GepiWidgetLayout {
	
	public enum ViewMode {
		/**
		 * The widget is in its overview mode, shown in juxtaposition to other widgets.
		 */
		OVERVIEW, 
		/**
		 * The widget covers the main view area of GePi, hiding other widgets.
		 */
		LARGE, 
		/**
		 * The widget is in fullscreen mode, covering the complete computer screen.
		 */
		FULLSCREEN
	}
	
	@Parameter(defaultPrefix = BindingConstants.LITERAL)
	@Property
	private String widgettitle;

	@Parameter(defaultPrefix = BindingConstants.LITERAL)
	@Property
	private String clientId;
	
	@Parameter(defaultPrefix = BindingConstants.LITERAL, name = "class")
	@Property
	private String classes;

	@Parameter
	@Property
	protected CompletableFuture<EventRetrievalResult> result;

	@InjectComponent
	private Zone widgetZone;

	@Inject
	private AjaxResponseRenderer ajaxResponseRenderer;

	@Inject
	private ComponentResources resources;

	@Environmental
	private JavaScriptSupport javaScriptSupport;

	@Persist
	private CompletableFuture<EventRetrievalResult> persistResult;

	@Persist
	@Property
	private String viewMode;

	void setupRender() {
		persistResult = result;
		if (result == null)
			viewMode = null;
		if (viewMode == null)
			viewMode = ViewMode.OVERVIEW.name().toLowerCase();
	}
	
	void afterRender() {
		Link eventLink = resources.createEventLink("toggleViewMode");
		javaScriptSupport.require("gepi/components/gepiwidgetlayout").invoke("setupViewModeHandle")
				.with(getResizeHandleId(), clientId, eventLink.toAbsoluteURI(), widgetZone.getClientId());
	}

	public boolean isDownload() {
		return clientId.equals("tableresult_widget");
	}
	
	public boolean isResultReady() {
		return persistResult != null && persistResult.isDone();
	}

	public boolean isResultLoading() {
		return persistResult != null && !persistResult.isDone();
	}

	void onRefreshContent() throws InterruptedException, ExecutionException {
		persistResult.get();
		ajaxResponseRenderer.addRender(widgetZone);
	}
	
	@InjectPage
	private Index index;
	void onToggleViewMode() {
		switch (viewMode) {
		case "fullscreen":
			break;
		case "large":
			viewMode = ViewMode.OVERVIEW.name().toLowerCase();
			index.setHasLargeWidget(false);
			break;
		case "overview":
			viewMode = ViewMode.LARGE.name().toLowerCase();
			index.setHasLargeWidget(true);
			break;
		}
		ajaxResponseRenderer.addRender(widgetZone);
	}

	void onLoad() {
		Link eventLink = resources.createEventLink("refreshContent");
		javaScriptSupport.require("gepi/components/gepiwidgetlayout").invoke("loadWidgetContent")
				.with(eventLink.toAbsoluteURI(), widgetZone.getClientId());
	}

	public String getZoneId() {
		String zoneId = "widgetzone_" + clientId;
		return zoneId;
	}

	public String getResizeHandleId() {
		return clientId + "_resize";
	}
	
	public String getSizeClass() {
		return viewMode;
	}
	
	@Log
	public boolean isLarge() {
		return viewMode.equals(ViewMode.LARGE.name().toLowerCase());
	}
	
	/**
	 * Pressing the Download Link/Button for the Table View
	 */
	@Log
	StreamResponse onActionFromDownload( ) {
		if (!persistResult.isDone()) {
			//TODO: how to handle case when download button is clicked, but the request is not yet fully done
		}
		return new StreamResponse() 
		{
			private InputStream inputStream;
			private String delim = "§";
			
			@Override public void prepareResponse(Response response)
			{
				EventRetrievalResult eResult = null;
				try {
					eResult = persistResult.get();
				} catch (InterruptedException | ExecutionException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				String tableresultcsv = createCSV(eResult);
				try
				{ 
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					String output = "";
					output = tableresultcsv;
					outputStream.write(output.getBytes());
					inputStream = new ByteArrayInputStream(outputStream.toByteArray());
					response.setHeader("Content-Length", "" + outputStream.size()); // output into file
					response.setHeader("Content-Length", "" + inputStream.available());
					response.setHeader("Content-disposition", "attachment; filename=gepi_table.csv");
				} catch (IOException e)
				{
						e.printStackTrace();
				}
			}
			
			@Override public InputStream getStream() throws IOException
			{
				return inputStream;
			}
			
			@Override public String getContentType()
			{ 
				return "text/csv";
			}
			
			private String createCSV(EventRetrievalResult eResult) {
				StringBuilder sResult = new StringBuilder();
				for (Event e : eResult.getEventList()) {
					Argument firstArgument = e.getFirstArgument();
					Argument secondArgument = e.getSecondArgument();
					String gene1Text = firstArgument.getText();
					String gene2Text = secondArgument.getText();
					String gene1ID = firstArgument.getGeneId();
					String gene2ID = secondArgument.getGeneId();
					String gene1PrefName = firstArgument.getPreferredName();
					String gene2PrefName = secondArgument.getPreferredName();
					String medlineID = "Medline";
					String pmcID = "PMC";
					String sentence = e.getSentence();
					
					sResult.append(gene1Text + delim);
					sResult.append(gene1ID + delim);
					sResult.append(gene1PrefName + delim);
					sResult.append(gene2Text + delim);
					sResult.append(gene2ID + delim);
					sResult.append(gene2PrefName + delim);
					sResult.append(medlineID + delim);
					sResult.append(pmcID + delim);
					sResult.append(sentence.replaceAll("\\R", " ") + System.getProperty("line.separator"));
				};
				
				return sResult.toString();
			}
		};
	}
}

package de.julielab.gepi.webapp.pages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.StreamResponse;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.ActivationRequestParameter;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Log;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.HttpError;
import org.apache.tapestry5.services.Response;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.slf4j.Logger;

import de.julielab.gepi.core.retrieval.data.Argument;
import de.julielab.gepi.core.retrieval.data.Event;
import de.julielab.gepi.core.retrieval.data.EventRetrievalResult;

/**
 * Start page of application gepi-webapp.
 */
@Import(stylesheet = { "context:css-pages/index.less" })
public class Index {
	@Inject
	private Logger logger;

	@Environmental
	private JavaScriptSupport javaScriptSupport;

	@Property
	@Inject
	@Symbol(SymbolConstants.TAPESTRY_VERSION)
	private String tapestryVersion;

	@InjectPage
	private About about;

	@InjectComponent
	private Zone outputZone;

	@InjectComponent
	private Zone inputZone;

	@Property
	@Persist
	private CompletableFuture<EventRetrievalResult> result;

	@Property
	private Event eventItem;
	
	@Persist
	private boolean hasLargeWidget;
	
	/**
	 * This is an emergency exit against being locked in an error during development.
	 */
	@ActivationRequestParameter
	private boolean reset;
	
	public Zone getOutputZone() {
		return outputZone;
	}

	public Zone getInputZone() {
		return inputZone;
	}

	// Handle call with an unwanted context
	Object onActivate(EventContext eventContext) {
		if (reset)
			result = null;
		return eventContext.getCount() > 0 ? new HttpError(404, "Resource not found") : null;
	}
	
	void afterRender() {
		javaScriptSupport.require("gepi/pages/index").invoke("loadGoogleCharts");
	}
	
	/**
	 * Pressing the Download Link/Button for the Table View
	 */
	@Log
	StreamResponse onActionFromDownload( ) {
		
		return new StreamResponse() 
		{
			private InputStream inputStream;
			private String delim = "§";
			
			@Override public void prepareResponse(Response response)
			{
				EventRetrievalResult eResult = null;
				try {
					eResult = result.get();
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
	
	/**
	 * 
	 * @return The class "in", causing the outputcol to show immediately, or the empty string which will hide the outputcol initially.
	 */
	public String getShowOutputClass() {
		if (result != null && result.isDone())
			return "in";
		return "";
	}
	
	public String getShowInputClass() {
		if (result == null)
			return "in";
		return "";
	}
	
	public Object onReset() {
		result = null;
		return this;
	}

	public boolean hasLargeWidget() {
		return hasLargeWidget;
	}

	public void setHasLargeWidget(boolean hasLargeWidget) {
		this.hasLargeWidget = hasLargeWidget;
	}
	
	public String getBodyScrollClass() {
		return hasLargeWidget ? "noScroll" : "";
	}
	
	public String getWidgetOverlayShowClass() {
		return hasLargeWidget ? "in" : "";
	}
	
}

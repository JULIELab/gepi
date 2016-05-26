package de.julielab.gepi.webapp.pages;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.tapestry5.EventContext;
import org.apache.tapestry5.SymbolConstants;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.InjectPage;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.TextArea;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.ioc.annotations.Symbol;
import org.apache.tapestry5.services.HttpError;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;
import org.slf4j.Logger;

import de.julielab.gepi.webapp.entities.Interaction;

/**
 * Start page of application gepi-webapp.
 */
public class Index {
	@Inject
	private Logger logger;

	@Inject
	private AjaxResponseRenderer ajaxResponseRenderer;

	@Property
	@Inject
	@Symbol(SymbolConstants.TAPESTRY_VERSION)
	private String tapestryVersion;

	@InjectPage
	private About about;

	@InjectComponent
	private Zone resultZone;

	@Inject
	private Request request;

	@Environmental
	private JavaScriptSupport javaScriptSupport;

	@InjectComponent
	private Form inputForm;
	
	@InjectComponent
	private TextArea lista;
	
	@Property
	@Persist
	private int counter;

	@Property
	private String listATextAreaValue;
	
	@Property
	private String listBTextAreaValue;
	
	@Property
	@Persist
	private List<Interaction> interactions;

	// Handle call with an unwanted context
	Object onActivate(EventContext eventContext) {
		return eventContext.getCount() > 0 ? new HttpError(404, "Resource not found") : null;
	}

	Object onActionFromLearnMore() {
		about.setLearn("LearnMore");

		return about;
	}

	void onValidateFromInputForm() {

		// Note, this method is triggered even if server-side validation has
		// already found error(s).

		if (listATextAreaValue == null || listATextAreaValue.isEmpty()) {
		 inputForm.recordError(lista, "List A must not be empty.");
		 return;
		}
		
		File file = new File("relationsPmc.lst");
		System.out.println(file.getAbsolutePath());
		try {
			interactions = new ArrayList<>();
			LineIterator lineIterator = IOUtils.lineIterator(new FileInputStream(file), "UTF-8");
			while (lineIterator.hasNext()) {
				String line = (String) lineIterator.next();
				String[] interactionRecord = line.split("\t");
				Interaction interaction = new Interaction();
				interaction.setDocumentId(interactionRecord[0]);
				interaction.setInteractionPartner1Id(interactionRecord[1]);
				interaction.setInteractionPartner2Id(interactionRecord[2]);
				interaction.setInteractionPartner1Text(interactionRecord[3]);
				interaction.setInteractionPartner2Text(interactionRecord[4]);
				interaction.setInteractionType(interactionRecord[6]);
				interaction.setSentenceText(interactionRecord[7]);
				interactions.add(interaction);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	void onSuccess() {
		++counter;
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(resultZone);
		}
	}

	void onFailure() {
		--counter;
		if (request.isXHR()) {
			ajaxResponseRenderer.addRender(resultZone);
		}
	}

	void afterRender() {
		javaScriptSupport.require("gepi/pages/index").invoke("initialize");
	}

}

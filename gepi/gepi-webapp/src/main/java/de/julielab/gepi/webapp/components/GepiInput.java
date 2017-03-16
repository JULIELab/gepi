package de.julielab.gepi.webapp.components;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.tapestry5.ComponentResources;
import org.apache.tapestry5.annotations.Environmental;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Log;
import org.apache.tapestry5.annotations.Persist;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.TextArea;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.json.JSONObject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;
import org.apache.tapestry5.services.javascript.JavaScriptSupport;

import de.julielab.gepi.webapp.entities.Interaction;
import de.julielab.gepi.webapp.pages.Index;

public class GepiInput {

	@Inject
	private AjaxResponseRenderer ajaxResponseRenderer;

	@Inject
	private Request request;

	@Environmental
	private JavaScriptSupport javaScriptSupport;

	@InjectComponent
	private Form inputForm;

	@InjectComponent
	private TextArea lista;
	
	@InjectComponent
	private TextArea listb;

	@Property
	private String listATextAreaValue;

	@Property
	private String listBTextAreaValue;

	@Property
	@Persist
	private List<Interaction> interactions;

	@Property
	@Persist
	private JSONObject pieData;

//	@InjectComponent
//	private Zone inputFormZone;
	
	@Inject
	private ComponentResources resources;

	void setupRender() {
	}
	
	@Log
	void onValidateFromInputForm() {
		// Note, this method is triggered even if server-side validation has
		// already found error(s).

		if (listATextAreaValue == null || listATextAreaValue.isEmpty()) {
			inputForm.recordError(lista, "List A must not be empty.");
			return;
		}
	}

	@Log
	void onSuccessFromInputForm() {
		Index indexPage = (Index) resources.getContainer();
		ajaxResponseRenderer.addRender(indexPage.getOutputZone()).addRender(indexPage.getInputZone());
		File file = new File("relationsPmc.lst");
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
			pieData = new JSONObject();
			pieData.put("c-Jun", 10);
			pieData.put("Raptor", 7);
			pieData.put("Rab", 13);
			pieData.put("IRS-1", 3);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void onFailure() {
		if (request.isXHR()) {
			Index indexPage = (Index) resources.getContainer();
			ajaxResponseRenderer.addRender(indexPage.getOutputZone()).addRender(indexPage.getInputZone());
		}
	}
	
	@Log
	void afterRender() {
		javaScriptSupport.require("gepi/components/gepiinput").invoke("initialize").with(lista.getClientId(), listb.getClientId());
	}
	
}

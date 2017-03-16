package de.julielab.gepi.webapp.pages;

import java.util.Date;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;

import org.apache.tapestry5.annotations.Import;
import org.apache.tapestry5.annotations.InjectComponent;
import org.apache.tapestry5.annotations.Property;
import org.apache.tapestry5.corelib.components.Form;
import org.apache.tapestry5.corelib.components.TextField;
import org.apache.tapestry5.corelib.components.Zone;
import org.apache.tapestry5.ioc.annotations.Inject;
import org.apache.tapestry5.services.Request;
import org.apache.tapestry5.services.ajax.AjaxResponseRenderer;

@Import(stylesheet = "js.css")
public class FormTest {
	// Screen fields

    @Property
    @NotNull
    private String firstName;

    @Property
    @NotNull
    private String lastName;

    @Property
    @NotNull
    @Past
    private Date birthday;

    // Generally useful bits and pieces

    @Inject
    private Request request;

    @InjectComponent("ajaxForm")
    private Form form;

    @InjectComponent("firstName")
    private TextField firstNameField;

    @InjectComponent
    private Zone formZone;

    @Inject
    private AjaxResponseRenderer ajaxResponseRenderer;

    // The code

    void setupRender() {
        if (firstName == null && lastName == null && birthday == null) {
            firstName = "Humpty";
            lastName = "Dumpty";
            birthday = new Date(0);
        }
    }

    void onValidateFromAjaxForm() {

        // Note, this method is triggered even if server-side validation has already found error(s).

    	System.out.println(firstName);
        if (firstName != null && firstName.equals("Acme")) {
        	System.out.println("Fehler");
            form.recordError(firstNameField, "First Name must not be Acme.");
        }

    }

    void onSuccess() {
        if (request.isXHR()) {
        	System.out.println("Success");
            ajaxResponseRenderer.addRender(formZone);
        }
    }

    void onFailure() {
        if (request.isXHR()) {
        	System.out.println("Failure");
            ajaxResponseRenderer.addRender(formZone);
        }
    }

    public String getName() {
        return firstName + " " + lastName;
    }

    public Date getServerTime() {
        return new Date();
    }
}

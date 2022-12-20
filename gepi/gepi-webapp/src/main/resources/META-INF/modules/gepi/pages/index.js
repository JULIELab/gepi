define(["jquery", "bootstrap5/modal"], function($, Modal){

    // The following is synchronization code. The widgets wait for this
    // deferred object to be resolved before they render. We use this
    // to wait for animations to finish before rendering diagrams.

    let readyIndicator = $.Deferred();

    let readyForWidgets = function () {
        console.log("Resolving readyIndicator");
        readyIndicator.resolve();
    };

    let getReadySemaphor = function () {
        return readyIndicator;
    }

    let displayCookieConsentModal = function () {
         const modal = new Modal(document.getElementById('cookieConsentModal'), {backgrop:'static', focus: true, keyboard: false})
         modal.show();
    }

    return {readyForWidgets, getReadySemaphor, displayCookieConsentModal};
    
});
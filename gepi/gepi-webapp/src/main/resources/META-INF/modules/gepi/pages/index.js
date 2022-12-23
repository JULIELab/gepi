define(["jquery", "bootstrap5/modal", "bootstrap5/tooltip"], function($, Modal, Tooltip){

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

    let setupDownloadUrlCopyButton = function () {
        const button = document.getElementById("downloadUrlCopyButton");
        const copyMessageTooltip = new Tooltip(button, {"trigger":"click", "title": "Copied!"})
        $(button).on("click", () => {
            // Get the text field
            const copyText = document.getElementById("downloadUrlCopyTextField");

            // Select the text field
            copyText.select();
            copyText.setSelectionRange(0, 99999); // For mobile devices

             // Copy the text inside the text field
            navigator.clipboard.writeText(copyText.value);

            setTimeout(() => copyMessageTooltip.hide(), 2000);
        });

    }

    return {readyForWidgets, getReadySemaphor, displayCookieConsentModal, setupDownloadUrlCopyButton};
    
});
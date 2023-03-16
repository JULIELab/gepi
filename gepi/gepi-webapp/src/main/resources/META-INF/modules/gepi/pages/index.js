define(["jquery", "bootstrap5/modal", "bootstrap5/tooltip", "bootstrap5/offcanvas", "bootstrap5/toast"], function($, Modal, Tooltip, Offcanvas, Toast){

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

    let displayCookieConsentOffcanvas = function () {
//            $('#cookie-consent-offcanvas-close-btn').on('click', displayRoadworksWarningToast);
            const offcanvas = new Offcanvas(document.getElementById('cookie-consent-offcanvas'), {keyboard:false, scroll: true, backdrop: false})
            offcanvas.show();
    }

    let displayRoadworksWarningToast = function () {
             const modal = new Toast(document.getElementById('roadworks-warning-toast'), {delay:10000, autohide:true})
             modal.show();
     }

    let setupDownloadUrlCopyButton = function () {
        const button = document.getElementById("downloadUrlCopyButton");
        let tooltipTitle = window.isSecureContext ? "Copied!" : "Copy failed: Connect via HTTPS to enable automatic copying."
        let tooltipHideDelay = window.isSecureContext ? 2000 : 7000;
        const copyMessageTooltip = new Tooltip(button, {"trigger":"click", "title": tooltipTitle})
        $(button).on("click", () => {
            // Get the text field
            const copyText = document.getElementById("downloadUrlCopyTextField");

            // Select the text field
            copyText.select();
            if (window.isSecureContext) {
                copyText.setSelectionRange(0, 99999); // For mobile devices
                console.log("COPYING; is secure context: "+ window.isSecureContext)
                 // Copy the text inside the text field
                navigator.clipboard.writeText(copyText.value);

             }
             setTimeout(() => copyMessageTooltip.hide(), tooltipHideDelay);
        });
    }

    return {readyForWidgets, getReadySemaphor,  displayCookieConsentOffcanvas,  displayRoadworksWarningToast, setupDownloadUrlCopyButton};
    
});
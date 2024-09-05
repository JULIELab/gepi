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
            let cookieConsentPanel = document.getElementById('cookie-consent-offcanvas')
            // The panel is not there if we show the maintenance-sign
            if (cookieConsentPanel) {
                const offcanvas = new Offcanvas(cookieConsentPanel, {keyboard:false, scroll: true, backdrop: false})
                offcanvas.show();
            }
    }

    let displayRoadworksWarningToast = function () {
             const modal = new Toast(document.getElementById('roadworks-warning-toast'), {delay:10000, autohide:false})
             modal.show();
     }

    let setupDownloadUrlCopyButton = function () {
        const button = document.getElementById("downloadUrlCopyButton");
        // The button is not there if we show the maintenance sign
        if (button) {
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
    }

    return {readyForWidgets, getReadySemaphor,  displayCookieConsentOffcanvas,  displayRoadworksWarningToast, setupDownloadUrlCopyButton};
    
});
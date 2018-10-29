define([ "jquery", "bootstrap/tooltip" ], function($) {

    var initialize = function() {
        var listaId = "lista";
        var listbId = "listb";
        var lista = '#' + listaId;
        var listb = '#' + listbId;

        observelistbchange();
        togglelistb();
        observelistachange();
        setuplistfileselectors();
        setupclearbuttons();
        setupShowInputPanel();

        /*
         * On changes of list B, checks if the list is empty. If not, some
         * control elements for single-list operations are disabled.
         */
        function observelistbchange() {
            $(listb).on("input change", function() {
                togglelistaoptions();
            });
        }

        /*
         * On changes of list A, checks if the list is empty. If not, list B is
         * enabled, otherwise it isn't available for input.
         */
        function observelistachange() {
            $(lista).on("input change", function() {
                togglelistb();
            });
        }

        /*
         * Sets up the "select file" buttons for ID lists A and B to put the
         * contents of the selected files into the correct text area.
         */
        function setuplistfileselectors() {
            var fileainput = $("#fileainput");
            fileainput.on("change", function() {
                loadfile(fileainput[0], listaId, function() {
                    togglelistb();
                    // since we load a file on "change", we reset the value
                    // after the file has been loaded; otherwise you couldn't
                    // load a file, clear the field and load the file again
                    fileainput.val("");
                })
            });
            var filebinput = $("#filebinput");
            filebinput.on("change", function() {
                loadfile(filebinput[0], listbId, function() {
                    togglelistaoptions();
                    // since we load a file on "change", we reset the value
                    // after the file has been loaded; otherwise you couldn't
                    // load a file, clear the field and load the file again
                    filebinput.val("");
                });
            });
        }

        function setupclearbuttons() {
            $("#cleara").on("click", function() {
                $(lista).val("");
                togglelistb();
            });
            $("#clearb").on("click", function() {
                $(listb).val("");
                togglelistaoptions();
            });
        }

        /*
         * After selecting a file with IDs, this function reads the file and
         * pastes its contents into the respective text area.
         */
        function loadfile(input, textarea, callback) {
            var reader = new FileReader();
            reader.onload = function(e) {
                document.getElementById(textarea).value = e.target.result;
                if ($.isFunction(callback))
                    callback();
            }
            reader.readAsText(input.files[0]);
        }

        /*
         * Deactivates the single-list options of ID list A one list B is not
         * empty.
         */
        function togglelistaoptions() {
            var islistbempty = $(listb).val().length == 0;
            var listaoptions = $("#listaoptions input")
            if (islistbempty) {
                listaoptions.attr("disabled", false);
            } else {
                listaoptions.attr("disabled", true);
            }
        }

        /*
         * Deactivates or activates the whole list B area (and buttons) if there
         * is no value in list A yet.
         */
        function togglelistb() {
            var islistaempty = $(lista).val().length == 0;
            var listbelements = $("#listbdiv textarea, #listbdiv label, #listbdiv button")
            var listbdiv = $("#listbdiv");
            if (islistaempty) {
                listbelements.attr("disabled", true);
                listbdiv.tooltip("enable");
            } else {
                listbelements.attr("disabled", false);
                listbdiv.tooltip("disable");
            }
        }

        function setupShowInputPanel() {
            $("#handle").on("click", function(){toggleShowInputPanel();})
        }
    };

    function toggleShowInputPanel() {
                console.log("Input shown: " + $("#inputcol").data("shown"))
                if ($("#inputcol").data("shown") === 0){
                    console.log("Showing input")
                    showInput();
                }
                else {
                    console.log("Hiding input")
                    showOutput();
                }
            }

    var showOutput = function() {
        console.log("Hiding the input, showing the output")
        // The inputcol's width is one third. However, when we push it out of
        // the screen for one third, the body container's padding won't be
        // accounted for and the inputcol would still be visible for this exact
        // amount. Thus, we also have to add the padding when computing the
        // negative left margin.
        // However, when we would just set the negative left margin for this
        // exact number of pixels, resizing the viewport will lead to the
        // inputcol to grow since its width is defined in percent. As the margin
        // would be defined absolute, the inputcol would be visible again at the
        // left border of the screen. Thus, we need to compute the percentage
        // the margin needs to have so that it also adjusts automatically.
        var availableWidth = $("#body-container").innerWidth();
        var bodyPadding = parseFloat($("#body-container").css("padding-left"));
        var marginLeft = -(availableWidth / 3) - bodyPadding;
        var marginLeftPercent = marginLeft / availableWidth * 100;

        // Show the outputcol. The CSS defines all the timings, including a
        // delay for the let the inputcol disappear first.
        $("#inputcol").css("margin-left", marginLeftPercent + "%");
        $("#outputcol").addClass("in");
        $("#inputcol").data("shown", 0)
        var semaphor = $.Deferred();
        $("#inputcol").data("animationtimer", semaphor);
        setTimeout(() => semaphor.resolve(), 1000);

    }

    var showInput = function() {
        console.log("Fetching the input panel back into view")
        $("#outputcol").removeClass("in").addClass("fade");
        $("#inputcol").css("margin-left", 0);
        $("#inputcol").data("shown", 1);
    }

    return {
        "initialize" : initialize,
        "showOutput" : showOutput,
        "showInput"  : showInput
    };
})
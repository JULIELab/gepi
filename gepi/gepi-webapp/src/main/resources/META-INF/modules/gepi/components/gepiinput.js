define(["jquery", "gepi/pages/index", "gepi/charts/data", "bootstrap/tooltip"], function($, index, data) {

    let initialize = function(resultExists) {
        console.log("Initializing the input panel");
        let listaId = "lista";
        let listbId = "listb";
        let lista = '#' + listaId;
        let listb = '#' + listbId;
        inputCol = $("#inputcol");
        inputColHandle = $("#inputcolhandle");

        observelistbchange();
        togglelistb();
        observelistachange();
        setuplistfileselectors();
        setupclearbuttons();
        setupShowInputPanel();
        observekeypress();
        observeFormSubmit();
        observeInputFetchArea();

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
            let fileainput = $("#fileainput");
            fileainput.on("change", function() {
                loadfile(fileainput[0], listaId, function() {
                    togglelistb();
                    // since we load a file on "change", we reset the value
                    // after the file has been loaded; otherwise you couldn't
                    // load a file, clear the field and load the file again
                    fileainput.val("");
                });
            });
            let filebinput = $("#filebinput");
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
            let reader = new FileReader();
            reader.onload = function(e) {
                document.getElementById(textarea).value = e.target.result;
                if ($.isFunction(callback))
                    callback();
            };
            reader.readAsText(input.files[0]);
        }

        /*
         * Deactivates the single-list options of ID list A one list B is not
         * empty.
         */
        function togglelistaoptions() {
            let islistbempty = $(listb).val().length == 0;
            let listaoptions = $("#listaoptions input");
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
            let islistaempty = $(lista).val().length == 0;
            let listbelements = $("#listbdiv textarea, #listbdiv label, #listbdiv button")
            let listbdiv = $("#listbdiv");
            if (islistaempty) {
                listbelements.attr("disabled", true);
                listbdiv.tooltip("enable");
            } else {
                listbelements.attr("disabled", false);
                listbdiv.tooltip("disable");
            }
        }

        function setupShowInputPanel() {
            let button = $("#inputToggleButton");
            console.log("[setupShowInputPanel] resultExists: " + resultExists);
            if (resultExists) {
                if (button.hasClass("disabled")) {
                    button.addClass("navbar-highlight");
                    setTimeout(() => button.removeClass("navbar-highlight"), 3000);
                }
                $("#inputToggleButton").removeClass("disabled");
                hideInput();
                $("#inputToggleButton,#disableplane").off("click");
                $("#inputToggleButton,#disableplane,#inputcolhandle").on("click", function() {
                 toggleShowInputPanel();
                });
            } else {
                $("#inputToggleButton").addClass("disabled");
            }
        }

        function observekeypress() {
        function KeyPress(e) {
              let evt = window.event || e;

              if ((evt.metaKey || evt.ctrlKey) && evt.keyCode == 83){
                inputCol.find("form").submit();
                // prevents the default action (opening a saving dialog)
                return false;
                }
        }

        inputCol.on("keydown", KeyPress);
        }

        function observeFormSubmit() {
            form = $("form[id^='input']");
            form.on("submit", form => {console.log("Search form was submitted, clearing data chache."); data.clearData();});
        }

        function observeInputFetchArea() {
            let inputFetchArea = $("#inputfetcharea,#inputcolhandle");
            console.log("Einrichtung")
            inputFetchArea.hover(function() {
                console.log("rein")
                inputColHandle.removeClass("inputcolhandle-retracted");
                inputColHandle.addClass("inputcolhandle-extended");
                },
                function() {
                    console.log("raus")
                    inputColHandle.removeClass("inputcolhandle-extended");
                    inputColHandle.addClass("inputcolhandle-retracted");
                }
            );
        }
    };



    function toggleShowInputPanel() {
        let shown = !inputCol.css("margin-left").includes("-");

        if (!shown || shown === 0) {
            console.log("Showing input");
            showInput();
        } else {
            console.log("Hiding input");
            showOutput();
        }
    }

    let showOutput = function() {
        console.log("Showing output widgets");
        hideInput();
        $("#disableplane").removeClass("show");
        $("#outputcol").addClass("show");
        let semaphor = $.Deferred();
        inputCol.data("animationtimer", semaphor);
        setTimeout(() => semaphor.resolve(), 300);
        semaphor.then(() => inputCol.addClass("hidden"));
        console.log("Marking as being ready for widgets");
        index.readyForWidgets();
    };

    let showInput = function() {
        console.log("Showing input panel");
        inputCol.css("margin-left", "");
        inputColHandle.removeClass("show");
        $("#disableplane").addClass("show");
        inputColHandle.removeClass("background-arrow-right inputcolhandle-retracted");
        inputColHandle.addClass("background-arrow-left inputcolhandle-extended");
    };

    let hideInput = function() {
        console.log("Hiding input panel");
        let inputColWidth = parseInt(inputCol.css("width").slice(0, -2));
        let inputColPadding = parseInt(inputCol.css("padding-right").slice(0, -2));
        inputCol.css("margin-left", "-"+(inputColWidth-inputColPadding)+"px");
        inputColHandle.removeClass("background-arrow-left inputcolhandle-extended");
        inputColHandle.addClass("background-arrow-right inputcolhandle-retracted");
    }

    return {
        "initialize": initialize,
        "showOutput": showOutput,
        "showInput": showInput
    };
})
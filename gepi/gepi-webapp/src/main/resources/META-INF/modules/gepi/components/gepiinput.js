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
        setuplistfiledragndrop(listaId);
        setuplistfiledragndrop(listbId);
        setupclearbuttons();
        setupShowInputPanel();
        observekeypress();
        observeFormSubmit();
        observeInputFetchArea();
        let running = false;
        window.addEventListener('resize',() => {
            if (!running) {
                running = true;
                let shown = !inputCol.css("margin-left").includes("-");ing = false;
                if (!shown) {
                    window.setTimeout(() => {hideInput(); running = false;}, 1000);
                }
            }
        });

        /*
         * On changes of list B, checks if the list is empty. If not, some
         * control elements for single-list operations are disabled.
         */
        function observelistbchange() {
            $(listb).on("input change", function() {
//                togglelistaoptions();
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

        /*
         * Enabled drag & drop functionality for a text area. We use it to drop file lists to the listA and listB
         * text areas.
         */

        function setuplistfiledragndrop(textAreaId) {
            textarea = document.getElementById(textAreaId);
            textarea.addEventListener("dragenter", dragenter, false);
            textarea.addEventListener("dragover", dragover, false);
            textarea.addEventListener("drop", e => drop(e, textAreaId), false);
        }

        /* Prohibit unwanted side effects from other component through the dragging */
        function dragenter(e) {
          e.stopPropagation();
          e.preventDefault();
        }

        /* Prohibit unwanted side effects from other component through the dragging */
        function dragover(e) {
          e.stopPropagation();
          e.preventDefault();
        }

        /* Load a file dragged on a text area and set the contents to the text area */
        function drop(e, textarea) {
          e.stopPropagation();
          e.preventDefault();

          const dt = e.dataTransfer;

          loadfile(dt, textarea, togglelistb);
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
        // There is currently nothing to do. The options below currently do not exist.
//            let islistbempty = $(listb).val().length == 0;
//            let listaoptions = $("#listaoptions input");
//            if (islistbempty) {
//                listaoptions.attr("disabled", false);
//            } else {
//                listaoptions.attr("disabled", true);
//            }
        }

        /*
         * Deactivates or activates the whole list B area (and buttons) if there
         * is no value in list A yet.
         */
        function togglelistb() {
            let islistaempty = $(lista).val().length == 0;
            let listbelements = $("#listbdiv textarea, #listbdiv input, #listbdiv button");
            let selectFile = $("#listbdiv label");
            let listbdiv = $("#listbdiv");
            if (islistaempty) {
                listbelements.attr("disabled", true);
                selectFile.addClass("disabled");
                listbdiv.tooltip("enable");
            } else {
                listbelements.attr("disabled", false);
                selectFile.removeClass("disabled");
                listbdiv.tooltip("disable");
            }
        }

        function setupShowInputPanel() {
            if (resultExists) {
                hideInput();
                $("#disableplane").off("click");
                $("#disableplane,#inputcolhandle").on("click", function() {
                 toggleShowInputPanel();
                });
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
            console.log("Adding submit listener")
            form.on("submit", form => {console.log("Search form was submitted, clearing data cache."); data.clearData();});
            // scroll to top; sometimes one needs to scroll down to find the submit button
            form.on("submit", form => {window.scrollTo(0, 0, {"behavior":"smooth"})});
        }

        function observeInputFetchArea() {
            let inputFetchArea = $("#inputfetcharea,#inputcolhandle");
            inputFetchArea.hover(() => {
                    inputColHandle.removeClass("inputcolhandle-retracted");
                    inputColHandle.addClass("inputcolhandle-extended");
                },
                () => {
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
        inputColHandle.removeClass("background-arrow-left fade inputcolhandle-extended");
        inputColHandle.addClass("background-arrow-right inputcolhandle-retracted");
    }

    return {
        "initialize": initialize,
        "showOutput": showOutput,
        "showInput": showInput
    };
})
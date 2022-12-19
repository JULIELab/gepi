define(["jquery", "gepi/pages/index", "gepi/charts/data", "bootstrap5/tooltip"], function($, index, data, Tooltip) {

    let initialize = function(resultExists) {
        const listaId = "lista";
        const listbId = "listb";
        const lista = '#' + listaId;
        const listb = '#' + listbId;
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
        setupInputExamples();
        let running = false;
        window.addEventListener('resize', () => {
            if (!running) {
                running = true;
                let shown = !inputCol.css("margin-left").includes("-");
                ing = false;
                if (!shown) {
                    window.setTimeout(() => {
                        hideInput();
                        running = false;
                    }, 1000);
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

                if ((evt.metaKey || evt.ctrlKey) && evt.keyCode == 83) {
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
            form.on("submit", form => {
                console.log("Search form was submitted, clearing data cache.");
                data.clearData();
            });
            // scroll to top; sometimes one needs to scroll down to find the submit button
            form.on("submit", form => {
                window.scrollTo(0, 0, {
                    "behavior": "smooth"
                })
            });
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

        function setupInputExamples() {
            const formElementIds = {
                listaTextAreaId                : "lista",
                listbTextAreaId                : "listb",
                orgTextFieldId                 : "organismInput",
                eventTypeChecklistId           : "eventtypes",
                negRegulationCheckboxSelector  : "#eventtypes input[value='Negative_regulation']",
                radioLikelihoodNegRadioClientId: "radio_likelihood_negation",
                radioLikelihoodModRadioClientId: "radio_likelihood_moderate",
                includeUnaryId                 : "includeUnary",
                sentenceTextFieldId            : "sentencefilter",
                filterOperatorAndRadioClientId : "and",
                filterOperatorOrRadioClientId  : "or",
                paragraphTextFieldId           : "paragraphfilter",
                sectionNameTextFieldId         : "sectionnamefilter"
             };

            $("#btn-clear-input").on("click", () => resetInputFields(formElementIds));
            document.querySelectorAll("button.example-input").forEach(btn => new Tooltip(btn, {trigger: "hover", placement: "bottom"}));
            setupInputExample1(formElementIds);
            setupInputExample2(formElementIds);
            setupInputExample3(formElementIds);
        }

        function setupInputExample1(formElementIds) {
            const btn = $("#btn-example-1");
            // https://www.abeomics.com/brca1-pathway
            btn.on("click", () => {
                resetInputFields(formElementIds);
                const listaTextArea = document.getElementById(formElementIds.listaTextAreaId);
                const radioLikelihoodModRadio = document.querySelector(`input[clientid='${formElementIds.radioLikelihoodModRadioClientId}'`);
                const sentenceTextField = document.getElementById(formElementIds.sentenceTextFieldId);
                const filterOperatorOrRadio = document.querySelector(`input[clientid='${formElementIds.filterOperatorOrRadioClientId}'`);
                const paragraphTextField = document.getElementById(formElementIds.paragraphTextFieldId);
                listaTextArea.value = ["fanc", "gene:571", "hgnc:HGNC:20473"].reduce((acc, x) => acc + "\n" + x);
                radioLikelihoodModRadio.checked = true;
                sentenceTextField.value = 'stress';
                filterOperatorOrRadio.checked = true;
                paragraphTextField.value = 'stress';
            });
        }

        function setupInputExample2(formElementIds) {
            $("#btn-example-2").on("click", () => {
                resetInputFields(formElementIds);
                const listaTextArea = document.getElementById(formElementIds.listaTextAreaId);
                const listbTextArea = document.getElementById(formElementIds.listbTextAreaId);
                const orgTextField = document.getElementById(formElementIds.orgTextFieldId);
                listaTextArea.value = ["207", "208", "3611", "5578", "5579", "5591", "9261", "10000", "11651", "11652", "16202", "17164", "18750", "18751", "19090", "23797", "24185", "24680", "25023", "25233", "29110", "29414", "35329", "41957", "48311", "53573", "56480", "66725", "120892"]
                    .reduce((acc, x) => acc + "\n" + x);
                listbTextArea.value = ["HRAS", "KRAS", "MAP2K1", "MAP2K2", "MAPK1", "MAPK3", "NRAS", "RAF1"].reduce((acc, x) => acc + "\n" + x);
                togglelistb();
                orgTextField.value = "9606";
            });
        }

        function setupInputExample3(formElementIds) {
            $("#btn-example-3").on("click", () => {
                resetInputFields(formElementIds);
                // uncheck all but negative regulation
                document.querySelectorAll(`#${formElementIds.eventTypeChecklistId} input:not([value='Negative_regulation'])`).forEach(checkbox => checkbox.checked = false);
                const includeUnaryCheckbox = document.getElementById(formElementIds.includeUnaryId);
                const paragraphTextField = document.getElementById(formElementIds.paragraphTextFieldId);
                includeUnaryCheckbox.checked = true;
                paragraphTextField.value = '"MAPK pathway" | "MAPK signaling" | "MAPK signaling pathway" | "MAPK signal transduction pathway" | "MAPK cascade" | "MAP kinase pathway"';
            });
        }

        function resetInputFields(formElementIds) {
            const listaTextArea = document.getElementById(formElementIds.listaTextAreaId);
            const listbTextArea = document.getElementById(formElementIds.listbTextAreaId);
            const orgTextField = document.getElementById(formElementIds.orgTextFieldId);
            const eventTypeCheckboxes = document.querySelectorAll(`#${formElementIds.eventTypeChecklistId} input`);
            const radioLikelihoodNegRadio = document.querySelector(`input[clientid='${formElementIds.radioLikelihoodNegRadioClientId}'`);
            const includeUnaryCheckbox = document.getElementById(formElementIds.includeUnaryId);
            const sentenceTextField = document.getElementById(formElementIds.sentenceTextFieldId);
            const filterOperatorAndRadio = document.querySelector(`input[clientid='${formElementIds.filterOperatorAndRadioClientId}'`);
            const paragraphTextField = document.getElementById(formElementIds.paragraphTextFieldId);
            const sectionNameTextField = document.getElementById(formElementIds.sectionNameTextFieldId);

            listaTextArea.value = "";
            listbTextArea.value = "";
            orgTextField.value = "";
            eventTypeCheckboxes.forEach(box => box.checked=true);
            includeUnaryCheckbox.checked = false;
            radioLikelihoodNegRadio.checked = true;
            sentenceTextField.value = "";
            filterOperatorAndRadio.checked = true;
            paragraphTextField.value = "";
            sectionNameTextField.value = "";

            togglelistb();
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
        inputCol.css("margin-left", "-" + (inputColWidth - inputColPadding) + "px");
        inputColHandle.removeClass("background-arrow-left fade inputcolhandle-extended");
        inputColHandle.addClass("background-arrow-right inputcolhandle-retracted");
    }

    return {
        "initialize": initialize,
        "showOutput": showOutput,
        "showInput": showInput
    };
})
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

        $('#inputform').on("t5.form.validate", function() {
            console.log("validate!!")
        })

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
    };

    var showOutput = function() {
        var inputPaddingLeft = parseFloat($("#inputcol").css("padding-left"))
        var inputPaddingRight = parseFloat($("#inputcol").css("padding-right"))

        // Configure the output column to:
        // * fade.IN
        // * flow right (growing) so it will expand to the left via
        // * larger1 which uses a keyframe to change the width from 1/3% to 2/3%
        $("#outputcol").addClass("in growing larger1");
        // This is actually the first visiable movement: remove the offset that
        // keeps the input col in the page center
        $("#inputcol").removeClass("col-md-offset-4");
        // After the first shift is completed (set in the CSS to take 1s), now
        // tell the output column to shift to 100% width
        setTimeout(function() {
            $("#outputcol").addClass("larger2");
        }, 1000)
        // at the same time, we must remove the inputcol from the relative
        // positioning flow
        // or the outputcol will be wrapped below the inputcol since there would
        // be no 100% available (the wrapping does happen despite the fact that
        // the change from 2/3% to 100% happens slowly in an animation, I don't
        // know why)
        setTimeout(function() {
            $("#inputcol").css({
                "position" : "absolute",
                "left" : -(inputPaddingLeft + inputPaddingRight + $("#inputcol").width())
            });
        }, 1000);
        // After two seconds, the grand finally: the inputcol is officially no
        // part of the bootstrap grid any more. In exchange, the outputcol now
        // spans all 12 bootstrap columns.
        // Also, remove all the classes used on outputcol for transition so that we get default bootstrap behaviour back.
        // The inputcol just stays the way it is, it is not planned for it to come back
        setTimeout(function() {
            $("#inputcol").removeClass("col-md-4");
            $("#outputcol").removeClass("growing larger1 larger2 col-md-4").addClass("col-md-12");
        }, 2000)
    }

    return {
        "initialize" : initialize,
        "showOutput" : showOutput
    };
})
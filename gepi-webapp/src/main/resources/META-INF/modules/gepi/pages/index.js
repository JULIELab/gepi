define([ "jquery", "bootstrap/tooltip" ], function($) {

	var initialize = function() {
		observelistbchange();
		togglelistb();
		observelistachange();
		setuplistfileselectors();
		setupclearbuttons();

		/*
		 * On changes of list B, checks if the list is empty. If not, some
		 * control elements for single-list operations are disabled.
		 */
		function observelistbchange() {
			$("#listb").on("input change", function() {
				togglelistaoptions();
			});
		}

		/*
		 * On changes of list A, checks if the list is empty. If not, list B is
		 * enabled, otherwise it isn't available for input.
		 */
		function observelistachange() {
			$("#lista").on("input change", function() {
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
				loadfile(fileainput[0], "lista", function() {
					togglelistb();
					// since we load a file on "change", we reset the value
					// after the file has been loaded; otherwise you couldn't
					// load a file, clear the field and load the file again
					fileainput.val("");
				})
			});
			var filebinput = $("#filebinput");
			filebinput.on("change", function() {
				loadfile(filebinput[0], "listb", function() {
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
				$("#lista").val("");
				togglelistb();
			});
			$("#clearb").on("click", function() {
				$("#listb").val("");
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
			var islistbempty = $("#listb").val().length == 0;
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
			var islistaempty = $("#lista").val().length == 0;
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

	return {
		"initialize" : initialize
	};
})
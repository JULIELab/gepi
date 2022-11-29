define(["jquery", "bootstrap5/modal", "bootstrap5/tooltip"], function($, modal, Tooltip) {
	$('#downloadModal').modal();
	const download = function(downloadUrl) {
		$('#downloadButton').on('click', function() {
			$('#downloadModal').modal('toggle');
			console.log("Downloading data from " + downloadUrl)

			// from https://stackoverflow.com/a/23013574/1314955
			var link = document.createElement("a");
			// If you don't know the name or want to use
			// the webserver default set name = ''
			name = ''
			link.setAttribute('download', name);
			link.href = downloadUrl;
			document.body.appendChild(link);
			link.click();
			console.log("Download window popped up")
			link.remove();
		})
	};

	const setupHighlightTooltips = function() {
		const makeTooltips = function() {
			$(".hl-argument").each(function() {
				new Tooltip(this, {
					title: "interaction partner"
				});
			});
			$(".hl-trigger").each(function() {
				new Tooltip(this, {
					title: "interaction indicator word"
				});
			})
			$(".hl-filter").each(function() {
				new Tooltip(this, {
					title: "fulltext filter match"
				});
			})
			$('.hl-like1').each(function() {
				new Tooltip(this, {
					title: "factuality level 'negation'"
				});
			})
			$('.hl-like2').each(function() {
				new Tooltip(this, {
					title: "factuality level 'low'"
				});
			})
			$('.hl-like3').each(function() {
				new Tooltip(this, {
					title: "factuality level 'investigation'"
				});
			})
			$('.hl-like4').each(function() {
				new Tooltip(this, {
					title: "factuality level 'moderate'"
				});
			})
			$('.hl-like5').each(function() {
				new Tooltip(this, {
					title: "factuality level 'high'"
				});
			})
		}

		// Register the tooltip-function with ajaxComplete because
		// after using a paging button, the table gets updated
		// using Ajax and we need to re-enable the tooltips
		// on the new table page.
		$(document).ajaxComplete(function() {
				makeTooltips();
			}

		);
		makeTooltips();
	}


	return {
		download: download,
		setupHighlightTooltips: setupHighlightTooltips
	}
})
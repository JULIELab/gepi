define([ "jquery", "gepi/pages/index" ], function($) {

    return function drawPieChart(barDat) {
		/*
        google.charts.setOnLoadCallback(function() {
        	var maxVal = barDat[0][1]; 
        	var data = new google.visualization.DataTable();
            data.addColumn('string', 'Gene');
            data.addColumn('number', 'Count');
            
            data.addRows(barDat)

            var options = {
            	legend: 'none',
	            viewWindow: {min : 0}
            }
            
            // display one grid line per integer for low counts
            if (maxVal < 10) {	
            	options.hAxis = {
            		gridlines: {
            			count : maxVal+1
	            	}
	            };
            } else {
        		options.hAxis = {
                		gridlines: {
                			count : 10
    	            	}
        		};
            	if (maxVal % 2 > 0) {
            		options.viewWindow.max = maxVal;
            	} else {
            		options.viewWindow.max = maxVal + 1;
            		
            	}            	
            }
                        
            var chart = new google.visualization.BarChart(document.getElementById('barchart'));

            chart.draw(data, options);
            
        })
        */
    }
})
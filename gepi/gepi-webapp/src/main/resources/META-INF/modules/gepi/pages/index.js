define(function(){
    var loadGoogleCharts = function() {
        google.charts.load('45', {'packages':['corechart']});
    };
   
    return {"loadGoogleCharts":loadGoogleCharts};
    
});
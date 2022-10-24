define(['jquery', 'gepi/charts/data', 'gepi/pages/index', 'gepi/components/widgetManager', 'bootstrap5/tooltip', 'bootstrap5/tab', 'bootstrap5/dropdown'], function($, data, index, widgetManager, Tooltip) {
    class PieWidget {
        elementId;
        widgetSettings;
         // id of the element that holds the tabs and the dropdown menu
        tabPieId = 'pie-tabs'
        // id of the input element where the user can specify the numbers of genes to be shown
        numGeneInputId = 'numgeneinput-pie';
        // id of the dropdown menu
        numGenesDropdownItemId = 'numgenesdropdown-pie';
        makeOtherBin = true;

        constructor(elementId, widgetSettings) {
            this.elementId = elementId;
            this.widgetSettings = widgetSettings;
            this.setup();
        }

        setup() {
          index.getReadySemaphor().done(() => {
            data.awaitData('acounts', this.widgetSettings.dataSessionId).done(() => {
                data.awaitData('bcounts', this.widgetSettings.dataSessionId).done(() => {
                  const inputcolReadyPromise = $('#inputcol').data('animationtimer');
                  if (inputcolReadyPromise) {
                    inputcolReadyPromise.done(() => {
                        this.init(this.elementId);
                    });
                  } else {
                    this.init(this.elementId);
                  }
               });
            });
          });
        }

        /*
        * Helper function to obtain the width and height of a plot area
        */
        getDimensionsByElementId(elementId) {
            let element = document.getElementById(elementId);
            let clientRect = element.getBoundingClientRect();
            return [clientRect.height, clientRect.width]
        }

        init() {
            // Remove the Loading... banner
            $('#' + this.elementId + '-outer .text-center.shine').remove();
            // show the control elements: tabs, dropdown menu
            $('#' + this.tabPieId).removeClass('d-none');
            let aCountElId = this.elementId+'-acounts';
            let bCountElId = this.elementId+'-bcounts'

            // The client dimensions of the chart that is currently not shown (because of the tabs for acounts and bcounts)
            // are 0x0. Thus, we here get all the dimensions and use the non-zero ones.
            let [aCountElHeight, aCountElWidth] = this.getDimensionsByElementId(aCountElId);
            let [bCountElHeight, bCountElWidth] = this.getDimensionsByElementId(bCountElId);
            this.height = Math.max(aCountElHeight, bCountElHeight);
            this.width = Math.max(aCountElWidth, bCountElWidth);
           
            this.drawPieChart('acounts', aCountElId);
            this.drawPieChart('bcounts', bCountElId)

            let timeoutId = undefined;
              // Observe changes in the the 'number of genes' input field
            $('#' + this.numGeneInputId).on('input', x => {
                if (timeoutId)
                    clearTimeout(timeoutId);
                timeoutId = setTimeout(
                    () => {
                        this.drawPieChart('acounts', aCountElId);
                        this.drawPieChart('bcounts', bCountElId);
                    }, 500);
            });
            $('#'+this.numGenesDropdownItemId+' li a.number').on('click', e => {
                $('#' + this.numGeneInputId).val(e.currentTarget.text);
                this.drawPieChart('acounts', aCountElId);
                this.drawPieChart('bcounts', bCountElId);
            });

            this.initTooltips();
        }


        drawPieChart(countType, parentElementId) {
            let argCounts = data.getData(countType)['argumentcounts'];
            const sum = argCounts.reduce((accumulator, value) => accumulator + value[1], 0);
            argCounts = argCounts.map(x => {return {label: x[0], value: x[1], percentage: x[1]/sum}})

            // get the number of slices to show from the input field
            let numToShow = $('#' + this.numGeneInputId).val();
            // default to 20
            numToShow = parseInt(numToShow) ? numToShow : 20;
            let otherSum = undefined;
            if (argCounts.length > numToShow && this.makeOtherBin) {
                let tail = argCounts.slice(numToShow);
                otherSum = tail.reduce((accumulator, item) => accumulator + item.value, 0);
            }
            argCounts = argCounts.slice(0, numToShow);
            if (otherSum)
                argCounts.push({label: 'others', value: otherSum, percentage: otherSum/sum});
            
            let width = this.width,
                height = this.height,
                radius = Math.min(width, height) / 2;

            // remove potentially existing SVG element
            d3.select('#'+parentElementId + " svg").remove();

            let svg = d3.select('#'+parentElementId)
                .append('svg')
                .attr('class', 'piechartcanvas')
                .append('g');

            svg.append('g')
                .attr('class', 'slices')
            svg.append('g')
                .attr('class', 'callouts');
            svg.append('g')
                .attr('class', 'labels');
            svg.append('g')
                .attr('class', 'percentages')

            svg.attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");

            let colorScale = d3.scaleOrdinal().domain(argCounts.map(d => d.label)).range(d3.schemeAccent);
            let maxFrequency = d3.max(argCounts.map(d => d.value));

            let sequentialScale = d3.scaleSequential()
                .domain([0, maxFrequency])
                .interpolator(d3.interpolateViridis);
            // the data items are arrays where first element is the gene symbol and the second is the count
            let pieGen = d3.pie()
                .value(d => d.value)
            let arc = d3.arc()
                .innerRadius(radius * 0.2)
                .outerRadius(radius * 0.6);

            let outerArc = d3.arc()
                .innerRadius(radius * 0.8)
                .outerRadius(radius * 0.8);

            let pie = pieGen(argCounts)
            svg.select('.slices')
                .selectAll('path')
                .data(pie)
                .enter()
                .append('path')
                .attr('class', 'slice')
                .attr('opacity', '.7')
                .attr('d', arc)
                .attr('fill', (d,i) => colorScale(i))
                .on('mouseover', function(d,i) {
                    d3.select(this).transition()
                        .duration('400')
                        .attr('opacity', '1');
                })
                .on('mouseout', function(d,i) {
                    d3.select(this).transition()
                        .duration('400')
                        .attr('opacity', '.7');
                });

            function midAngle(d){
                return d.startAngle + (d.endAngle - d.startAngle)/2;
            }

            /* ------- labels ---------- */

           svg.select(".labels")
                .selectAll("text")
                .data(pie)
                .enter()
                .append("text")
                .attr("dy", ".35em")
                .text(function(d) {
                    return d.data.label;
                 })
                .attr("transform", function(d) {
                     let pos = outerArc.centroid(d);
                     pos[0] = radius * (midAngle(d) < Math.PI ? 1 : -1);
                     return "translate("+ pos +")";
                 })
                .style("text-anchor", function(d){
                   return midAngle(d) < Math.PI ? "start":"end";
                 });
    
            /* ------- percentages -------- */

            svg.select('.percentages')
                .selectAll('text')
                .data(pie)
                .enter()
                .append("text")
                .text(function(d){
                    return Math.round(100 * d.data.percentage) + '%';
                })
                .attr('class', 'percentage')
                .attr('transform', function(d) {
                    let pos = arc.centroid(d);
                    return 'translate(' + pos + ')';
                })
                .style('text-anchor', 'middle')
                .style('dominant-baseline', 'middle');

            /* ------- callouts ---------- */

            svg.select(".callouts")
                .selectAll("polyline")
                .data(pie)
                .enter()
                .append("polyline")
                .attr('class', 'callout')
                .attr("points", function(d){
                    let labelPoint = outerArc.centroid(d);
                    labelPoint[0] = radius * 0.95 * (midAngle(d) < Math.PI ? 1 : -1);
                    let slicePoint = arc.centroid(d)
                    slicePoint[0] = slicePoint[0] * 1.3
                    slicePoint[1] = slicePoint[1] * 1.3
                    return [slicePoint, outerArc.centroid(d), labelPoint];
                }); 
        }

        initTooltips() {
            let numGeneInput = $('#' + this.numGeneInputId)[0];
            new Tooltip(numGeneInput, {'trigger':'hover'});

            //let makeOtherBinMenuItem = $('#' + this.numGenesDropdownItemId+ ' li > a.other-bin').parent()[0];
            //new Tooltip(makeOtherBinMenuItem, {'trigger':'hover'});
        }      
    }

    return function newPieWidget(elementId, widgetsettings) {
        widgetManager.addWidget(widgetsettings.widgetId, new PieWidget(elementId, widgetsettings));
  };
});
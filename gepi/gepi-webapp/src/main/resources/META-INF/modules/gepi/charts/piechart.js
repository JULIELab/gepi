define(['jquery', 'gepi/charts/data', 'gepi/pages/index', 'gepi/components/widgetManager'], function($, data, index, widgetManager) {
    class PieWidget {
        elementId
        widgetSettings

        constructor(elementId, widgetSettings) {
            this.elementId = elementId;
            this.widgetSettings = widgetSettings;
            console.log("Creating pie widget with settings " + widgetSettings)
            this.setup();
        }

        setup() {
          index.getReadySemaphor().done(() => {
            data.awaitData('acounts', this.widgetSettings.dataSessionId).done(() => {
              const inputcolReadyPromise = $('#inputcol').data('animationtimer');
              if (inputcolReadyPromise) {
                inputcolReadyPromise.done(() => {
                    this.init(this.elementId, this.orderType);
                });
              } else {
                this.init(this.elementId, this.orderType);
              }
            });
          });
        }

        init() {
            // Remove the Loading... banner
            $('#' + this.elementId + '-outer .text-center.shine').remove();
            
            const argCounts = data.getData('acounts')['argumentcounts'];
            let argMap = {}
            for (let i = 0; i < argCounts.length; i++)
                argMap[argCounts[i][0]] = argCounts[i][1]

            let svg = d3.select('#'+this.elementId)
                .append('svg')
                .attr('width', 300)
                .attr('height', 300);
            
            let colorScale = d3.scaleOrdinal().domain(argCounts.map(i => i[0])).range(d3.schemeSet2);
            let maxFrequency = d3.max(argCounts.map(x => x[1]));
            
            let sequentialScale = d3.scaleSequential()
                .domain([0, maxFrequency])
                .interpolator(d3.interpolateViridis);
            // the data items are arrays where first element is the gene symbol and the second is the count
            let pieGen = d3.pie()
                .value(d => d[1])
            let arc = d3.arc()
                .innerRadius(0)
                .outerRadius(100);

            let pie = pieGen(argCounts)
            console.log(pie)
            let g = svg.append('g')
                .attr('transform', 'translate(150,150)');
            g.selectAll('path')
                .data(pie)
                .enter()
                .append('path')
                .attr('d', arc)
                .attr('fill', d => colorScale(d.value))
            g.selectAll('path')
                .append('title')
                .text(d => d.data[0])
            g.selectAll('text')
                .data(pie)
                .enter()
                .filter(d => (d.endAngle - d.startAngle)*100 > 30)
                .append('text')
                .text(d => d.data[0])
                .attr('transform', d => 'translate('+arc.centroid(d)+')')
                .style('text-anchor', 'middle')
        }
    }

    return function newPieWidget(elementId, widgetsettings) {
        widgetManager.addWidget(widgetsettings.widgetId, new PieWidget(elementId, widgetsettings));
  };
});
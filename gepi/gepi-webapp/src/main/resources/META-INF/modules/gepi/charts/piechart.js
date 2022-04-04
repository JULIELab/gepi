define(['jquery', 'gepi/charts/data', 'gepi/pages/index', 'gepi/components/widgetManager'], function($, data, index, widgetManager) {
    class PieWidget {
        elementId
        widgetSettings

        constructor(elementId, widgetSettings) {
            this.elementId = elementId;
            this.widgetSettings = widgetSettings;
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
            const argCounts = data.getData('acounts');

            let svg = d3.select('#'+this.elementId)
                .append('svg')
                .attr('width', 300)
                .attr('height', 300);

            let colorScale = d3.scaleOrdinal(["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"]);
            // the data items are arrays where first element is the gene symbol and the second is the count
            let pie = d3.pie()
                .value(d => d[1])
            let arc = d3.arc()
                .outerRadius(200);

            svg.append('g')
                .selectAll('path')
                .enter(pie(argCounts))
                .attr('d', arc)
                .attr('fill', d => colorScale(d[0]))
        }
    }
}
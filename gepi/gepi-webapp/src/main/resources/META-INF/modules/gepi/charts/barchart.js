define(['jquery', 'gepi/charts/data', 'gepi/pages/index', 'gepi/components/widgetManager', 'bootstrap5/tooltip', 'bootstrap5/tab'], function($, data, index, widgetManager, Tooltip) {
    class PieWidget {
        elementId;
        widgetSettings;
        // id of the element that holds the tabs and the dropdown menu
        tabBarId = 'bar-tabs'
        // id of the input element where the user can specify the numbers of genes to be shown
        numGeneInputId = 'numgeneinput-bar';
        // id of the dropdown menu
        numGenesDropdownItemId = 'numgenesdropdown-bar';
        makeOtherBin = true;
        displayPercentages = false;

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
            $('#' + this.tabBarId).removeClass('d-none');

            this.redraw();

            const update = () => {
                this.redraw();
                this.toggleMenuTicks();
            }

            let timeoutId = undefined;
            // Observe changes in the the 'number of genes' input field
            $('#' + this.numGeneInputId).on('input', x => {
                if (timeoutId)
                    clearTimeout(timeoutId);
                timeoutId = setTimeout(
                    update, 500);
            });
            $('#' + this.numGenesDropdownItemId + ' li a.number').on('click', e => {
                $('#' + this.numGeneInputId).val(e.currentTarget.text);
                update();
            });
            $('#' + this.numGenesDropdownItemId + ' li a.other-bin').on('click', e => {
                this.makeOtherBin = !this.makeOtherBin;
                update();
            });
            $('#' + this.numGenesDropdownItemId + ' li a.percentages').on('click', e => {
                this.displayPercentages = !this.displayPercentages;
                update();
            });
        }

        redraw() {
            let aCountElId = this.elementId + '-acounts';
            let bCountElId = this.elementId + '-bcounts'

            // The client dimensions of the chart that is currently not shown (because of the tabs for acounts and bcounts)
            // are 0x0. Thus, we here get all the dimensions and use the non-zero ones.
            let [aCountElHeight, aCountElWidth] = this.getDimensionsByElementId(aCountElId);
            let [bCountElHeight, bCountElWidth] = this.getDimensionsByElementId(bCountElId);
            this.height = Math.max(aCountElHeight, bCountElHeight);
            this.width = Math.max(aCountElWidth, bCountElWidth);

            this.drawBarChart('acounts', aCountElId);
            this.drawBarChart('bcounts', bCountElId)
        }

        /*
         * Toggles the 'd-none' class of the given element depending on whether 'state' is true or false.
         */
        toggleDisplayNone(elementId, state) {
            const element = $('#' + elementId);
            if (state)
                element.removeClass('d-none')
            else if (!element.hasClass('d-none'))
                element.addClass('d-none')
        }

        toggleMenuTicks() {
            // Toggle the 'enabled/disabled' tick for the creation of the 'other' bar in the dropdown menu
            this.toggleDisplayNone(this.numGenesDropdownItemId + ' li a.other-bin span.tick', this.makeOtherBin);
            // Toggle the 'enabled/disabled' tick for percentages in the dropdown menu
            this.toggleDisplayNone(this.numGenesDropdownItemId + ' li a.percentages span.tick', this.displayPercentages);
        }

        drawBarChart(countType, parentElementId) {
            let argCounts = data.getData(countType)['argumentcounts'];
             if (argCounts.length === 0) {
                $('#'+parentElementId).append('<div class="alert alert-info mx-auto">There is not data to display.</div>');
                return;
            }
            const sum = argCounts.reduce((accumulator, value) => accumulator + value[1], 0);
            argCounts = argCounts.map(x => {
                return {
                    label: x[0],
                    value: x[1],
                    percentage: x[1] / sum
                }
            })

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
                argCounts.push({
                    label: 'others',
                    value: otherSum,
                    percentage: otherSum / sum
                });
            const valueSupplier = item => this.displayPercentages ? item.percentage : item.value;
            let maxDisplayedValue = argCounts.reduce((accumulator, item) => Math.max(accumulator, valueSupplier(item)), 0);

            // remove potentially existing SVG element
            d3.select('#' + parentElementId + " svg").remove();

            // set the dimensions and margins of the graph
            const margin = {
                    top: 30,
                    right: 30,
                    bottom: 70,
                    left: 60
                },
                width = this.width - margin.left - margin.right,
                height = this.height - margin.top - margin.bottom;

            if (width < 0)
                throw `Width is negative: ${width}`
            if (height < 0)
                throw `Height is negative: ${height}`

            // append the svg object to the body of the page
            const svg = d3.select('#' + parentElementId)
                .append("svg")
                .attr("class", "barchartcanvas")
                .attr("width", width + margin.left + margin.right)
                .attr("height", height + margin.top + margin.bottom)
                .append("g")
                .attr("transform", `translate(${margin.left},${margin.top})`);



            // X axis
            const x = d3.scaleBand()
                .range([0, width])
                .domain(argCounts.map(d => d.label))
                .padding(0.2);
            svg.append("g")
                .attr("transform", `translate(0, ${height})`)
                .call(d3.axisBottom(x))
                .selectAll("text")
                .attr("transform", "translate(-10,0)rotate(-45)")
                .style("text-anchor", "end");

            // Add Y axis
            const y = d3.scaleLinear()
                .domain([0, maxDisplayedValue])
                .range([height, 0]);
            if (!this.displayPercentages) {
                // hide fraction number label when we only have whole number values
                const yAxisTicks = y.ticks()
                    .filter(tick => Number.isInteger(tick));
                svg.append("g")
                    .call(d3.axisLeft(y)
                        .tickValues(yAxisTicks)
                        .tickFormat(d3.format("d")));
            } else {
                svg.append("g")
                    .call(d3.axisLeft(y))
            }
            // text label for the y axis
            svg.append("text")
                .attr("transform", "rotate(-90)")
                .attr("y", 0 - margin.left)
                .attr("x",0 - (height / 2))
                .attr("dy", "1em")
                .style("text-anchor", "middle")
                .text(this.displayPercentages ? "percent" : "frequency");      

            // Bars
            svg.selectAll("mybar")
                .data(argCounts)
                .join("rect")
                .attr("x", d => x(d.label))
                .attr("y", d => y(valueSupplier(d)))
                .attr("width", x.bandwidth())
                .attr("height", d => height - y(valueSupplier(d)))
                .attr("class", "bar")
                .attr('data-bs-toggle', 'default-tooltip')
                .attr('title', d => d.label + "<br />count: " + d.value + "<br />fraction: " + (Math.round(100 * d.percentage) + '%'))
                .attr("fill", "#69b3a2")
                .on('mouseover', function(d,i) {
                    d3.select(this).transition()
                        .duration('400')
                        .attr('fill', '#518c7f');
                })
                .on('mouseout', function(d,i) {
                    d3.select(this).transition()
                        .duration('400')
                        .attr('fill', '#69b3a2');
                });

                this.initTooltips();
        }

        initTooltips() {
            const tooltipTriggerList = [].slice.call(document.querySelectorAll(
                '#' + this.numGeneInputId + ',' +
                '#' + this.numGenesDropdownItemId + ' li[data-bs-toggle="tooltip"]'));
            tooltipTriggerList.forEach(i => new Tooltip(i, {
                'trigger': 'hover'
            }));

            $('#' + this.elementId + '-outer svg .bar').each(function() {
                 new Tooltip(this, {html:true})
             });
        }
    }

    return function newPieWidget(elementId, widgetsettings) {
        widgetManager.addWidget(widgetsettings.widgetId, new PieWidget(elementId, widgetsettings));
    };
});
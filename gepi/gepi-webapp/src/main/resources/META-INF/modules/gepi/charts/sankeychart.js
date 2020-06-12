define(["jquery", "gepi/charts/data", "gepi/pages/index", "gepi/components/widgetManager"], function($, data, index, widgetManager) {

    class SankeyWidget {
        elementId
        orderType

        constructor(elementId, orderType, widgetsettings) {
            this.elementId = elementId;
            this.orderType = orderType;
            // hier eigentlich gar nicht gebraucht, vermutlich zu entfernen
            this.widgetsettings = widgetsettings;

            this.setup();
        }

        setup() {
            console.log("Preparing to draw sankey chart for element ID " + this.elementId + " with node ordering type " + this.orderType);

            index.getReadySemaphor().done(() => {
                console.log("Chart drawing has green light from the central index semaphor, requesting data");
                data.awaitData("relationCounts").done(() => {
                    console.log("Loading data was successful. Checking if the input column also gives green light.");
                    let inputcolReadyPromise = $("#inputcol").data("animationtimer");
                    if (inputcolReadyPromise)
                        inputcolReadyPromise.done(() =>
                            this.init(this.elementId, this.orderType));
                    else
                        this.init(this.elementId, this.orderType);
                });
            });
        }

        init() {
            console.log("Initializing sankey chart");
            let sankeyDat = data.getData("relationCounts");
            console.log(sankeyDat);
            this.preprocessed_data = data.preprocess_data(sankeyDat, this.orderType);

            this.settings = {
                width: 500,
                height: 300,
                min_height: 200,
                padding_x: 100,
                padding_y: 10,
                node_spacing: 7,
                min_node_height: 5,
                label_font_size: 12,
                node_width: 10,
                node_to_label_spacing: 5,
                //max_number_nodes: 3,
                show_other: false,
                restrict_other_height: true,
                max_other_height: 100,
            };


            let running = false;
            window.onresize = () => {
                if (!running) {
                    running = true;
                    this.redraw();
                    running = false;
                }
            };


            this.selected_by_node_id = {};



            this.main();


        }

        main() {
            console.log("Call to main")
            if (!$("#" + this.elementId).data("mainWasCalled")) {
                let settings = this.settings;
                // Hide the Loading... banner
                $("#" + this.elementId + "-outer .panel-body .shine").addClass("hidden");

                this.redraw();

                this.add_slider("padding-slider", "Padding: ", 0, 50, 2, settings.node_spacing, (value) => settings.node_spacing = Number(value));
                this.add_slider("min-size-slider", "Minimum node size: ", 0, 150, 2, settings.min_node_height, (value) => settings.min_node_height = value);
                this.add_slider("node-height-slider", "Chart height: ", 0, 1000, 2, settings.height, (value) => settings.height = value - 0);
                //add_slider("node-number-slider", "Max number of nodes: ", 0, 300, 2, this.settings.max_number_nodes, (value) => this.settings.max_number_nodes = value);
                this.add_slider("max-other-slider", "Maximum size of \"Other\" node:", 0, 300, 2, settings.max_other_height, (value) => settings.max_other_height = value);

                this.add_toggle(
                    "restrict-other-toggle",
                    "Restrict size of \"Other\" node",
                    settings.restrict_other_height,
                    (state) => settings.restrict_other_height = state
                );

                this.add_toggle(
                    "show-other-toggle",
                    "Show \"Other\" node",
                    settings.show_other,
                    (state) => settings.show_other = state
                );

                this.add_button("Clear selection", () => {
                    this.selected_by_node_id = {};
                    this.redraw();
                });
                $("#" + this.elementId).data("mainWasCalled", true);
                console.log("Finished main.")
            } else {
                console.log("Not executing sankeychart#main() again because it had already been called");
            }
        }

        create_svg() {
            let chart_elem = document.getElementById(this.elementId);
            let chart = d3.select(chart_elem);

            chart.selectAll("svg").remove();

            let chartContainer = $("#" + this.elementId + "-container");
            //chartContainer.closest(".panel-body > .shine").addClass("hidden");
            chartContainer.removeClass("hidden");
            this.settings.width = chart_elem.clientWidth - 2 * this.settings.padding_x - 10;
            let svg = chart
                .append("svg")
                .attr("width", this.settings.width + 2 * this.settings.padding_x)
                .attr("height", this.settings.height + 2 * this.settings.padding_y);

            return svg.append("g").attr("transform", "translate(" + this.settings.padding_x + "," + this.settings.padding_y + ")");
        }



        redraw() {
            console.log("Redrawing sankey!");

            let svg = this.create_svg();

            console.log("Preparing sankey data");
            let max_other_height;
            if (this.settings.restrict_other_height) {
                max_other_height = Number(this.settings.max_other_height);
            } else {
                max_other_height = Infinity;
            }
            let the_data = data.prepare_data(this.preprocessed_data, this.settings.height, this.settings.min_node_height, this.settings.node_spacing, this.settings.show_other, max_other_height);
            console.log("Finished preparing data");

            let sankey = d3.sankey();

            sankey
                .size([this.settings.width, this.settings.height])
                .nodeWidth(this.settings.node_width)
                .nodePadding(this.settings.node_spacing)
                .nodeId((d) => d.id)
                .nodes(the_data.nodes)
                .links(the_data.links)
                .iterations(0);

            console.log("Computing sankey layout...")
            sankey();
            console.log("Done")

            //shift(the_data.nodes[4], 50, 20);

            sankey.update(the_data);

            this.adapt_node_widths(the_data, max_other_height);

            //links
            let links = svg.append("g")
                .attr("fill", "none")
                .attr("stroke", "#000")
                .attr("fill-opacity", "0.2")
                .selectAll("path.link")
                .data(the_data.links)
                .enter().append("path")
                .attr("class", "link")
                .attr("fill", (d) => d.color)
                //.attr("stroke", (d) => d.color)
                .attr("d", this.compute_path)
                .attr("stroke-width", 0);
            //.attr("stroke-width", (d) => d.width);

            links.append("title")
                .text(link => [link.source.id, link.target.id, link.color].join());

            // nodes
            let nodes = svg.append("g")
                .selectAll(".node")
                .data(the_data.nodes)
                .enter().append("g")
                .attr("class", "node")
                .attr("transform", (d) => "translate(" + d.x0 + "," + d.y0 + ")");

            // nodes: rects
            nodes.append("rect")
                .attr("height", (d) => d.y1 - d.y0)
                .attr("width", (d) => d.x1 - d.x0)
                .attr("opacity", (d) => {
                    if (d.id === "MISC_from" || d.id === "MISC_to") {
                        return 0.4;
                    } else {
                        return 1;
                    }
                })
                .attr("fill", (d) => {

                    /* COLOR SCHEME FOR NODES
                    normal - black
                    misc/hidden - gray (semi-transparent)
                    selected - blue
                    pinned - ? maybe a pattern? red checkerboard?

                     */

                    if (this.selected_by_node_id[d.id]) {
                        return "#0040a0";
                    } else {
                        return "#000000";
                    }
                });

            nodes.on("click", (d) => {
                this.selected_by_node_id[d.id] = !this.selected_by_node_id[d.id];
                this.redraw();
            });

            let settings = this.settings
            // nodes: labels
            nodes.append("text")
                .text((d) => d.name)
                .style("font-size", this.settings.label_font_size + "px")
                .attr("y", (d) => (d.y1 - d.y0 + this.settings.label_font_size) / 2)
                .attr("x", function(d) {
                    if (d.id.endsWith("_from")) {
                        return -this.getComputedTextLength() - settings.node_to_label_spacing;
                    } else {
                        return settings.node_width + settings.node_to_label_spacing;
                    }
                });

            nodes.append("title")
                .text((d) => d.name);

        }

        add_toggle(id, text, initial_state, change_handler) {
            let p = d3.select("#" + this.elementId + "-container .settings .checkboxes").append("p");
            console.log('[add_toggle] node: ' + p.node());
            let input = p.append("input").attr("type", "checkbox").attr("id", id);
            if (initial_state) {
                input.attr("checked", "checked");
            }
            p.append("label").attr("for", id).text(" " + text);
            input.on("change", function() {
                change_handler(this.checked);
                this.redraw();
            });
        }

        add_slider(id, label_text, min, max, step, value, change_handler) {
            let p = d3.select("#" + this.elementId + "-container .settings").select(".sliders").append("p");
            let redraw = this.redraw.bind(this)


            p.append("label")
                .attr("for", id)
                .text(label_text);

            p.append("input")
                .attr("type", "range")
                .attr("id", id)
                .attr("min", min)
                .attr("max", max)
                .attr("step", step)
                .attr("value", value)
                .on("input", function() {
                    let value = this.value;
                    change_handler(value);
                    redraw();
                });
        }

        add_button(text, click_handler) {
            d3.select(this.elementId).select(".buttons")
                .append("button")
                .text(text)
                .on("click", click_handler);
        }

        shift(node, x, y) {
            node.x0 += x;
            node.x1 += x;
            node.y0 += y;
            node.y1 += y;
        }

        compute_path(d) {
            let x_left = d.source.x1;
            let x_right = d.target.x0;
            let x_center = (x_left + x_right) / 2;
            let left_width = d.left_width;
            let right_width = d.right_width;

            /* Thick, stroked path
            let path =
                "M " +
                x_left + ", " +
                d.y0 +
                " C " +
                x_center + ", " +
                d.y0 + ", " +
                x_center + ", " +
                d.y1 + ", " +
                x_right + ", " +
                d.y1;
            */
            // filled path
            let path =
                "M " +
                x_left + ", " +
                (d.y0 - left_width / 2) +
                " L " +
                x_left + ", " +
                (d.y0 + left_width / 2) +
                " C " +
                x_center + ", " +
                (d.y0 + left_width / 2) + ", " +
                x_center + ", " +
                (d.y1 + right_width / 2) + ", " +
                x_right + ", " +
                (d.y1 + right_width / 2) +
                " L " +
                x_right + ", " +
                (d.y1 - right_width / 2) +
                " C " +
                x_center + ", " +
                (d.y1 - right_width / 2) + ", " +
                x_center + ", " +
                (d.y0 - left_width / 2) + ", " +
                x_left + ", " +
                (d.y0 - left_width / 2);

            return path;
        }

        adapt_node_widths(data, max_other_height) {
            let left_other_y0 = 0;
            let left_scale = 1;
            let right_other_y0 = 0;
            let right_scale = 1;

            for (let node of data.nodes) {
                if (node.id === "MISC_from") {
                    left_other_y0 = node.y0;
                    if (node.y1 - node.y0 > max_other_height) {
                        left_scale = max_other_height / (node.y1 - node.y0);
                        node.y1 = node.y0 + max_other_height;
                    }
                }
                if (node.id === "MISC_to") {
                    right_other_y0 = node.y0;
                    if (node.y1 - node.y0 > max_other_height) {
                        right_scale = max_other_height / (node.y1 - node.y0);
                        node.y1 = node.y0 + max_other_height;
                    }
                }
            }

            for (let link of data.links) {
                if (link.source.id === "MISC_from") {
                    link.left_width = left_scale * link.width;
                    link.y0 = left_other_y0 + left_scale * (link.y0 - left_other_y0);
                } else {
                    link.left_width = link.width;
                }
                if (link.target.id === "MISC_to") {
                    link.right_width = right_scale * link.width;
                    link.y1 = right_other_y0 + right_scale * (link.y1 - right_other_y0);
                } else {
                    link.right_width = link.width;
                }
            }
        }
    }

    return function newSankeyWidget(elementId, orderType, widgetsettings) {
        widgetManager.addWidget(widgetsettings.widgetId, new SankeyWidget(elementId, orderType, widgetsettings));
    };



});
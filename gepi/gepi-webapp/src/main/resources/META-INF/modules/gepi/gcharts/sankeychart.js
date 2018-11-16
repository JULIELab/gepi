define([ "jquery", "gepi/gcharts/sankey/data" ], function($, data) {

    return function drawSankeyChart(elementId, sankeyDat) {
                let promise = $("#inputcol").data("animationtimer");
                if (promise)
                     promise.then(() =>
                        draw(elementId, sankeyDat));
                else
                   draw(elementId, sankeyDat);
    };

    function draw(elementId, sankeyDat) {
        console.log("sankey-data:");
        console.log(sankeyDat);

        let preprocessed_data = data.preprocess_data(sankeyDat, "commonPartnersHarmonicMean");


        let settings = {
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
            max_number_nodes: 3
        };

        let chart_elem = document.getElementById(elementId);

        let running = false;
        window.onresize = () => {
            if (!running) {
                running = true;
                redraw();
                running = false;
            }
        };

        function create_svg() {
            settings.width = chart_elem.clientWidth - 2 * settings.padding_x - 10;
            /*settings.height = chart_elem.clientHeight - 2 * settings.padding_y - 10;
            if (settings.height < settings.min_height) {
                settings.height = settings.min_height;
            }*/

            let chart = d3.select(chart_elem);

            chart.selectAll("svg").remove();

            let svg = chart
                .append("svg")
                .attr("width", settings.width + 2 * settings.padding_x)
                .attr("height", settings.height + 2 * settings.padding_y);

            return svg.append("g").attr("transform", "translate("+settings.padding_x+","+settings.padding_y+")");
        }

        let selected_by_node_id = {};

        function main() {
            redraw();

            add_slider("padding-slider", "Padding: ", 0, 50, 2, settings.node_spacing, (value) => settings.node_spacing = value);
            add_slider("min-size-slider", "Minimum node size: ", 0, 150, 2, settings.min_node_height, (value) => settings.min_node_height = value);
            add_slider("node-height-slider", "Chart height: ", 0, 10000, 2, settings.height, (value) => settings.height = value - 0);
            add_slider("node-number-slider", "Max number of nodes: ", 0, 300, 2, settings.max_number_nodes, (value) => settings.max_number_nodes = value);

            add_button("Clear selection", () => {
                selected_by_node_id = {};
                redraw();
            });
        }

        main();

        function redraw() {
            console.log("Redrawing sankey!");

            let svg = create_svg();

            console.log("Preparing sankey data");
            let the_data = data.prepare_data(preprocessed_data, settings.height, settings.min_node_height, settings.node_spacing, settings.max_number_nodes);
            console.log("Finished preparing data");

            let sankey = d3.sankey();

            sankey
                .size([settings.width, settings.height])
                .nodeWidth(settings.node_width)
                .nodePadding(settings.node_spacing)
                .nodeId((d) => d.id)
                .nodes(the_data.nodes)
                .links(the_data.links)
                .iterations(0);

            console.log("Computing sankey layout...")
            sankey();
            console.log("Done")

            //shift(the_data.nodes[4], 50, 20);

            sankey.update(the_data);

            console.log("Creating sankey links")
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
                .attr("d", compute_path)
                .attr("stroke-width", 0);
            //.attr("stroke-width", (d) => d.width);

            links.append("title")
                .text(link => [link.source.id, link.target.id, link.color].join());

            console.log("Creating sankey nodes")
            // nodes
            let nodes = svg.append("g")
                .selectAll(".node")
                .data(the_data.nodes)
                .enter().append("g")
                .attr("class", "node")
                .attr("transform", (d) => "translate("+d.x0+","+d.y0+")");

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

                    if (selected_by_node_id[d.id]) {
                        return "#0040a0";
                    } else {
                        return "#000000";
                    }
                });

            nodes.on("click", (d) => {
                selected_by_node_id[d.id] = !selected_by_node_id[d.id];
                redraw();
            });

            // nodes: labels
            nodes.append("text")
                .text((d) => d.name)
                .style("font-size", settings.label_font_size + "px")
                .attr("y", (d) => (d.y1 - d.y0 + settings.label_font_size) / 2)
                .attr("x", function (d) {
                    if (d.id.endsWith("_from")) {
                        return -this.getComputedTextLength() - settings.node_to_label_spacing;
                    } else {
                        return settings.node_width + settings.node_to_label_spacing;
                    }
                });

            nodes.append("title")
                .text((d) => d.name);

        }

        function add_slider(id, label_text, min, max, step, value, change_handler) {
            let p = d3.select("#"+elementId +"-container .settings").select(".sliders").append("p");

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
                .on("input", function () {
                    let value = this.value;
                    change_handler(value);
                    redraw();
                });
        }

        function add_button(text, click_handler) {
            d3.select(elementId).select(".buttons")
                .append("button")
                .text(text)
                .on("click", click_handler);
        }

        function shift(node, x, y) {
            node.x0 += x;
            node.x1 += x;
            node.y0 += y;
            node.y1 += y;
        }

        function compute_path(d) {
            let x_left = d.source.x1;
            let x_right = d.target.x0;
            let x_center = (x_left + x_right) / 2;
            let width = d.width;

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
                (d.y0 - width/2) +
                " L " +
                x_left + ", " +
                (d.y0 + width/2) +
                " C " +
                x_center + ", " +
                (d.y0 + width/2) + ", " +
                x_center + ", " +
                (d.y1 + width/2) + ", " +
                x_right + ", " +
                (d.y1 + width/2) +
                " L " +
                x_right + ", " +
                (d.y1 - width/2) +
                " C " +
                x_center + ", " +
                (d.y1 - width/2) + ", " +
                x_center + ", " +
                (d.y0 - width/2) + ", " +
                x_left + ", " +
                (d.y0 - width/2);

            return path;
        }
    };
});

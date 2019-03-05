define(["jquery", "gepi/pages/index", "gepi/charts/data", "gepi/components/widgetManager"], function($, index, data, widgetManager) {

    let settings = {
        radius: 150,
        min_radius: 120,
        node_count: 75,
        padding: 90,
        node_spacing: 10,
        node_thickness: 10,
        default_grey: false,
        fine_node_highlights: true,
        active_link_color: "#000055",
        inactive_link_color: "#aaa",
        active_node_opacity: 1,
        inactive_node_opacity: 0.15,
    };

    function prepare_data(links) {
        // input: links der form (sourceId, targetId, frequency)
        //let {raw_nodes, links} = data();
        let node_indices = {};

        let total_weight = 0;
        let node_weights = {};
        let node_weight_target = {};
        let nodes = [];
        let raw_nodes = [];
        const node_count = settings.node_count;

        for (let {
                source,
                target,
                frequency
            } of links) {
            total_weight += frequency;

            function add_to_node(node, w) {
                if (node_weights[node] === undefined) {
                    node_weights[node] = 0;
                    let index = raw_nodes.length;
                    raw_nodes.push(node);
                    node_weight_target[node] = 0;
                }
                node_weights[node] += w;
            }
            add_to_node(source, frequency);
            add_to_node(target, frequency);
            node_weight_target[target] += frequency;
        }
        // jetzt haben wir node_weights der form nodeId -> frequencySum
        // raw_nodes: [nodeId]

        let sorted_nodes_and_weights = Array.from(Object.entries(node_weights)).sort(([n1, w1], [n2, w2]) => w2 - w1).slice(0, node_count);
        let included_nodes = {};

        for ([n, w] of sorted_nodes_and_weights) {
            included_nodes[n] = true;
        }
        // now we have included_nodes: nodeId -> true
        // of 100 most frequent nodes

        console.log("Included nodes: ", included_nodes);

        links = links.filter(({
            source,
            target
        }) => {
            return included_nodes[source] && included_nodes[target];
        });
        // die links sind jetzt nur noch diejenigen zwischen den included nodes

        // distribute the 100 nodes evenly across a circle
        const node_distance = 360 / node_count;

        // raw_nodes: [nodeId] (i.e. index below is just the array index)
        // node_weights: nodeId -> frequencySum
        // nodes is empty until now
        let psi = (Math.sqrt(5) - 1) / 2;
        let next_index = 0;
        let offset = Math.round(psi * node_count);
        for (let [id, weight] of sorted_nodes_and_weights) {
                next_index = (next_index + offset) % node_count;
                while (nodes[next_index % node_count]) {
                    next_index += 1;
                    if (next_index > 2*node_count) {
                        console.log("Critical error!");
                        break;
                    }
                }
                next_index %= node_count;

                let weight_target = node_weight_target[id];
                let weight_ratio = weight_target / weight;

                nodes[next_index] = {
                    id,
                    pos: next_index * node_distance,
                    weight,
                    weight_ratio,
                };
                node_indices[id] = next_index;
        }
        // now we have
        // nodes: [{nodeId, pos, weight, weight_ratio}]
        // node_indices = nodeId -> index in nodes

        let compute_link_offset = at => node_indices[at] * node_distance;
        for (let link of links) {
            link.start_pos = compute_link_offset(link.source);
            link.end_pos = compute_link_offset(link.target);
        }
        // the links now have (sourceId, targetId, frequency, start_pos, end_pos)
        // thus, everything required to draw them

        console.log(node_weights);
        console.log(links);

        // links: [(sourceId, targetId, frequency, start_pos, end_pos)]
        // raw_nodes: [nodeId]
        // nodes: [{nodeId, pos, weight, weight_ratio}]
        return {
            links,
            raw_nodes,
            nodes,
        };
    }

    function get_svg(elementId) {
        let element = document.getElementById(elementId);
        let parent = element.parentElement;
        settings.radius = Math.min(
            parent.clientWidth / 2,
            parent.parentElement.parentElement.clientHeight / 2 - 30
        ) - settings.padding;

        settings.radius = Math.max(settings.radius, settings.min_radius);

        settings.node_count = Math.floor(settings.radius / 2);

        let chart = d3.select(element);

        chart.selectAll("svg").remove();

        let svg = chart
            .append("svg")
            .style("margin-left", "auto")
            .style("margin-right", "auto")
            .attr("width", 2 * settings.radius + 2 * settings.padding)
            .attr("height", 2 * settings.radius + 2 * settings.padding);

        let offset = settings.padding + settings.radius;

        return svg.append("g").attr("transform", "translate(" + offset + "," + offset + ")");

    }

    let links;
    let nodes;
    let hovered_id = "";

    function draw(elementId) {
        let svg = get_svg(elementId);

        //let data = prepare_data(raw_data);

        let chartData = data.getData("relationCounts");
        let nodesById = new Map();
        chartData.nodes.forEach(n => nodesById.set(n.id, n));
        console.log("Relation counts for circle chart:");
        console.log(chartData);
        chartData = prepare_data(chartData.links);

        console.log("Circle chart preprocessed data:");
        console.log(chartData);


        nodes = svg.append("g")
            .attr("class", "nodes")
            .selectAll("g.node")
            .data(chartData.nodes)
            .enter().append("g")
            .attr("class", "node")
            .attr("opacity", hoverless_node_opacity())
            .attr("transform", function(d) {
                return "rotate(" + (d.pos - 90) + ")";
            });

        let node_texts = nodes.append("text")
            .attr("class", "nodeText")
            .attr("x", settings.radius + 10)
            .attr("y", 4)
            .text(d => nodesById.get(d.id).name)
            .property("onmouseover", () => node_hover)
            .property("onmouseout", () => node_unhover)
            .attr("fill", (d) => {
                let red = Math.round(d.weight_ratio * 100);
                let green = Math.round((1 - d.weight_ratio) * 100);

                return "rgb("+red+","+green+",0)";
            });

        node_texts.filter(d => ((d.pos % 360) + 360) % 360 > 180)
            .attr("transform", "rotate(180)")
            .attr("x", -settings.radius - 10)
            .attr("text-anchor", "end");


        // hack: global
        if (window.opacity_base === undefined) {
            window.opacity_base = 0.99;
        }

        links = svg.append("g")
            .attr("class", "links")
            .selectAll("path.link")
            .data(chartData.links)
            .enter().append("path")
            .attr("class", "link")
            .attr("fill", "none")
            .attr("opacity", d => (1 - Math.pow(window.opacity_base, d.frequency)))
            .attr("stroke", hoverless_link_color())
            .attr("d", compute_link_path)
            .attr("stroke-width", 5);

        opacity_redraw = () => draw(elementId);
    }

    function node_hover(event) {
        hovered_id = event.target.__data__.id;

        let connected_nodes = {};
        connected_nodes[hovered_id] = 10000000;

        links.attr("stroke", settings.inactive_link_color);

        links.filter(link => {
            if (link.source === hovered_id) {
                connected_nodes[link.target] = link.frequency;
                return true;
            } else if (link.target === hovered_id) {
                connected_nodes[link.source] = link.frequency;
                return true;
            } else {
                return false;
            }
        }).attr("stroke", settings.active_link_color).raise();

        nodes.attr("opacity", n => {
            if (settings.fine_node_highlights) {
                let v1 = 1 - Math.pow(0.97, connected_nodes[n.id] || 0);
                return (settings.active_node_opacity - settings.inactive_node_opacity) * v1
                    + settings.inactive_node_opacity;
            } else {
                if (connected_nodes[n.id]) {
                    return settings.active_node_opacity;
                } else {
                    return settings.inactive_node_opacity;
                }
            }
        });
    }

    function node_unhover(event) {
        let unhovered_id = event.target.__data__.id;

        if (unhovered_id === hovered_id) {
            hovered_id = "";
            links.attr("stroke", hoverless_link_color());
            nodes.attr("opacity", hoverless_node_opacity());
        }
    }

    function hoverless_link_color() {
        if (settings.default_grey) {
            return settings.inactive_link_color;
        } else {
            return settings.active_link_color;
        }
    }

    function hoverless_node_opacity() {
        if (settings.default_grey) {
            return settings.inactive_node_opacity;
        } else {
            return settings.active_node_opacity;
        }
    }

    function deg_to_coord(deg) {
        let r = settings.radius;
        let rad = deg / 180 * Math.PI;
        return (r * Math.sin(rad)) + " " + (-1 * r * Math.cos(rad));
    }

    function compute_link_path(link) {
        let {
            start_pos,
            end_pos
        } = link;
        let path = "M " + deg_to_coord(start_pos) +
            " Q 0 0 " + deg_to_coord(end_pos);

        return path;
    }

    function add_toggle(elementId, id, text, initial_state, change_handler) {
        let p = d3.select("#"+elementId+"-container .settings .checkboxes").append("p");
        console.log(p.node());
        let input = p.append("input").attr("type", "checkbox").attr("id", id);
        if (initial_state) {
            input.attr("checked", "checked");
        }
        p.append("label").attr("for", id).text(" "+text);
        input.on("change", function () {
            change_handler(this.checked);
            draw(elementId);
        });
    }

    function add_slider(elementId, id, label_text, min, max, step, value, change_handler) {
        let p = d3.select("#" + elementId + "-container .settings .sliders").append("p");

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
                draw(elementId);
            });
    }

    function first_draw(elementId) {
        if (!$("#"+elementId).data("firstDrawn")) {
            let running = false;
            window.onresize = () => {
                if (!running) {
                    running = true;
                    draw(elementId);
                    running = false;
                }
            };

            $("#" + widgetManager.getWidget("circlechart-outer").handleId).click(function() {
                draw(elementId);
            });

            add_toggle(
                elementId,
                "default-gray-toggle",
                "Grey out nodes and links by default",
                settings.default_grey,
                (state) => settings.default_grey = state
            );

            add_toggle(
                elementId,
                "fine-opacity-toggle",
                "Highlight nodes based on edge weight",
                settings.fine_node_highlights,
                (state) => settings.fine_node_highlights = state
            );

            // add_slider(elementId, "size_slider", "Size of the diagram: ", 50, 300, 5, settings.node_count, (count) => {
            //     settings.node_count = count;
            //     settings.radius = 2 * count;
            // });

            draw(elementId);
            $("#"+elementId).data("firstDrawn", true)
        } else {
            console.log("Not executing circleshart#first_draw() because it has already been run.");
        }
    }

    function main(elementId) {
        console.log("Preparing to draw circle chart for element ID " + elementId);
        index.getReadySemaphor().done(() => {
            console.log("Chart drawing has green light from the central index semaphor, requesting data");
            data.awaitData("relationCounts").done(() => {
                console.log("Loading data was successful. Checking if the input column also gives green light.");
                let inputcolReadyPromise = $("#inputcol").data("animationtimer");
                if (inputcolReadyPromise)
                    inputcolReadyPromise.done(() =>
                        first_draw(elementId));
                else
                    first_draw(elementId);
            });
        });
    }

    return main;
});
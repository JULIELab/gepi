define(["jquery", "gepi/pages/index", "gepi/charts/data"], function($, index, data) {

    //return function drawSankeyChart(elementId, sankeyDat) {

    let settings = {
        radius: 200,
        padding: 100,
        node_spacing: 10,
        node_thickness: 10,
    };

    function prepare_data(links) {
        // input: links der form (sourceId, targetId, frequency)
        //let {raw_nodes, links} = data();
        let node_indices = {};

        let total_weight = 0;
        let node_weights = {};
        let nodes = [];
        let raw_nodes = [];

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
                    node_indices[node] = index;
                }
                node_weights[node] += w;
            }
            add_to_node(source, frequency);
            add_to_node(target, frequency);
        }
        // jetzt haben wir node_weights der form nodeId -> frequencySum
        // raw_nodes: [nodeId]
        // node_indices = nodeId -> index in raw_nodes

        let sorted_nodes_and_weights = Array.from(Object.entries(node_weights)).sort(([n1, w1], [n2, w2]) => w2 - w1).slice(0, 100);
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
        const node_distance = 360 / 100;

        let compute_link_offset = at => node_indices[at] * node_distance;
        for (let link of links) {
            link.start_pos = compute_link_offset(link.source);
            link.end_pos = compute_link_offset(link.target);
        }
        // the links now have (sourceId, targetId, frequency, start_pos, end_pos)
        // thus, everything required to draw them

        // raw_nodes: [nodeId] (i.e. index below is just the array index)
        // node_weights: nodeId -> frequencySum
        // nodes is empty until now
        for (let [index, id] of raw_nodes.entries()) {
            if (included_nodes[id]) {
                let weight = node_weights[id];
                let new_index = nodes.length;
                nodes.push({
                    id,
                    pos: new_index * node_distance,
                    weight,
                });
            }
        }
        // now we have
        // nodes: [{nodeId, pos, weight}]

        console.log(node_weights);
        console.log(links);

        // links: [(sourceId, targetId, frequency, start_pos, end_pos)]
        // raw_nodes: [nodeId]
        // nodes: [{nodeId, pos, weight}]
        return {
            links,
            raw_nodes,
            nodes,
        };
    }

    function get_svg(elementId) {
        let chart = d3.select(document.getElementById(elementId));

        chart.selectAll("svg").remove();

        let svg = chart
            .append("svg")
            .attr("width", 2 * settings.radius + 2 * settings.padding)
            .attr("height", 2 * settings.radius + 2 * settings.padding);

        let offset = settings.padding + settings.radius;

        return svg.append("g").attr("transform", "translate(" + offset + "," + offset + ")");

    }

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


        let nodes = svg.append("g")
            .attr("class", "nodes")
            .selectAll("g.node")
            .data(chartData.nodes)
            .enter().append("g")
            .attr("class", "node")
            .attr("transform", function(d) {
                return "rotate(" + (d.pos - 90) + ")";
            });

        let node_texts = nodes.append("text")
            .attr("class", "nodeText")
            .attr("opacity", "1")
            .attr("x", settings.radius + 10)
            .attr("y", 4)
            .text(d => nodesById.get(d.id).name);

        node_texts.filter(d => ((d.pos % 360) + 360) % 360 > 180)
            .attr("transform", "rotate(180)")
            .attr("x", -settings.radius - 10)
            .attr("text-anchor", "end");


        // hack: global
        if (window.opacity_base === undefined) {
            window.opacity_base = 0.97;
        }

        let links = svg.append("g")
            .attr("class", "links")
            .selectAll("path.link")
            .data(chartData.links)
            .enter().append("path")
            .attr("class", "link")
            .attr("fill", "none")
            .attr("opacity", d => (1 - Math.pow(window.opacity_base, d.frequency)))
            .attr("stroke", "#000055")
            .attr("d", compute_link_path)
            .attr("stroke-width", 5);

        opacity_redraw = () => draw(elementId);
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

    function main(elementId) {
        console.log("Preparing to draw circle chart for element ID " + elementId);
        index.getReadySemaphor().done(() => {
            console.log("Chart drawing has green light from the central index semaphor, requesting data");
            data.awaitData("relationCounts").done(() => {
                console.log("Loading data was successful. Checking if the input column also gives green light.");
                let inputcolReadyPromise = $("#inputcol").data("animationtimer");
                if (inputcolReadyPromise)
                    inputcolReadyPromise.done(() =>
                        draw(elementId));
                else
                    draw(elementId);
            });
        });
    }

    return main;
});
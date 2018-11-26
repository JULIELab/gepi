define([ "jquery", "gepi/pages/index", "gepi/gcharts/sankey/data" ], function($, index, data) {

    //return function drawSankeyChart(elementId, sankeyDat) {

    let settings = {
        radius: 200,
        padding: 100,
        node_spacing: 10,
        node_thickness: 10,
    };

    function data() {
        let nodes = ["A", "B", "C", "D", "E", "Long", "EvenLonger", "Lonely", "universal"];
        let links = [
            {
                from: "A",
                to: "B",
                weight: 4,
            },
            {
                from: "A",
                to: "C",
                weight: 2,
            },
            {
                from: "C",
                to: "D",
                weight: 1,
            },
            {
                from: "E",
                to: "B",
                weight: 3,
            },
            {
                from: "Long",
                to: "B",
                weight: 2.2,
            },
            {
                from: "universal",
                to: "A",
                weight: 1,
            },
            {
                from: "universal",
                to: "B",
                weight: 1,
            },
            {
                from: "universal",
                to: "C",
                weight: 1,
            },
            {
                from: "universal",
                to: "D",
                weight: 3,
            },
            {
                from: "universal",
                to: "E",
                weight: 1,
            },
            {
                from: "universal",
                to: "Long",
                weight: 1,
            },
            {
                from: "universal",
                to: "EvenLonger",
                weight: 1,
            },
        ];

        return {
            raw_nodes: nodes,
            links,
        }
    }

    function prepare_data(links) {
        //let {raw_nodes, links} = data();
        let node_indices = {};

        let total_weight = 0;
        let node_data = {};
        let nodes = [];
        let raw_nodes = [];

        for (let [index, id] of raw_nodes.entries()) {
            node_indices[id] = index;
        }

        for (let {source, target, weight} of links) {
            total_weight += weight;
            function add_to_node(node, w, other_index) {
                if (!node_data[node]) {
                    node_data[node] = {
                        weight: 0,
                        links: [],
                    };
                    let index = raw_nodes.length;
                    raw_nodes.push(node);
                    node_indices[node] = index;
                }
                node_data[node].weight += w;
                node_data[node].links[other_index] = {
                    weight: w,
                };
            }
            add_to_node(source, weight, node_indices[target]);
            add_to_node(target, weight, node_indices[source]);
        }

        const node_distance = 360 / raw_nodes.length;

        for (let link of links) {
            function compute_link_offset(at) {
                return node_indices[at] * node_distance;
            }

            link.start_pos = compute_link_offset(link.source);
            link.end_pos = compute_link_offset(link.target);
        }

        for (let [index, id] of raw_nodes.entries()) {
            let data = node_data[id];
            nodes[index] = {
                id,
                pos: index * node_distance,
                weight: data.weight,
            };
        }

        console.log(node_data);
        console.log(links);

        return {
            links,
            raw_nodes,
            nodes,
        }
    }

    function get_svg(elementId) {
        let chart = d3.select(document.getElementById(elementId));

        chart.selectAll("svg").remove();

        let svg = chart
            .append("svg")
            .attr("width", 2 * settings.radius + 2 * settings.padding)
            .attr("height", 2 * settings.radius + 2 * settings.padding);

        let offset = settings.padding + settings.radius;

        return svg.append("g").attr("transform", "translate("+offset+","+offset+")");

    }

    function draw(elementId, data) {
        let svg = get_svg(elementId);

        let nodes = svg.append("g")
            .attr("class", "nodes")
            .selectAll("g.node")
            .data(data.nodes)
            .enter().append("g")
            .attr("class", "node")
            .attr("transform", d => "rotate("+(d.pos - 90)+")");

        let node_texts = nodes.append("text")
            .attr("class", "nodeText")
            .attr("opacity", "1")
            .attr("x", settings.radius + 10)
            .attr("y", 4)
            .text(d => d.id);

        node_texts.filter(d => ((d.pos % 360) + 360) % 360 > 180)
            .attr("transform", "rotate(180)")
            .attr("x", -settings.radius - 10)
            .attr("text-anchor", "end");

        console.log(data.nodes);

        let links = svg.append("g")
            .attr("class", "links")
            .selectAll("path.link")
            .data(data.links)
            .enter().append("path")
            .attr("class", "link")
            .attr("fill", "none")
            .attr("opacity", d => (1 - Math.pow(0.9, d.weight)))
            .attr("stroke", "#000055")
            .attr("d", compute_link_path)
            .attr("stroke-width", 10);
    }

    function deg_to_coord(deg) {
        let r = settings.radius;
        let rad = deg / 180 * Math.PI;
        return (r * Math.sin(rad)) + " " + (-1 * r * Math.cos(rad));
    }

    function compute_link_path(link) {
        let {start_pos, end_pos} = link;
        let path = "M " + deg_to_coord(start_pos)
            + " Q 0 0 " + deg_to_coord(end_pos);

        return path;
    }

    function main(elementId, raw_data) {
        let data = prepare_data(raw_data);
        draw(elementId, data);
    }

    return main;
});
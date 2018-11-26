define([ "jquery", "gepi/pages/index", "gepi/gcharts/sankey/data" ], function($, index, data) {

    //return function drawSankeyChart(elementId, sankeyDat) {

    let settings = {
        radius: 200,
        padding: 100,
        node_spacing: 10,
        node_thickness: 10,
    };

    function prepare_data(links) {
        //let {raw_nodes, links} = data();
        let node_indices = {};

        let total_weight = 0;
        let node_weights = {};
        let nodes = [];
        let raw_nodes = [];

        for (let {source, target, weight} of links) {
            total_weight += weight;
            function add_to_node(node, w, other_index) {
                if (node_weights[node] === undefined) {
                    node_weights[node] = 0;
                    let index = raw_nodes.length;
                    raw_nodes.push(node);
                    node_indices[node] = index;
                }
                node_weights[node] += w;
            }
            add_to_node(source, weight, node_indices[target]);
            add_to_node(target, weight, node_indices[source]);
        }

        let sorted_nodes_and_weights = Array.from(Object.entries(node_weights)).sort(([n1, w1], [n2, w2]) => w2 - w1).slice(0, 100);
        let included_nodes = {};

        for ([n, w] of sorted_nodes_and_weights) {
            included_nodes[n] = true;
        }

        console.log("Included nodes: ", included_nodes);

        links = links.filter(({source, target}) => {
            return included_nodes[source] && included_nodes[target];
        });

        const node_distance = 360 / 100;

        for (let link of links) {
            function compute_link_offset(at) {
                return node_indices[at] * node_distance;
            }

            link.start_pos = compute_link_offset(link.source);
            link.end_pos = compute_link_offset(link.target);
        }

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

        console.log(node_weights);
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

        console.log(data.nodes);

        let nodes = svg.append("g")
            .attr("class", "nodes")
            .selectAll("g.node")
            .data(data.nodes)
            .enter().append("g")
            .attr("class", "node")
            .attr("transform", function(d) {
                return "rotate("+(d.pos - 90)+")";
            });

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

        // hack: global
        if (window.opacity_base === undefined) {
            window.opacity_base = 0.97;
        }

        let links = svg.append("g")
            .attr("class", "links")
            .selectAll("path.link")
            .data(data.links)
            .enter().append("path")
            .attr("class", "link")
            .attr("fill", "none")
            .attr("opacity", d => (1 - Math.pow(window.opacity_base, d.weight)))
            .attr("stroke", "#000055")
            .attr("d", compute_link_path)
            .attr("stroke-width", 5);

        opacity_redraw = () => draw(elementId, data);
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
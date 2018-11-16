define(["gepi/gcharts/sankey/weightfunctions"], function(functions) {
    function color_edges(input_links) {

        for (let link of input_links) {
            let {
                source,
                target,
                type
            } = link;

            let color = "";
            if (type) {
                color = "red";

                link.color = color;
            }
        }
    }

    /*
     * Computes the sum of weights for each node, divided into "left" and "right"
     * according to the events' "source" and "target", respectively.
     * The weight of a node is the sum of the weights, e.g. frequencies, of its
     * incident edges, i.e. event links to other nodes.
     *
     * Also colors the input links by adding a "color" field.
     *
     * Returns:
     *   - the input links with their color,
     *   - the descending weight-sorted left nodes
     *   - the descending weight-sorted right nodes
     *   - the sum of all link weights
     */
    function preprocess_data(nodesNLinks, weightFunction) {
        color_edges(nodesNLinks.links);

        let {
            filtered_links,
            filtered_nodes
        } = cutoffLinksByWeight(nodesNLinks, 0);

let thelink = [];
        for(let link of nodesNLinks.links) {
            if (link.target === "atid24231")
                thelink.push(link);
        }


        console.log("Calling weight function");
        let t0 = performance.now();
        let {leftnodes, rightnodes} = functions[weightFunction](nodesNLinks);
        let t1 = performance.now();
        console.log("Call to the weightFunction took " + (t1 - t0) + " milliseconds.");
        leftnodes.sort((a, b) => b[weightFunction] - a[weightFunction]);
        rightnodes.sort((a, b) => b[weightFunction] - a[weightFunction]);

        //let sorted_ids_and_weights_left = getSortedIdsAndWeights(filtered_links, "source", weightFunction);
        //let sorted_ids_and_weights_right = getSortedIdsAndWeights(filtered_links, "target", weightFunction);

        let total_weight = 0;
        for (let link of filtered_links) {
            total_weight += link.frequency;
        }

        return {
            nodesNLinks: {
                links: filtered_links,
                nodes: filtered_nodes
            },
            sorted_ids_and_weights_left: leftnodes,
            sorted_ids_and_weights_right: rightnodes,
            total_weight
        };
    }

    function cutoffLinksByWeight(nodesNLinks, cutoff) {
        let filtered_links = [];
        for (let link of nodesNLinks.links) {
            if (link.frequency >= cutoff)
                filtered_links.push(link);
        }
        let filtered_node_ids = {};
        for (let link of filtered_links) {
            if (!filtered_node_ids[link.source])
                filtered_node_ids[link.source] = true;
            if (!filtered_node_ids[link.target])
                filtered_node_ids[link.target] = true;
        }
        let filtered_nodes = [];
        for (let node of nodesNLinks.nodes) {
            if (filtered_node_ids[node.id])
                filtered_nodes.push(node);
        }
        return {
            filtered_links,
            filtered_nodes
        };
    }

    function getSortedIdsAndWeights(input_links, link_field, weightFunction) {
        let weightsById = getWeightsById(input_links, link_field, weightFunction);
        let weightsAndIds = Object.entries(weightsById).map(([id, weight]) => ({
            id,
            weight
        }));
        weightsAndIds.sort((a, b) => (b.weightFunction - a.weightFunction));
        return weightsAndIds;
    }

    function getWeightsById(input_links, link_field) {
        let weightsById = {};
        for (let link of input_links) {
            let relevant_node_id = link[link_field];
            weightsById[relevant_node_id] = (weightsById[relevant_node_id] || 0) + link.frequency;
        }
        return weightsById;
    }

    function prepare_data(pre_data, total_height, min_height, padding, max_number_nodes) {

        let {
            nodesNLinks,
            sorted_ids_and_weights_left,
            sorted_ids_and_weights_right,
            total_weight,
        } = pre_data;

        let included_ids_from = getIncludedIds(sorted_ids_and_weights_left, total_weight, total_height, min_height, padding, max_number_nodes);
        let included_ids_to = getIncludedIds(sorted_ids_and_weights_right, total_weight, total_height, min_height, padding, max_number_nodes);

        let {
            filtered_links,
            misc_from,
            misc_to,
        } = filter_and_suffix_links(nodesNLinks.links, included_ids_from, included_ids_to);
        let filtered_nodes = filter_and_suffix_nodes(nodesNLinks, included_ids_from, included_ids_to, misc_from, misc_to);

        return {
            nodes: filtered_nodes,
            links: filtered_links,
        };
    }



    /*
     * Returns the node ids of the leading elements in 'sorted_ids_and_weights' that can be displayed
     * taking into account the given 'total_height' of the diagram, the 'min_height' of each displayed
     * node and the 'padding' between the nodes.
     */
    function getIncludedIds(sorted_ids_and_weights, total_weight, total_height, min_height, padding, max_number_nodes) {

        let included_ids = {};
        let first_node = true;
        let num = 0;

        for (let node of sorted_ids_and_weights) {
            if (num < max_number_nodes)
                included_ids[node.id] = true;
            ++num;
            /*
            if (!first_node) {
                total_height -= padding;
            }
            first_node = false;

            total_weight += node.weight;
            let min_weight = node.weight;

            if (total_height / total_weight < min_height / min_weight) {
                break;
            }

            included_ids[node.id] = true;*/
        }

        return included_ids;
    }

    /*
     * Filters 'input_links' according the the provided included nodes.
     * Computes the surrogate edges to and from the misc nodes (one left, one right).
     */
    function filter_and_suffix_links(input_links, included_ids_from, included_ids_to) {
        let filtered_links = [];
        let from_misc_by_id_and_color = {};
        let to_misc_by_id_and_color = {};
        let between_misc_by_color = {};

        let misc_from = false;
        let misc_to = false;

        // Filter the links according to the included nodes, adding up the weights of the links filtered out

        for (let link of input_links) {
            if (included_ids_from[link.source]) {
                if (included_ids_to[link.target]) {
                    filtered_links.push({
                        source: link.source + "_from",
                        target: link.target + "_to",
                        value: link.frequency,
                        color: link.color,
                    });
                } else {
                    let from = link.source + "_from";
                    let id_to_misc_by_color = to_misc_by_id_and_color[from] || {};
                    id_to_misc_by_color[link.color] = (id_to_misc_by_color[link.color] || 0) + link.frequency;
                    to_misc_by_id_and_color[from] = id_to_misc_by_color;
                }
            } else {
                if (included_ids_to[link.target]) {
                    let to = link.target + "_to";
                    let misc_to_id_by_color = from_misc_by_id_and_color[to] || {};
                    misc_to_id_by_color[link.color] = (misc_to_id_by_color[link.color] || 0) + link.weight;
                    from_misc_by_id_and_color[to] = misc_to_id_by_color;
                } else {
                    between_misc_by_color[link.color] = (between_misc_by_color[link.color] || 0) + link.weight;
                }
            }
        }

        // Add "fake links" connecting nodes to misc nodes

        for (let [id, misc_to_id_by_color] of Object.entries(from_misc_by_id_and_color)) {
            for (let [color, weight] of Object.entries(misc_to_id_by_color)) {
                filtered_links.push({
                    source: "MISC_from",
                    target: id,
                    value: weight,
                    color,
                });
            }
            misc_from = true;
        }

        for (let [id, id_to_misc_by_color] of Object.entries(to_misc_by_id_and_color)) {
            for (let [color, weight] of Object.entries(id_to_misc_by_color)) {
                filtered_links.push({
                    source: id,
                    target: "MISC_to",
                    value: weight,
                    color,
                });
            }
            misc_to = true;
        }

        for (let [color, weight] of Object.entries(between_misc_by_color)) {
            filtered_links.push({
                source: "MISC_from",
                target: "MISC_to",
                value: weight,
                color,
            });
            misc_from = true;
            misc_to = true;
        }

        return {
            filtered_links,
            misc_from,
            misc_to
        };
    }

    function filter_and_suffix_nodes(nodesNLinks, included_ids_from, included_ids_to, misc_from, misc_to) {
        let raw_nodes = nodesNLinks.nodes;
        let filtered_nodes = [];

        for (let node of raw_nodes) {
            if (included_ids_from[node.id]) {
                filtered_nodes.push({
                    id: node.id + "_from",
                    name: node.name,
                });
            }

            if (included_ids_to[node.id]) {
                filtered_nodes.push({
                    id: node.id + "_to",
                    name: node.name,
                });
            }
        }

        if (misc_from) {
            filtered_nodes.push({
                id: "MISC_from",
                name: "Other",
            });
        }

        if (misc_to) {
            filtered_nodes.push({
                id: "MISC_to",
                name: "Other",
            });
        }

        return filtered_nodes;
    }

    return {
        preprocess_data,
        prepare_data,
    };
});
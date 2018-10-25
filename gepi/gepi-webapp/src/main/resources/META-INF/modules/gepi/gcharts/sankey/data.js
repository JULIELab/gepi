define(function() {
    function convert_data(input_links) {
        let nodes = {};
        let raw_links = [];

        for (let [n1, n2, weight] of input_links) {
            for (let n of [n1, n2]) {
                if (!nodes[n]) {
                    nodes[n] = {
                        id: n,
                        name: n,
                    }
                }
            }
            raw_links.push({
                source: n1,
                target: n2,
                weight,
                color: "teal",
            });
        }

        let raw_nodes = Object.values(nodes);

        return {
            raw_nodes,
            raw_links,
        };
    }

    function preprocess_data(input_links) {
        let {
            raw_nodes,
            raw_links,
        } = convert_data(input_links);

        let sorted_ids_and_weights_left = getSortedIdsAndWeights(raw_links, "source");
        let sorted_ids_and_weights_right = getSortedIdsAndWeights(raw_links, "target");

        let total_weight = 0;
        for (let link of raw_links) {
            total_weight += link.weight;
        }

        return {
            raw_links,
            raw_nodes,
            sorted_ids_and_weights_left,
            sorted_ids_and_weights_right,
            total_weight,
        };
    }

    function prepare_data(pre_data, total_height, min_height, padding) {

        let {
            raw_links,
            raw_nodes,
            sorted_ids_and_weights_left,
            sorted_ids_and_weights_right,
            total_weight,
        } = pre_data;

        let included_ids_from = getIncludedIds(sorted_ids_and_weights_left, total_weight, total_height, min_height, padding);
        let included_ids_to = getIncludedIds(sorted_ids_and_weights_right, total_weight, total_height, min_height, padding);

        let {
            filtered_links,
            misc_from,
            misc_to,
        } = filter_and_suffix_links(raw_links, included_ids_from, included_ids_to);
        let filtered_nodes = filter_and_suffix_nodes(raw_nodes, included_ids_from, included_ids_to, misc_from, misc_to);

        return {
            nodes: filtered_nodes,
            links: filtered_links,
        };
    }

    function getWeightsById(raw_links, link_field) {
        let weightsById = {};
        for (let link of raw_links) {
            let relevant_node_id = link[link_field];
            weightsById[relevant_node_id] = (weightsById[relevant_node_id] || 0) + link.weight;
        }
        return weightsById;
    }

    function getSortedIdsAndWeights(raw_links, link_field) {
        let weightsById = getWeightsById(raw_links, link_field);
        let weightsAndIds = Object.entries(weightsById).map(([id, weight]) => ({id, weight}));
        weightsAndIds.sort((a, b) => (b.weight - a.weight));
        return weightsAndIds;
    }

    function getIncludedIds(sorted_ids_and_weights, total_weight, total_height, min_height, padding) {

        let included_ids = {};
        let first_node = true;

        for (let node of sorted_ids_and_weights) {
            if (!first_node) {
                total_height -= padding;
            }
            first_node = false;

            total_weight += node.weight;
            let min_weight = node.weight;

            if (total_height / total_weight < min_height / min_weight) {
                break;
            }

            included_ids[node.id] = true;
        }

        return included_ids;
    }

    function filter_and_suffix_links(raw_links, included_ids_from, included_ids_to) {
        let filtered_links = [];
        let from_misc_by_id_and_color = {};
        let to_misc_by_id_and_color = {};
        let between_misc_by_color = {};

        let misc_from = false;
        let misc_to = false;

        // Filter the links, adding up the weights of the links filtered out

        for (let link of raw_links) {
            if (included_ids_from[link.source]) {
                if (included_ids_to[link.target]) {
                    filtered_links.push({
                        source: link.source + "_from",
                        target: link.target + "_to",
                        value: link.weight,
                        color: link.color,
                    });
                } else {
                    let from = link.source + "_from";
                    let id_to_misc_by_color = to_misc_by_id_and_color[from] || {};
                    id_to_misc_by_color[link.color] = (id_to_misc_by_color[link.color] || 0) + link.weight;
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

    function filter_and_suffix_nodes(raw_nodes, included_ids_from, included_ids_to, misc_from, misc_to) {
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
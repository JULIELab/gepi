define(["jquery", "t5/core/ajax", "gepi/charts/sankey/weightfunctions"], function($, t5ajax, functions) {
    // This map holds the original data downloaded from the web application
    // as well as transformed versions for caching
    let data = new Map();
    let sortedNodes = new Map();
    // For synchonization: Deferrer objects created on data requests
    // which are resolved when a
    // specific dataset has actually been set
    let requestedData = new Map();
    let dataUrl = null;

    function getSortedNodes(nodesNLinks, orderFunction) {
        let nodesObject = sortedNodes.get(orderFunction);
        if (!nodesObject) {
            console.log("Calling order function");
            let t0 = performance.now();
            nodesObject = functions[orderFunction](nodesNLinks);
            nodesObject.leftnodes.sort((a, b) => b[orderFunction] - a[orderFunction]);
            nodesObject.rightnodes.sort((a, b) => b[orderFunction] - a[orderFunction]);
            let t1 = performance.now();
            console.log("Call to the order function and sorting took " + (t1 - t0) + " milliseconds.");
            sortedNodes.set(orderFunction, nodesObject);
        }
        return nodesObject;
    }

    function setDataUrl(url) {
        dataUrl = url;
        console.log("URL to request data has been set to " + dataUrl);
    }

    function getDataUrl() {
        return dataUrl;
    }

    function loadData(source) {
        console.log("Loading data with source " + source + " from " + dataUrl);
        $.get(dataUrl, "datasource=" + source, data => setData(source, data));
    }

    function setData(name, dataset) {
        data.set(name, dataset);
        console.log("Data for key " + name + " was set, its promise is resolved.");
        awaitData(name).resolve();
    }

    function getData(name) {
        return data.get(name);
    }

    function awaitData(sourceName) {
        console.log("Data with source name " + sourceName + " was requested");
        let promise = requestedData.get(sourceName);
        if (!promise) {
            console.log("Creating new promise for data " + sourceName);
            promise = $.Deferred();
            requestedData.set(sourceName, promise);
            loadData(sourceName);
        } else {
            console.log("Data with source name " + sourceName + " was already requested and is not loaded again.");
        }
        return promise;
    }

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
    function preprocess_data(nodesNLinks, orderFunction) {
        color_edges(nodesNLinks.links);

        let {
            filtered_links,
            filtered_nodes
        } = cutoffLinksByWeight(nodesNLinks, 0);

        let sortedNodes = getSortedNodes({links: filtered_links, nodes: filtered_nodes}, orderFunction);


        let left_nodes_by_id = {};
        let right_nodes_by_id = {};
        for (let node of sortedNodes.leftnodes) {
            node.node_frequency = 0;
            left_nodes_by_id[node.id] = node;
        }
        for (let node of sortedNodes.rightnodes) {
            node.node_frequency = 0;
            right_nodes_by_id[node.id] = node;
        }

        let total_frequency = 0;
        for (let link of filtered_links) {
            total_frequency += link.frequency;
            left_nodes_by_id[link.source].node_frequency += link.frequency;
            right_nodes_by_id[link.target].node_frequency += link.frequency;
        }

        return {
            nodesNLinks: {
                links: filtered_links,
                nodes: filtered_nodes
            },
            sorted_ids_and_weights_left: sortedNodes.leftnodes,
            sorted_ids_and_weights_right: sortedNodes.rightnodes,
            total_frequency,
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

    function prepare_data(pre_data, total_height, min_height, padding, show_other, max_other_size) {

        let {
            nodesNLinks,
            sorted_ids_and_weights_left,
            sorted_ids_and_weights_right,
            total_frequency,
        } = pre_data;

        let included_ids_from = getIncludedIds(sorted_ids_and_weights_left, total_frequency, total_height, min_height, padding, show_other, max_other_size);
        let included_ids_to = getIncludedIds(sorted_ids_and_weights_right, total_frequency, total_height, min_height, padding, show_other, max_other_size);

        let {
            filtered_links,
            misc_from,
            misc_to,
        } = filter_and_suffix_links(nodesNLinks.links, included_ids_from, included_ids_to, show_other);
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
    function getIncludedIds(sorted_ids_and_weights, total_frequency, total_height, min_height, padding, show_other, max_other_size) {

        let included_ids = {};

        let frequency_so_far = 0;
        let padding_so_far = 0;
        if (!show_other) padding_so_far = -padding;
        let min_frequency = Infinity;

        for (let node of sorted_ids_and_weights) {

            padding_so_far += padding;
            frequency_so_far += node.node_frequency;
            min_frequency = Math.min(node.node_frequency, min_frequency);

            let min_scale = min_height / min_frequency;
            let real_node_height = frequency_so_far * min_scale;
            let other_node_height = 0;
            if (show_other) {
                other_node_height = Math.min(min_scale * (total_frequency-frequency_so_far), max_other_size);
            }

            if (real_node_height + other_node_height + padding_so_far > total_height) {
                break;
            }

            included_ids[node.id] = true;
        }

        return included_ids;
    }

    /*
     * Filters 'input_links' according the the provided included nodes.
     * Computes the surrogate edges to and from the misc nodes (one left, one right).
     */
    function filter_and_suffix_links(input_links, included_ids_from, included_ids_to, show_other) {
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

        if (show_other) {
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
        setData,
        getData,
        awaitData,
        setDataUrl,
        getDataUrl
    };
});
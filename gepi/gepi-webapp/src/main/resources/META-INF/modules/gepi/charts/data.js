    define(["jquery", "t5/core/ajax", "gepi/charts/sankey/weightfunctions"], function($, t5ajax, functions) {
        // This map holds the original data downloaded from the web application
        // as well as transformed versions for caching
        let data = new Map();
        // For synchronization: Deferrer objects created on data requests
        // which are resolved when a
        // specific dataset has actually been set
        let requestedData = new Map();
        let dataUrl = null;

        function getSortedNodes(nodesNLinks, orderFunction) {
            let t0 = performance.now();
            nodesObject = functions[orderFunction](nodesNLinks);
            nodesObject.leftnodes.sort((a, b) => b[orderFunction] - a[orderFunction]);
            nodesObject.rightnodes.sort((a, b) => b[orderFunction] - a[orderFunction]);
            let t1 = performance.now();
            console.log("Call to the order function and sorting took " + (t1 - t0) + " milliseconds.");

            return nodesObject;
        }

        function setDataUrl(url) {
            dataUrl = url;
        }

        function getDataUrl() {
            return dataUrl;
        }

        function clearData() {
            data = new Map();
            requestedData = new Map();
        }

        function loadData(sourceName, dataSessionId) {
            parameters = "datasource=" + sourceName + "&dataSessionId=" + dataSessionId;
            console.log("Loading data with parameters " + parameters + " from " + dataUrl);
            $.get(dataUrl, parameters, data => setData(sourceName, dataSessionId, data));
        }

        function setData(sourceName, dataSessionId, dataset) {
            data.set(makeDataKey(sourceName, dataSessionId), dataset);
            console.log("Data for key " + sourceName + " for dataSessionId " + dataSessionId + " was set, its promise is resolved.");
            awaitData(sourceName, dataSessionId).resolve();
        }

        function getData(sourceName, dataSessionId) {
            return data.get(makeDataKey(sourceName, dataSessionId));
        }

        function makeDataKey(sourceName, dataSessionId) {
            return sourceName + "_" + dataSessionId;
        }

        function awaitData(sourceName, dataSessionId) {
            const key = makeDataKey(sourceName, dataSessionId);
            let promise = requestedData.get(key);
            if (!promise) {
                console.log("Data with source name " + sourceName + " was requested for dataSessionId " + dataSessionId);
                promise = $.Deferred();
                requestedData.set(key, promise);
                loadData(sourceName, dataSessionId);
            } else {
                console.log("Data with source name " + sourceName + " was already requested for dataSessionId " + dataSessionId + " and is not loaded again.");
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

                let color = "gray";
                // if (type) {
                //     color = "gray";

                //     link.color = color;
                // }
                link.color = color;
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
        function preprocess_data_for_sankey(nodesNLinks, orderFunction) {
            color_edges(nodesNLinks.links);

            let {
                filtered_links,
                filtered_nodes
            } = cutoffLinksByWeight(nodesNLinks, 0);

            // here the orderFunction is applied, i.e. the nodes are sorted by frequency or harmonic mean of
            // source and target frequencies
            let sortedNodes = getSortedNodes({
                links: filtered_links,
                nodes: filtered_nodes
            }, orderFunction);
            // When sorting by another measure than frequency - i.e. harmonic mean - weights can be zero,
            // indicating that the nodes do not show the measured property at all (in case of harmonic
            // mean: this node does not belong to common interaction triangle). Those should not be shown.
            if (orderFunction !== 'frequency') {
                sortedNodes.leftnodes = sortedNodes.leftnodes.filter(n => n[orderFunction] > 0);
                sortedNodes.rightnodes = sortedNodes.rightnodes.filter(n => n[orderFunction] > 0);
            }

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
            // Again, for harmonic mean the weight may be 0 for nodes. Remove those and links that that would
            // be connected to a node that was deleted above.
            if (orderFunction !== 'frequency') {
                filtered_nodes = filtered_nodes.filter(n => left_nodes_by_id[n.id] || right_nodes_by_id[n.id]);
                filtered_links = filtered_links.filter(l => left_nodes_by_id[l.source] && right_nodes_by_id[l.target]);
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

        /*
         * Expects the output from preprocess_data_for_sankey. Does some filtering that depends on the
         * available canvas size and user settings such as padding and whether the 'other' link should
         * be displayed.
         */
        function prepare_data_new(pre_data, total_height, min_height, padding, show_other, max_other_size) {
            let {
                nodesNLinks,
                // the nodes are objects with keys 'id' and 'frequency'
                sorted_ids_and_weights_left,
                sorted_ids_and_weights_right,
                total_frequency,
            } = pre_data;
            // This is just some heuristic. We can determine how many nodes are shown
            // but the ones displayed should have some room.
            const maxNumNodes = total_height / (padding + 20);
            // Some helper data structures
            const linksBySource = new Map();
            for (let link of nodesNLinks.links) {
                let links4node = linksBySource.get(link.source);
                if (!links4node) {
                    links4node = [];
                    linksBySource.set(link.source, links4node);
                }
                links4node.push(link);
            }
            const id2name = new Map();
            for (let node of nodesNLinks.nodes)
                id2name.set(node.id, node.name);
            // Now the actual work begins: gather nodes and links that fit into the available screen space,
            //  i.e. up to maxNumNodes
            const filtered_nodes = [];
            const filtered_links = [];
            const leftNodeIds = new Set();
            const rightNodeIds = new Set();
            const keptLinks = new Set();
            const linkKeyFunc = link => link.source + "-" + link.target;
            
            // Get the nodes and links until maxNumNodes is reached
            assembleNodesAndLinksForMaxNumNodes(maxNumNodes, sorted_ids_and_weights_left, linksBySource, keptLinks, leftNodeIds, rightNodeIds, linkKeyFunc, id2name, filtered_nodes, filtered_links)

            if (show_other)
                makeOtherNodes(nodesNLinks.links, keptLinks, leftNodeIds, rightNodeIds, linkKeyFunc, filtered_nodes, filtered_links);

            return {
                nodes: filtered_nodes,
                links: filtered_links,
            };
        }

        /*
         * Assembles the top-scored nodes and their incident links until the number of either left or right nodes
         * reaches maxNumNodes. maxNumNodes should be chosen to allow as much nodes to the available space
         * as are still nicely drawable, i.e. "look good".
         */
        function assembleNodesAndLinksForMaxNumNodes(maxNumNodes, sorted_ids_and_weights_left, linksBySource, keptLinks, leftNodeIds, rightNodeIds, linkKeyFunc, id2name, filtered_nodes, filtered_links) {
            // Collect the hightest-frequency nodes (i.e. nodes with the highest cumulative link-frequencies)
            // and their links until the maximum number of nodes is reached. This leaves no missing or extra
            // nodes but for each included link its source and target node.
            for (let node of sorted_ids_and_weights_left) {
                const links4node = linksBySource.get(node.id);
                for (let link of links4node) {
                    filtered_links.push({
                        source: link.source + "_from",
                        target: link.target + "_to",
                        // The preprocessing function always outputs the 'frequency' property,
                        // even when its the common interaction partners sankey with the harmonic mean
                        // measure.
                        // The D3 Sankey plugin expects the 'value' property from links.
                        value: link.frequency
                    });
                    keptLinks.add(linkKeyFunc(link));
                    if (!leftNodeIds.has(link.source)) {
                        filtered_nodes.push({
                            id: link.source + "_from",
                            name: id2name.get(link.source)
                        });
                        leftNodeIds.add(link.source);
                    }
                    if (!rightNodeIds.has(link.target)) {
                        filtered_nodes.push({
                            id: link.target + "_to",
                            name: id2name.get(link.target)
                        });
                        rightNodeIds.add(link.target);
                    }

                    if (leftNodeIds.size > maxNumNodes || rightNodeIds.size > maxNumNodes)
                        break;
                }
                if (leftNodeIds.size > maxNumNodes || rightNodeIds.size > maxNumNodes)
                    break;
            }
        }

        /*
         * If there are displayed nodes with links to non-displayed nodes, this function creates
         * 'other' nodes to replace the non-displayed nodes. This allows to show all links of all
         * displayed nodes without the need to also show all the other nodes, too.
         */
        function makeOtherNodes(links, keptLinks, leftNodeIds, rightNodeIds, linkKeyFunc, filtered_nodes, filtered_links) {
            // The other node allows us to display all links of the included nodes
            // while avoiding the problem that we also need to include potential new
            // nodes. The 'other' nodes are "link drains".

            let leftOtherValue = 0;
            let rightOtherValue = 0;
            for (let link of links) {
                // Omit links that are on display anyway. We look for links that have one node
                // displayed but not the other: this one is replaced by the 'other' node.
                if (!keptLinks.has(linkKeyFunc(link))) {
                    const keptLeftNode = leftNodeIds.has(link.source);
                    const keptRightNode = rightNodeIds.has(link.target);
                    if (keptLeftNode) {
                        rightOtherValue = rightOtherValue + link.frequency;
                        filtered_links.push({
                            source: link.source + "_from",
                            target: "MISC_to",
                            value: link.frequency
                        });
                    } else if (keptRightNode) {
                        leftOtherValue = leftOtherValue + link.frequency;
                        filtered_links.push({
                            source: "MISC_from",
                            target: link.target + "_to",
                            value: link.frequency
                        });
                    }
                }
            }
            if (leftOtherValue) {
                filtered_nodes.push({
                    id: "MISC_from",
                    name: "others"
                });
            }
            if (rightOtherValue) {
                filtered_nodes.push({
                    id: "MISC_to",
                    name: "others"
                });
            }
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
                    misc_to_id_by_color[link.color] = (misc_to_id_by_color[link.color] || 0) + link.frequency;
                    from_misc_by_id_and_color[to] = misc_to_id_by_color;
                } else {
                    between_misc_by_color[link.color] = (between_misc_by_color[link.color] || 0) + link.frequency;
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
            preprocess_data_for_sankey,
            prepare_data,
            setData,
            getData,
            awaitData,
            setDataUrl,
            getDataUrl,
            clearData
        };
    });
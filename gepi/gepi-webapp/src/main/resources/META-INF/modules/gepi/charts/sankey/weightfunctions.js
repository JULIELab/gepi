define(["gepi/formulas", "lodash-amd/groupBy", "lodash-amd/orderBy", "lodash-amd/max"], function(formulas, groupBy, orderBy, max){

    function commonPartnersHarmonicMean(nodesNLinks) {
        let nodesById = {};
        for (let node of nodesNLinks.nodes)
        	nodesById[node.id] = node;
        let leftids = new Map();
        let rightids = new Map();
        // Group the links by their target
        const groupedLinks = groupBy(nodesNLinks.links, link => link.target);
        const maxGroupSize = max(Object.values(groupedLinks).map(group => group.length));
        let maxHm = 0;
        for(let linkGroup of Object.values(groupedLinks)) {
            let hm = formulas.harmonicMean(linkGroup.map(link => link.frequency));
            linkGroup.hm = hm;
            if (hm > maxHm)
            	maxHm = hm;
        }
        for(let linkGroup of Object.values(groupedLinks)) {
            const hm = linkGroup.hm;
            // We now first normalize the harmonic mean value and the group size value by dividing
            // through the respective maximum value. Then we build the mean of these values.
            // The idea: We want to see those nodes that connect strongly a lot of other nodes
            const hmScore = linkGroup.length < 2 ? 0 : formulas.harmonicMean([hm/maxHm,linkGroup.length/maxGroupSize]);
            for(let link of linkGroup) {
                // The links are grouped by target. So they all have the same target and thus, only one
                // value - hm - can be assigned to the target.
                // But each link has a different source. Thus, a source can appear for multiple
                // link groups that share a target. We want to show those sources that connect to the
                // highest ranking targets. Because of that, we assign the source the maximum hm value
                // encountered for any of its connected targets.
                // Note: the property name MUST be the name of this method! It is used to get the right value in data.js
                let source = nodesById[link.source];
                let target = nodesById[link.target];
                if (!leftids.has(source.id))
               	 leftids.set(source.id, {id: source.id, name: source.name});
               	if (!(rightids.has(target.id)))
               		rightids.set(target.id, {id: target.id, name: target.name});
                // The source receives the maximum score of all its links as explained above.
                let sourceweight = leftids.get(link.source).commonPartnersHarmonicMean || 0;
                leftids.get(link.source).commonPartnersHarmonicMean = Math.max(sourceweight, hmScore);
                // The target receives the hm value for its group.
                rightids.get(link.target).commonPartnersHarmonicMean = hmScore;
            }
        }
        let leftnodes  = Array.from(leftids.values());
        let rightnodes = Array.from(rightids.values());
        return {leftnodes, rightnodes};
    }

      function frequency(nodesNLinks) {
         let weightsByIdLeft = {};
         let weightsByIdRight = {};
        for (let link of nodesNLinks.links) {
            let leftNodeId = link.source;
            let rightNodeId = link.target;
            weightsByIdLeft[leftNodeId] = (weightsByIdLeft[leftNodeId] || 0) + link.frequency;
            weightsByIdRight[rightNodeId] = (weightsByIdRight[rightNodeId] || 0) + link.frequency;
        }
        let leftnodes = [];
        let rightnodes = [];
        for (let id in weightsByIdLeft)
            leftnodes.push({id, "frequency": weightsByIdLeft[id]});
        for (let id in weightsByIdRight)
            rightnodes.push({id, "frequency": weightsByIdRight[id]});

        return {leftnodes, rightnodes};
    }

    function noop() {}

    return {commonPartnersHarmonicMean, frequency, noop};
});
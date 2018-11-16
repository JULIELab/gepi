define([], function(){
    // The general harmonic mean formula for x1,...,xn: n / ( 1/x1 + ... + 1/xn)
    function harmonicMean(array) {
        return array.length / array.map(x => 1/x).reduce((sum, x) => sum+x);
    };

    return {harmonicMean};
});
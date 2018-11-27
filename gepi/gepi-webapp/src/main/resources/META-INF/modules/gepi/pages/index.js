define(["jquery"], function($){
    let readyIndicator = $.Deferred();

    let readyForWidgets = function () {
        readyIndicator.resolve();
    };

    let getReadySemaphor = function () {
        return readyIndicator;
    }

    return {readyForWidgets, getReadySemaphor};
    
});
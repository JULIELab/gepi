define(["jquery"], function($){

    // The following is synchronization code. The widgets wait for this
    // deferred object to be resolved before they render. We use this
    // to wait for animations to finish before rendering diagrams.

    // This deferred is the normal jQuery deferred object but with
    // a modified callback acceptance function for done().
    // Our function checks for duplicates and only allows a callback
    // to be added once. This avoid the multiple rendering of
    // diagrams and actually multiple instances of some control elements.
    // Taken from https://stackoverflow.com/a/20425733/1314955
    let readyIndicator = $.extend({}, $.Deferred(), {
         queue: [],
         done: function () {
             var callback = arguments[0];
             console.log("HIER: " + callback)
             if (!~$.inArray(callback, this.queue)) {
                 this.queue.push(callback.toString());
                 this.then($.proxy(function(){this.queue = [];callback()},this));
             }
             return this
         }
     });

    let readyForWidgets = function () {
        console.log("Resolving readyIndicator");
        readyIndicator.resolve();
    };

    let getReadySemaphor = function () {
        return readyIndicator;
    }

    return {readyForWidgets, getReadySemaphor};
    
});
define(["jquery", "bootstrap5/scrollspy"], function($, ScrollSpy){

let setupScrollSpy = function () {
    const scrollSpy = new ScrollSpy(document.body, {
    target: '#toc'
    })
}

return {
	setupScrollSpy: setupScrollSpy
}
    
});
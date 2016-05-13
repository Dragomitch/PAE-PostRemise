"use strict";
var Router = (function() {

    var routes = {};
    var root = '/';
    var tag = 1; // Disable initial popstate on page load

    function getPath() {
        return window.location.pathname;
    }

    window.onpopstate = function (event) {
        if(event.state) {
            PubSub.publish('destroy');
            var callback = resolveRoute(getPath());
            callback();
        }
    }

    function resolveRoute(path) {
        return routes[path];
    }

    function add(template, callback) {
        routes[template] = callback;
    }

    function navigate(path, redirect) {
        var callback = resolveRoute(path);
        if(callback === undefined) {
            Router.navigate('/');
        } else {
            if(redirect === true) {
                history.replaceState({tag: tag}, null, path);
            } else if(path !== '/') {
                history.pushState({tag: tag}, null, path);
            }
            PubSub.publish('destroy');
            callback();
        }
    }

    return {
        add: add,
        navigate: navigate,
        getPath: getPath
    }
})();
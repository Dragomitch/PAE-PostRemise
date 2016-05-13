var PubSub = (function() {

    // Private variables
    var topics = {};
    var oid = 0;
    
    function publish(topic) {
        if(!topics[topic]) {
            return;
        }
        for(var key in topics[topic]) {
            if(topics[topic].hasOwnProperty(key)) {
                (topics[topic][key])();
            }
        }
    }

    function subscribe(topic, callback) {
        // The topic hasn't been initialized yet
        if(!topics.hasOwnProperty(topic)) {
            topics[topic] = {};
        }
        topics[topic][oid++] = callback;
    }

    function unsubscribe(topic, callback) {
        console.log('motiel');
        for(var key in topics[topic]) {
            if(topics[topic].hasOwnProperty(key)) {
                if(topics[topic][key] === callback) {
                    delete topics[topic][key];
                    return true;
                }
            }
        }
        return false;
    }

    function unsubscribeAll(topic) {
        topics[topic] = {};
    }

    // Public API
    return {
        publish: publish,
        subscribe: subscribe,
        unsubscribe: unsubscribe,
        unsubscribeAll: unsubscribeAll
    }

})();
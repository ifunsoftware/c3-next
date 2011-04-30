var authHeader = null

function utf8_encode (argString) {
    // Encodes an ISO-8859-1 string to UTF-8
    //
    // version: 1103.1210
    // discuss at: http://phpjs.org/functions/utf8_encode
    // +   original by: Webtoolkit.info (http://www.webtoolkit.info/)
    // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
    // +   improved by: sowberry
    // +    tweaked by: Jack
    // +   bugfixed by: Onno Marsman
    // +   improved by: Yves Sucaet
    // +   bugfixed by: Onno Marsman
    // +   bugfixed by: Ulrich
    // *     example 1: utf8_encode('Kevin van Zonneveld');
    // *     returns 1: 'Kevin van Zonneveld'
    var string = (argString + ''); // .replace(/\r\n/g, "\n").replace(/\r/g, "\n");
    var utftext = "",
        start, end, stringl = 0;

    start = end = 0;
    stringl = string.length;
    for (var n = 0; n < stringl; n++) {
        var c1 = string.charCodeAt(n);
        var enc = null;

        if (c1 < 128) {
            end++;
        } else if (c1 > 127 && c1 < 2048) {
            enc = String.fromCharCode((c1 >> 6) | 192) + String.fromCharCode((c1 & 63) | 128);
        } else {
            enc = String.fromCharCode((c1 >> 12) | 224) + String.fromCharCode(((c1 >> 6) & 63) | 128) + String.fromCharCode((c1 & 63) | 128);
        }
        if (enc !== null) {
            if (end > start) {
                utftext += string.slice(start, end);
            }
            utftext += enc;
            start = end = n + 1;
        }
    }

    if (end > start) {
        utftext += string.slice(start, stringl);
    }

    return utftext;
}

function base64_encode (data) {
    // Encodes string using MIME base64 algorithm
    //
    // version: 1103.1210
    // discuss at: http://phpjs.org/functions/base64_encode
    // +   original by: Tyler Akins (http://rumkin.com)
    // +   improved by: Bayron Guevara
    // +   improved by: Thunder.m
    // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
    // +   bugfixed by: Pellentesque Malesuada
    // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
    // -    depends on: utf8_encode
    // *     example 1: base64_encode('Kevin van Zonneveld');
    // *     returns 1: 'S2V2aW4gdmFuIFpvbm5ldmVsZA=='
    // mozilla has this native
    // - but breaks in 2.0.0.12!
    //if (typeof this.window['atob'] == 'function') {
    //    return atob(data);
    //}
    var b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    var o1, o2, o3, h1, h2, h3, h4, bits, i = 0,
        ac = 0,
        enc = "",
        tmp_arr = [];

    if (!data) {
        return data;
    }

    data = this.utf8_encode(data + '');

    do { // pack three octets into four hexets
        o1 = data.charCodeAt(i++);
        o2 = data.charCodeAt(i++);
        o3 = data.charCodeAt(i++);

        bits = o1 << 16 | o2 << 8 | o3;

        h1 = bits >> 18 & 0x3f;
        h2 = bits >> 12 & 0x3f;
        h3 = bits >> 6 & 0x3f;
        h4 = bits & 0x3f;

        // use hexets to index into b64, and append result to encoded string
        tmp_arr[ac++] = b64.charAt(h1) + b64.charAt(h2) + b64.charAt(h3) + b64.charAt(h4);
    } while (i < data.length);

    enc = tmp_arr.join('');

    switch (data.length % 3) {
    case 1:
        enc = enc.slice(0, -2) + '==';
        break;
    case 2:
        enc = enc.slice(0, -1) + '=';
        break;
    }

    return enc;
}

Ext.data.Types.C3ARRAY = {
    convert: function(v, data) {

        var fieldName = ''

        if(this.mapping){
            fieldName = this.mapping
        }else{
            fieldName = this.name
        }

        var result = new Array()

        var nodes = data.childNodes

        for(var i=0; i<nodes.length; i++){

            var currNode = nodes[i]

            if(currNode.nodeName == fieldName){
                if(currNode.childNodes.length < 2){
                    //array of strings
                    result.push(currNode.textContent)
                }else{
                    var obj = new Object()

                    for(var j=0; j<currNode.childNodes.length; j++){
                        var childNodeName = currNode.childNodes[j].nodeName
                        var childNodeValue = currNode.childNodes[j].textContent
                        obj[childNodeName] = childNodeValue
                    }
                    result.push(obj)
                }
            }
        }

        return result;
    },

    sortType: function(v){
        //return v[0];
        return '';
    },

    type: 'C3Array'
};

Ext.data.Types.C3PLAIN = {
    convert: function(v, data) {
        return data.textContent;
    },

    sortType: function(v){
        return v;
    },

    type: 'C3Plain'
};

Ext.data.WSStore = Ext.extend(Ext.data.Store, {

    constructor: function(config){
        Ext.data.WSStore.superclass.constructor.call(this, config);
    },

    execute : function(action, rs, options, /* private */ batch) {
        // blow up if action not Ext.data.CREATE, READ, UPDATE, DESTROY
        if (!Ext.data.Api.isAction(action)) {
            throw new Ext.data.Api.Error('execute', action);
        }
        // make sure options has a fresh, new params hash
        options = Ext.applyIf(options||{}, {
            params: {}
        });
        if(batch !== undefined){
            this.addToBatch(batch);
        }
        // have to separate before-events since load has a different signature than create,destroy and save events since load does not
        // include the rs (record resultset) parameter.  Capture return values from the beforeaction into doRequest flag.
        var doRequest = true;

        if (action === 'read') {
            doRequest = this.fireEvent('beforeload', this, options);
            Ext.applyIf(options.params, this.baseParams);

            var xmlBody = createXmlForMethod(this.baseParams.methodName, this.baseParams.methodParams)

            options.params.xmlData = xmlBody;
        }
        else {
            // if Writer is configured as listful, force single-record rs to be [{}] instead of {}
            // TODO Move listful rendering into DataWriter where the @cfg is defined.  Should be easy now.
            if (this.writer.listful === true && this.restful !== true) {
                rs = (Ext.isArray(rs)) ? rs : [rs];
            }
            // if rs has just a single record, shift it off so that Writer writes data as '{}' rather than '[{}]'
            else if (Ext.isArray(rs) && rs.length == 1) {
                rs = rs.shift();
            }
            // Write the action to options.params
            if ((doRequest = this.fireEvent('beforewrite', this, action, rs, options)) !== false) {
                this.writer.apply(options.params, this.baseParams, action, rs);
            }
        }
        if (doRequest !== false) {
            // Send request to proxy.
            if (this.writer && this.proxy.url && !this.proxy.restful && !Ext.data.Api.hasUniqueUrl(this.proxy, action)) {
                options.params.xaction = action;    // <-- really old, probaby unecessary.
            }
            // Note:  Up until this point we've been dealing with 'action' as a key from Ext.data.Api.actions.
            // We'll flip it now and send the value into DataProxy#request, since it's the value which maps to
            // the user's configured DataProxy#api
            // TODO Refactor all Proxies to accept an instance of Ext.data.Request (not yet defined) instead of this looooooong list
            // of params.  This method is an artifact from Ext2.
            this.proxy.request(Ext.data.Api.actions[action], rs, options.params, this.reader, this.createCallback(action, rs, batch), this, options);
        }
        return doRequest;
    }
});

function setLoginAndPassword(login, password){
    authHeader = 'Basic ' + base64_encode(login + ':' + password)
}

function getDefaultHeaders(){
    return {
        'Content-Type': 'text/xml;charset="utf-8"',
        'Authorization': authHeader
    }
}

function createDefaultConnection(){
    return new Ext.data.Connection({
        url: '/ws/management',
        method: 'POST',
        defaultHeaders: getDefaultHeaders()
    })
}

function createXmlForMethod(methodName, parameters){
    var requestBody = '<?xml version="1.0" ?><S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/"><S:Body>' +
		'<ns2:' + methodName + ' xmlns:ns2="remote.c3.aphreet.org">'

    if(parameters){
        for(var i=0; i<parameters.length; i++){
            requestBody = requestBody + '<arg' + i + '>' + parameters[i] + '</arg' + i + '>'
        }
    }

    requestBody = requestBody + '</ns2:' + methodName + '></S:Body></S:Envelope>';

    return requestBody;
}

function ManagementCreateStorage(type, path, success, failure){

    var requestBody = createXmlForMethod("createStorage", [type, path])

    createDefaultConnection().request({
        xmlData:requestBody,
        success:success,
        failure:failure
    })
}

var _connectionProxy = null;

function getConnectionProxy(){
    if(_connectionProxy == null){
        _connectionProxy = new Ext.data.HttpProxy(createDefaultConnection());
    }

    return _connectionProxy;
}
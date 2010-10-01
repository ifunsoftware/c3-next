Ext.data.Types.C3ARRAY = {
    convert: function(v, data) {
        return ""
    },

    sortType: function(v){
        return v[0];
    },

    type: 'C3Array'
};



var PlatformProperty = Ext.data.Record.create([
    {name: 'key'},
    {name: 'value'}
]);

var Storage = Ext.data.Record.create([
    {name: 'stId', mapping:'id'},
    {name: 'storageType'},
    {name: 'mode'},
    {name: 'count', type:Ext.data.Types.INT},
    {name: 'path'},
   // {name: 'ids', type:types.ARRAY}
]);
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
    {name: 'ids', type:Ext.data.Types.C3ARRAY},
    {name: 'stIndexes', mapping:'indexes', type:Ext.data.Types.C3ARRAY}
]);

var StorageType = Ext.data.Record.create([
    {name: 'name', type:Ext.data.Types.C3PLAIN}
]);
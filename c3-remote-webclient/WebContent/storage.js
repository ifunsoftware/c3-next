var storageStore = new Ext.data.WSStore({
    reader: new Ext.data.XmlReader(
        {
            record: "return",
        },
        Storage
    ),
    writer: new Ext.data.XmlWriter({
    }),
    proxy: getConnectionProxy(),
    baseParams:{
        methodName:'listStorages'
    },
    autoLoad:true,
   /* listeners:{
        load:function(store, records, options){
            alert(records);
        }
    }*/
});

var storageTypesStore = new Ext.data.WSStore({
    reader: new Ext.data.XmlReader(
        {
            record: "return",
        },
        StorageType
    ),
    writer: new Ext.data.XmlWriter({
    }),
    proxy: getConnectionProxy(),
    baseParams:{
        methodName:'listStorageTypes'
    },
    autoLoad:false,
   /* listeners:{
        load:function(store, records, options){
            alert(records);
        }
    }*/
});

var volumesStore = new Ext.data.WSStore({
    reader: new Ext.data.XmlReader(
        {
            record: "return",
        },
        Volume
    ),
    writer: new Ext.data.XmlWriter({
    }),
    proxy: getConnectionProxy(),
    baseParams:{
        methodName:'volumes'
    },
    autoLoad:true,
   /* listeners:{
        load:function(store, records, options){
            alert(records);
        }
    }*/
});


var volumesColumnModel = new Ext.grid.ColumnModel({
    defaults: {
        sortable: true
    },
    columns: [
        {
            id: 'path',
            header: 'Mount point',
            dataIndex: 'path',
            width: 130
        },
        {
            header: 'Size',
            dataIndex: 'size',
            width: 130,
            renderer: Ext.util.Format.fileSize
        },
        {
            header: 'Available',
            dataIndex: 'available',
            width:100,
            renderer: Ext.util.Format.fileSize
        },
        {
            header: 'Free',
            dataIndex: 'free',
            width:100,
            renderer: Ext.util.Format.fileSize
        },
        {
            header: 'Storages',
            dataIndex: 'storages',
            width:100
        }
    ]
});


var storageColumnModel = new Ext.grid.ColumnModel({
        defaults: {
            sortable: true
        },
        columns: [
            {
                id: 'stId',
                header: 'ID',
                dataIndex: 'stId',
                width: 80
            },
            {
                header: 'Type',
                dataIndex: 'storageType',
                width: 130
            },
            {
                header: 'Mode',
                dataIndex: 'mode',
                width:100
            },
            {
                header: 'Res count',
                dataIndex: 'count',
                width:100
            },
            {
                header: 'Path',
                dataIndex: 'path',
                width:240
            }
        ]
    });


    // create the Grid
    var storageGrid = new Ext.grid.EditorGridPanel({
        store: storageStore,
        title:'Storages',
        cm:storageColumnModel,
        listeners: {
            rowdblclick: function(obj, index, e){

                var row = storageStore.getAt(index)

                var win = new Ext.Window({
                    title:'Storage ',// + row.data.stId,
                    renderTo:Ext.getBody(),
                    width:350,
                    height:220,
                    border:false,
                    layout:'table',
                    layoutConfig:{
                        columns:2
                    },
                    items:[
                        {html:'Id'},
                        {html:row.data.stId},
                        {html:'Type'},
                        {html:row.data.storageType},
                        {html:'Path'},
                        {html:row.data.path},
                        {html:'Count'},
                        {html:row.data.count},
                        {html:'Mode'},
                        {html:'RW'}
                    ]

                })

                win.show();
            }
        },
        tbar: [{
                text: 'Refresh',
                handler : function(){
                    storageStore.load({})
                }
            },
            {
                text: 'Add',
                handler : function(){

                    var win = new Ext.Window({
                        title:'Add Storage',
                        renderTo:Ext.getBody(),
                        width:420,
                        height:220,
                        border:false,
                        layout:'fit',
                        items:[{
                            xtype:'form',
                            labelWidth:60,
                            frame:true,
                            items:[
                                {
                                    id:'form_storage_type',
                                    fieldLabel:'Type',
                                    xtype:'combo',
                                    allowBlank : false,
                                    valueField:'name',
                                    displayField:'name',
                                    store: storageTypesStore,
                                    triggerAction: 'all',
                                    editable:false
                                },
                                {
                                    id: 'form_storage_path',
                                    fieldLabel:'Path',
                                    xtype:'textfield',
                                    anchor:'-18',
                                    allowBlank : false,
                                }
                            ],
                            buttons:[
                                {
                                    text:'Send',
                                    handler:function(b, event){
                                        var storageType = Ext.getCmp('form_storage_type').getValue()
                                        var storagePath = Ext.getCmp('form_storage_path').getValue()

                                        if(storageType && storagePath){

                                            ManagementCreateStorage(storageType, storagePath,
                                                function(resp, opts){
                                                    storageStore.load();
                                                    win.close();
                                                },
                                                function(resp, opts){
                                                    alert("Error!")
                                                }
                                            )
                                        }
                                    }
                                },
                                {
                                    text:'Cancel',
                                    handler:function(b, event){
                                        win.close();
                                    }
                                }
                            ]
                        }]
                    });
                    win.show();
                }
            }]
    });

    var volumesGrid = new Ext.grid.EditorGridPanel({
        store: volumesStore,
        title:'Volumes',
        cm:volumesColumnModel,
    });

function createStorageTab(){
    return new Ext.Panel({
        title: 'Storage',
        layout:'accordion',
        defaults:{layout:'fit', border:true},
        layoutConfig:{animate:true},
        items: [
            storageGrid,
            {
                title:'Content mappings',
                html: 'empty panel'
            },
            volumesGrid
        ]

    });
}
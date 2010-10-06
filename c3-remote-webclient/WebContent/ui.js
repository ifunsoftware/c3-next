
var proxy = new Ext.data.HttpProxy(createDefaultConnection());

var platformPropertiesStore = new Ext.data.WSStore({
    reader: new Ext.data.XmlReader(
        {
            record: "return",
        },
        PlatformProperty
    ),
    writer: new Ext.data.XmlWriter({
        tpl:'<?xml version="1.0" ?><S:Envelope xmlns:S="http://schemas.xmlsoap.org/soap/envelope/"><S:Body>' +
            '<ns2:setPlatformProperty xmlns:ns2="remote.c3.aphreet.org">' +
            '<tpl for="records">' +
            '<tpl for=".">' +
            '<arg{# - 1}>{value}</arg{# - 1}>' +
            '</tpl>' +
            '</tpl>' +
            '</ns2:setPlatformProperty></S:Body></S:Envelope>'
    }),
    proxy: proxy,
    baseParams:{
        methodName:'platformProperties'
    },
    autoLoad:true,
    autoSave:false,
    batch:false
});

var storageStore = new Ext.data.WSStore({
    reader: new Ext.data.XmlReader(
        {
            record: "return",
        },
        Storage
    ),
    writer: new Ext.data.XmlWriter({
    }),
    proxy: proxy,
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
    proxy: proxy,
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
 
Ext.onReady(function(){

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

    var accordion = new Ext.Panel({
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
            {
                title:'Volumes',
                html: 'empty panel'
            }
        ]

    });


    var fm = Ext.form;

    var cm = new Ext.grid.ColumnModel({
        defaults: {
            sortable: true
        },
        columns: [
            {
                id: 'prop-key',
                header: 'Key',
                dataIndex: 'key',
                width: 220
            }, {
                header: 'Value',
                dataIndex: 'value',
                width: 220,
               // use shorthand alias defined above
                editor: new fm.TextField({
                    allowBlank: false
                })
            }
        ]
    });

    // create the editor grid
    var grid = new Ext.grid.EditorGridPanel({
        title: 'Properties',
        store: platformPropertiesStore,
        cm: cm,
        frame: true,
        clicksToEdit: 2,
        tbar: [{
                text: 'Reload',
                handler : function(){
                    platformPropertiesStore.load({})
                }
            },
            {
                text: 'Save',
                handler : function(){
                    Ext.Msg.confirm("C3 Properties", "Properties will be written. Continue?", function(button){
                        if(button == 'yes'){
                            platformPropertiesStore.save();
                            platformPropertiesStore.commitChanges();
                        }
                    });
                }
            }]
    });

    var tabs = new Ext.TabPanel({
        region:'center',
        activeTab: 0,
        frame:true,
        items:[
            grid,
            accordion
        ]
    });

    var viewport = new Ext.Viewport({
        layout:'border',
        items:[
            tabs,
        ]
    });
});
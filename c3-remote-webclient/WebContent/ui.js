
var proxy = new Ext.data.HttpProxy(new Ext.data.Connection({
    url: '/c3-remote/ws/management',
    method: 'POST',
    defaultHeaders: {
        'Content-Type': 'text/xml;charset="utf-8"',
        'Authorization': 'Basic YWRtaW46cGFzc3dvcmQ='
    }
}));
    // create the data store
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
        methodName:'platformProperties',
        methodParams:[]
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
        methodName:'listStorages',
        methodParams:[]
    },
    autoLoad:true,
   /* listeners:{
        load: function(store, records, options){
            alert(records);
        }
    }*/
});
 
Ext.onReady(function(){

    var storageColumnModel = new Ext.grid.ColumnModel({
        defaults: {
            sortable: true // columns are not sortable by default
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
      //  frame: true,
        listeners: {
            rowdblclick: function(obj, index, e){
                alert(storageStore.getAt(index).stId);  
            }
        },
        //height:300,
        autoDestroy:false
    });

            var item2 = new Ext.Panel({
                title: 'Content mappings',
                html: 'empty panel',
            });

            var item3 = new Ext.Panel({
                title: 'Volumes',
                html: 'empty panel',
            });

            var accordion = new Ext.Panel({
                title: 'Storage',
                layout:'accordion',
                defaults:{layout:'fit'},
                items: [storageGrid, item2, item3],
                layoutConfig:{animate:true}
            });




    // shorthand alias
    var fm = Ext.form;

    var cm = new Ext.grid.ColumnModel({
        // specify any defaults for each column
        defaults: {
            sortable: true // columns are not sortable by default           
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
        //defaults:{autoHeight: true},
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
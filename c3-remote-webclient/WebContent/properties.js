var fm = Ext.form;

var platformPropertiesColumnModel = new Ext.grid.ColumnModel({
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

var platformPropertiesStore = null

function getPlatformPropertiesStore() {

    if(platformPropertiesStore == null){

        platformPropertiesStore = new Ext.data.WSStore({

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
            proxy: getConnectionProxy(),
            baseParams:{
                methodName:'platformProperties'
            },
            autoLoad:true,
            autoSave:false,
            batch:false
        })
    }
    
    return platformPropertiesStore
};


function createPropertiesTab() {
    return new Ext.grid.EditorGridPanel({
        title: 'Properties',
        store: getPlatformPropertiesStore(),
        cm: platformPropertiesColumnModel,
        frame: true,
        clicksToEdit: 2,
        tbar: [{
                text: 'Reload',
                handler : function(){
                    getPlatformPropertiesStore().load({})
                }
            },
            {
                text: 'Save',
                handler : function(){
                    Ext.Msg.confirm("C3 Properties", "Properties will be written. Continue?", function(button){
                        if(button == 'yes'){
                            getPlatformPropertiesStore().save();
                            getPlatformPropertiesStore().commitChanges();
                        }
                    });
                }
            }]
    });
}
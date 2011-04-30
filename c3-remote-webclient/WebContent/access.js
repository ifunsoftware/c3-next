var domainStore = null;

function getDomainStore(){

    if(domainStore == null){
        domainStore = new Ext.data.WSStore({
                        reader: new Ext.data.XmlReader(
                            {
                                record: "return",
                            },
                            Domain
                        ),
                        writer: new Ext.data.XmlWriter({
                        }),
                        proxy: getConnectionProxy(),
                        baseParams:{
                            methodName:'listDomains'
                        },
                        autoLoad:true,
                        autoSave:false,
                        batch:false
                      })
    }

    return domainStore;
}

var userStore = null

function getUserStore(){

    if(userStore == null){
        userStore = new Ext.data.WSStore({
                        reader: new Ext.data.XmlReader(
                            {
                                record: "return",
                            },
                            ManagementUser
                        ),
                        writer: new Ext.data.XmlWriter({
                        }),
                        proxy: getConnectionProxy(),
                        baseParams:{
                            methodName:'listUsers'
                        },
                        autoLoad:true,
                        autoSave:false,
                        batch:false
                      })
    }

    return userStore;
}



var domainModeStore = new Ext.data.SimpleStore({
    fields: ['modeId', 'mode'],
    data : [
        ['available', 'available'],
        ['readonly', 'readonly'],
        ['disabled', 'disabled']
    ]
});

var domainColumnModel = new Ext.grid.ColumnModel({
    defaults: {
        sortable: true
    },
    columns: [
        {
            header: 'Name',
            dataIndex: 'name',
            width: 80
        },
        {
            id: "domainId",
            header: 'ID',
            dataIndex: 'domainId',
            width: 240
        },
        {
            header: 'Mode',
            dataIndex: 'mode',
            width:100,
            editor:new Ext.form.ComboBox({
                allowBlank: false,
                store:domainModeStore,
                mode: 'local',
                displayField:'mode',
                editable:false,
            }),
        },
        {
            header: 'Key',
            dataIndex: 'key',
            width:200
        }
    ]
});

function createDomainGrid(){
    return new Ext.grid.EditorGridPanel({
        store: getDomainStore(),
        title:'Domains',
        cm:domainColumnModel,
//      clicksToEdit: 2
    });
}



var userColumnModel = new Ext.grid.ColumnModel({
    defaults: {
        sortable: true
    },
    columns: [
        {
            id: 'name',
            header: 'Name',
            dataIndex: 'name',
            width: 80
        },
        {
            header: 'Enabled',
            dataIndex: 'enabled',
            width: 80
        }
    ]
});

function createUserGrid(){
    return new Ext.grid.EditorGridPanel({
        store: getUserStore(),
        title:'Users',
        cm:userColumnModel,
//      clicksToEdit: 2
    });
}

function createAccessTab() {
    return new Ext.Panel({
        title: 'Access',
        layout:'accordion',
        defaults:{layout:'fit', border:true},
        layoutConfig:{animate:true},
        items: [
            createDomainGrid(),
            createUserGrid()
        ]

    });
}
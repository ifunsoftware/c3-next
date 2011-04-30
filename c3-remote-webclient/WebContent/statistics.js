var statisticsStore = null;

function getStatisticsStore(){

    if(statisticsStore == null){
        statisticsStore = new Ext.data.WSStore({
                        reader: new Ext.data.XmlReader(
                            {
                                record: "return",
                            },
                            Statistics
                        ),
                        writer: new Ext.data.XmlWriter({
                        }),
                        proxy: getConnectionProxy(),
                        baseParams:{
                            methodName:'statistics'
                        },
                        autoLoad:true,
                        autoSave:false,
                        batch:false
                      })
    }

    return statisticsStore;
}

var statisticsColumnModel = new Ext.grid.ColumnModel({
    defaults: {
        sortable: true
    },
    columns: [
        {
            id: 'key',
            header: 'Name',
            dataIndex: 'key',
            width: 250
        },
        {
            header: 'Value',
            dataIndex: 'value',
            width: 250
        }
    ]
});

function createStatisticsTab() {
    return new Ext.grid.EditorGridPanel({
        store: getStatisticsStore(),
        title:'Statistics',
        cm:statisticsColumnModel,
        tbar: [{
                text: 'Refresh',
                handler : function(){
                    getStatisticsStore().load()
                }
            }]
    })
}
Ext.onReady(function(){

    var tabs = new Ext.TabPanel({
        region:'center',
        activeTab: 0,
        frame:true,
        items:[
            createPropertiesTab(),
            createStorageTab(),
            createAccessTab(),
            createStatisticsTab()
        ]
    });

    var viewport = new Ext.Viewport({
        layout:'border',
        items:[
            tabs,
        ]
    });
});
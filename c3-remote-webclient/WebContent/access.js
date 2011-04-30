function createAccessTab() {
    return new Ext.Panel({
        title: 'Access',
        layout:'accordion',
        defaults:{layout:'fit', border:true},
        layoutConfig:{animate:true},
        items: [
            {
                title:'Application access',
                html: 'empty panel'
            },
            {
                title:'Management access',
                html: 'empty panel'
            }
        ]

    });
}
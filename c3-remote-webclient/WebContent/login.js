function showLoginWindow(callback){
 var win = new Ext.Window({
    title:'Sign in',
    renderTo:Ext.getBody(),
    width:230,
    height:130,
    border:false,
    layout:'fit',
    items:[{
        xtype:'form',
        labelWidth:60,
        frame:true,
        items:[
        {
            id:'form_login_name',
            fieldLabel:'Name',
            xtype:'textfield',
            allowBlank : false,
            triggerAction: 'all',
        },
        {
            id: 'form_login_pass',
            fieldLabel:'Password',
            xtype:'textfield',
            inputType: 'password',
            allowBlank : false,
        }],
        buttons:[
        {
            text:'Sign in',
            handler:function(b, event){
                var login = Ext.getCmp('form_login_name').getValue()
                var password = Ext.getCmp('form_login_pass').getValue()

                if(login && password){
                    win.close();
                    setLoginAndPassword(login, password)
                    callback.apply();
                }
            }
        }]
    }]
 })

 win.show()
}
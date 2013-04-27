
function dbg(txt) { if (window.console) console.log(txt); }

var Terminal = new Class({

    commandHistory: [],
    commandHistoryIndex: -1,
    inCopyPaste: false,

    supportsStorage: supports_html5_storage(),

    initialize: function(container) {
        this.terminal = container;

        this.loadCommandHistory();

        this.executeCommand('show system summary', this, function(console, response){

            if(response == null){
                console.out('Failed to establish connection to server')
            }else{
                console.out('Welcome to C3 management console');

                console.out(response);

                console.out('\nType \'help\' for a list of available commands.');
                console.out('&nbsp;');
                console.prompt()
            }
        });


        this.path = '.';

        // Hook events
        $(document).addEvent('keydown',  function(event) { this.keydown(event); }.bind(this));
        $(document).addEvent('keypress', function(event) { this.keypress(event); }.bind(this));
    },

    // Process keystrokes
    keydown: function(event) {
        //dbg('keydown> ' + event.key + '(' + event.code + ') ' + event.control + ' - ' + event.shift + ' - ' + event.alt + ' - ' + event.meta);

        var command = this.currentCommand.get('html');

        if(event.control){
            if(event.code == 86){
                $('paste-block').style.visibility = 'visible';

                $('paste-block-input').value = '';
                $('paste-block-input').focus();
                this.inCopyPaste = true
            }
        }


        if (event.control || event.alt || event.meta) return;

        if(event.key == 'esc') {
            if(this.inCopyPaste){
                this.inCopyPaste = false;
                $('paste-block').style.visibility = 'hidden';
                $('paste-block-input').value = '';
                return;
            }
        }

        if (event.key == 'enter') {
            if(!this.inCopyPaste){
                event.preventDefault();
                if(command != ''){
                    this.run();
                }else{
                    this.prompt();
                }
                return;
            }else{
                this.inCopyPaste = false;
                $('paste-block').style.visibility = 'hidden';
                this.currentCommand.set('html', command + $('paste-block-input').value);
            }
        }

        if (event.key == 'backspace') {
            event.preventDefault();
            if (command.substr(command.length-6) == '&nbsp;') {
                command = command.substr(0, command.length-6);
            } else {
                command = command.substr(0, command.length-1);
            }
            this.currentCommand.set('html', command);
            return;
        }

        if (event.code == 38) { // Up arrow
            event.preventDefault();
            dbg(this.commandHistoryIndex + ', ' + this.commandHistory.length);
            if (this.commandHistoryIndex > 0) {
                this.commandHistoryIndex--;
                this.currentCommand.set('html', this.commandHistory[this.commandHistoryIndex]);
            }
            return;
        }

        if (event.code == 40) { // Down arrow
            event.preventDefault();
            dbg(this.commandHistoryIndex + ', ' + this.commandHistory.length);
            if (this.commandHistoryIndex < this.commandHistory.length) {
                this.commandHistoryIndex++;
                this.currentCommand.set('html', this.commandHistory[this.commandHistoryIndex]);
                // This can overflow the array by 1, which will clear the command line
            }
        }

    },

    keypress: function(event) {
        //dbg('keypress> ' + event.key + '(' + event.code + ') ' + event.control + ' - ' + event.shift + ' - ' + event.alt + ' - ' + event.meta);

        if(this.inCopyPaste) return;

        if (event.control /*|| event.shift*/ || event.alt || event.meta) return;
        var command = this.currentCommand.get('html');

        if (event.key == 'space') {
            event.preventDefault();
            command += ' ';
            this.currentCommand.set('html', command);
            return;
        }

        // For all typing keys
        if (this.validkey(event.code)) {
            event.preventDefault();
            if (event.code == 46) {
                command += '.';
            } else {
                if(event.shift){
                    command += event.key.toUpperCase();
                }else{
                    command += event.key;
                }
            }
            this.currentCommand.set('html', command);
        }
    },

    validkey: function(code) {
        return  (code >= 33 && code <= 127);
    },

    // Outputs a line of text
    out: function(text) {
        var p = new Element('div');
        p.set('html', text);
        this.terminal.grab(p);
    },

    // Displays the prompt for command input
    prompt: function() {
        if (this.currentPrompt)
            this.currentPrompt.getElement('.cursor').destroy();

        this.currentPrompt = new Element('div');
        this.currentPrompt.grab(new Element('span').addClass('prompt').set('text', '[c3@' + window.location.hostname + ']$'));
        this.currentCommand = new Element('span').addClass('command');
        this.currentPrompt.grab(this.currentCommand);
        this.currentPrompt.grab(new Element('span').addClass('cursor'));
        this.terminal.grab(this.currentPrompt);
        $(window).scrollTo(0, this.currentPrompt.getPosition().y);
    },

    // Executes a command
    run: function() {
        var command = this.currentCommand.get('text');

        this.commandHistory.push(command);
        this.commandHistoryIndex = this.commandHistory.length;

        this.storeCommandHistory();

        this.executeCommand(command, this, function(context, response){

            if(response == null){
                context.out('Error: server request failed.')
            }else{
                context.out(response)
            }

            context.prompt()
        })
    },

    executeCommand: function(cliCommand, context, onComplete) {
        var request = new Request.HTML().get('/ws/cli?command=' + encodeURIComponent(cliCommand));
        request.addEvent('complete', function() {

            var response = null;

            if (request.isSuccess()) {
                response = request.response.text;
            }

            onComplete(context, response)
        }.bind(this));

        request.send();
    },

    storeCommandHistory: function() {
        if(this.supportsStorage){
            window['localStorage'].setItem("c3.command.history", JSON.encode(this.commandHistory));
        }
    },

    loadCommandHistory: function() {
        if(this.supportsStorage){
            var savedCommandHistory = window['localStorage'].getItem("c3.command.history");

            if(savedCommandHistory != null){
                this.commandHistory = JSON.decode(savedCommandHistory);
                this.commandHistoryIndex = this.commandHistory.length;
            }else{
                this.commandHistory = [];
            }

        }
    }
});

function supports_html5_storage() {
    try {
        return 'localStorage' in window && window['localStorage'] !== null;
    } catch (e) {
        return false;
    }
}

$(window).addEvent('domready', function() {
    window.terminal = new Terminal($('terminal'));
});
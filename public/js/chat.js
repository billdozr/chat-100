function loadMessages() {
    var chat_window = $("#chat_window");
    $.ajax({
        type: 'GET',
        url:'/chat/receive-messages/' + getLastMessageId(),
    	dataType: 'json',
    	success: function (json) {
    	    $.each(json, function(i, item){
    	        chat_window.html(chat_window.html() 
    	            + "<p id="+ item.id +">["+ item.create_on +"] <b>" + item.sender + "</b>: " + item.text + "</p>");
    	    });
    	    chat_window.attr({ scrollTop: chat_window.attr("scrollHeight") });
            // wait for next
            setTimeout("loadMessages("+ getLastMessageId() +")", 2000);
    	}
    });
}

function sendMessage(sender, text) {
    $.ajax({
        type: 'POST',
        data: {sender:sender.val(),text:text.val()},
        url:'/chat/add-message',
    	dataType: 'json',
    	success: function (json) {
            loadMessages(getLastMessageId());
    	}
    });
    $("#text").val('');
    $("#text").focus();
}

function getLastMessageId() { 
    var last_message_id = $("#chat_window p:last-child").attr("id");
    return last_message_id ? last_message_id : 0; 
}

$(document).ready(function() {    
    $("#send_button").click(function(){
        sendMessage($("#sender"), $("#text"));
    });
    $("#text").keypress(function(e){
        if (e.which == 13) { // enter/return key
            sendMessage($("#sender"), $("#text"));
        }
    });
    
    // on doc load
    loadMessages();
    
    $("#text").focus();
    $("#nickname").focus();
});
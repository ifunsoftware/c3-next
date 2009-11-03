function showForm(commentId) {
	var form = $('reply-block');
	form.parentNode.removeChild(form);
	$('message-' + commentId).appendChild(form);
	$('reply-form:reply-id').value = commentId;
	$('reply-form:reply-text').value= '';
	form.style.display = '';
}

function hideForm(){

	var form = $('reply-block');
	$('reply-block').style.display = 'none';	
	form.parentNode.removeChild(form);
	$('content_wrap').appendChild(form);
}

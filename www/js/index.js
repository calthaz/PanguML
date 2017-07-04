Dropzone.autoDiscover = false;
$(function(){
	/*$("div#upload-dropzone").dropzone({ 
	    url: "index.php", 
	    uploadMultiple:true, 
	    paramName:"upload", 
	    //params: {useremail:JSON.parse(window.localStorage.getItem("elefindUser")).email}, 
	});*/
	var myDropzone = new Dropzone("div#upload-dropzone", { 
	    url: "uploadHandler.php", 
	    uploadMultiple:true, 
	    paramName:"upload", 
	    //params: {useremail:JSON.parse(window.localStorage.getItem("elefindUser")).email}, 
	});

	myDropzone.on("success", function(file, response) {
    	var files = JSON.parse(response);
    	for(var k in files){
    		$('div#gallery').prepend('<div class="img-wrapper col s6 m4 l3">'+
				'<img src="../'+files[k]+'"">'+
				'<div><span class="label">no label</span>'+
				'<a class="btn-flat orange-text">删除</a></div>'+
			'</div>')
    	}
  	});

  	$("button.start-infer").click(function(){
  		$.post("uploadHandler.php", {action:"start"}).done(function(data){
  			console.log(data);
  		});
  	});
})
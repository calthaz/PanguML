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
				'<img src="'+files[k]+'"">'+
				'<div class="img-footer"><span class="label">No label</span>'+
				'<a class="teal-text infer-pic bed">识别</a>'+
				'<a class="blue-text infer-pic inception">识别</a>'+
        '<a class="purple-text infer-pic fur">识别</a>'+
				'<a class="orange-text delete-pic">删除</a></div>'+
			'</div>')
    	}
  	});

  	$("button.start-infer.bed").click(function(){
  		var $btn = $(event.target);
  		$btn.text("...");
  		$btn.css("pointer-events", "none");
  		$.post("uploadHandler.php", {startAll:"bed"}).done(function(data){
  			//console.log(data);
            try{
                data = JSON.parse(data);
                for(var x in data.results){
                	var filename = x.replace(/\\/g, "/");
                	//console.log(filename+" is "+data[x]);
                	$('img[src="'+filename+'"]').parent().find("span.label").text(data.results[x]);
                }
                $btn.text("识别");
                $btn.css("pointer-events", "initial");
                Materialize.toast(data.summary, 5000);
            }catch(e){
            	console.log(data);
            }   
  		});
  	});
  	$("button.start-infer.inception").click(function(){
  		var $btn = $(event.target);
  		$btn.text("...");
  		$btn.css("pointer-events", "none");
  		$.post("uploadHandler.php", {startAll:"inception"}).done(function(data){
  			//console.log(data);
            try{
                data = JSON.parse(data);
                for(var x in data.results){
                	var filename = x.replace(/\\/g, "/");
                	//console.log(filename+" is "+data[x]);
                	$('img[src="'+filename+'"]').parent().find("span.label").text(data.results[x]);
                }
                $btn.text("识别");
                $btn.css("pointer-events", "initial");
                Materialize.toast(data.summary, 5000);
            }catch(e){
            	console.log(data);
            }   
  		});
  	});
    $("button.start-infer.fur").click(function(){
      var $btn = $(event.target);
      $btn.text("...");
      $btn.css("pointer-events", "none");
      $.post("uploadHandler.php", {startAll:"fur"}).done(function(data){
        //console.log(data);
            try{
                data = JSON.parse(data);
                for(var x in data.results){
                  var filename = x.replace(/\\/g, "/");
                  //console.log(filename+" is "+data[x]);
                  $('img[src="'+filename+'"]').parent().find("span.label").text(data.results[x]);
                }
                $btn.text("识别");
                $btn.css("pointer-events", "initial");
                Materialize.toast(data.summary, 5000);
            }catch(e){
              console.log(data);
            }   
      });
    });

  	$("main").on("click", "a.delete-pic", function(event){
  		var filename = $(event.target).parents("div.img-wrapper").find("img").attr("src");
  		//console.log(filename);
  		$.post("uploadHandler.php", {delete:filename}).done(function(data){
  			if(data==="success"){
  			 	$('img[src="'+filename+'"]').parent().remove();
  			}else{
  				 Materialize.toast(data, 4000);
  			}
  		});
  	});

  	$("main").on("click", "a.infer-pic.bed", function(event){
  		var filename = $(event.target).parents("div.img-wrapper").find("img").attr("src");
  		//console.log(filename);
  		$.post("uploadHandler.php", {infer:filename, action:"bed"}).done(function(data){
  			try{
                data = JSON.parse(data);
                for(var x in data.results){
                	var filename = x.replace(/\\/g, "/");
                	//console.log(filename+" is "+data[x]);
                	$('img[src="'+filename+'"]').parent().find("span.label").text(data.results[x]);
                }
            }catch(e){
            	console.log(data);
            }  
  		});
  	});
  	$("main").on("click", "a.infer-pic.inception", function(event){
  		var filename = $(event.target).parents("div.img-wrapper").find("img").attr("src");
  		//console.log(filename);
  		$.post("uploadHandler.php", {infer:filename, action:"inception"}).done(function(data){
  			try{
                data = JSON.parse(data);
                for(var x in data.results){
                	var filename = x.replace(/\\/g, "/");
                	//console.log(filename+" is "+data[x]);
                	$('img[src="'+filename+'"]').parent().find("span.label").text(data.results[x]);
                }
            }catch(e){
            	console.log(data);
            }  
  		});
  	});
    $("main").on("click", "a.infer-pic.fur", function(event){
      var filename = $(event.target).parents("div.img-wrapper").find("img").attr("src");
      //console.log(filename);
      $.post("uploadHandler.php", {infer:filename, action:"fur"}).done(function(data){
        try{
                data = JSON.parse(data);
                for(var x in data.results){
                  var filename = x.replace(/\\/g, "/");
                  //console.log(filename+" is "+data[x]);
                  $('img[src="'+filename+'"]').parent().find("span.label").text(data.results[x]);
                }
            }catch(e){
              console.log(data);
            }  
      });
    });
})
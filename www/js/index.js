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
        '<a class="brown-text infer-pic style">识别</a>'+
				'<a class="orange-text delete-pic">删除</a></div>'+
			'</div>')
    	}
  	});

    function inferAll(type) {
      var $btn = $(event.target);
      $btn.text("...");
      $btn.css("pointer-events", "none");
      $.post("inferManager.php", {startAll:type}).done(function(data){
            try{
                data = JSON.parse(data);
                var summary = "";
                for (var i = data.inferData.length - 1; i >= 0; i--) {
                  var output = JSON.parse(data.inferData[i]); 
                  for(var x in output.results){
                    var filename = "upload-files/"+x.replace(/\\/g, "/");
                    //console.log(filename+" is "+data[x]);
                    $('img[src="'+filename+'"]').parent().find("span.label").text(output.results[x]);

                  }
                  summary=summary+output.summary+"<br/>";
                }
                Materialize.toast(summary, 5000);
            }catch(e){
              console.log(data);
            }   
      }).always(function() {
          $btn.text("识别");
            $btn.css("pointer-events", "initial");
      });
    };

  	$("button.start-infer.bed").click(function(){
  		inferAll("bed");
  	});

  	$("button.start-infer.inception").click(function(){
  		inferAll("inception");
  	});

    $("button.start-infer.fur").click(function(){
      inferAll("fur");
    });

    $("button.start-infer.style").click(function(){
      inferAll("style");
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

    function inferPic(event, type) {
      var filename = $(event.target).parents("div.img-wrapper").find("img").attr("src");
      //console.log(filename);
      $.post("inferManager.php", {infer:filename, action:type}).done(function(data){
        try{
                data = JSON.parse(data);
                var output = JSON.parse(data.inferData); 
                for(var x in output.results){
                  var filename = "upload-files/"+x.replace(/\\/g, "/");
                  //console.log(filename+" is "+data[x]);
                  $('img[src="'+filename+'"]').parent().find("span.label").text(output.results[x]);
                }
            }catch(e){
              console.log(data);
            }  
      });
    };

  	$("main").on("click", "a.infer-pic.bed", function(event){
  		inferPic(event, "bed");
  	});

  	$("main").on("click", "a.infer-pic.inception", function(event){
  		inferPic(event, "inception");
  	});
    $("main").on("click", "a.infer-pic.fur", function(event){
      inferPic(event, "fur");
    });
    $("main").on("click", "a.infer-pic.style", function(event){
      inferPic(event, "style");
    });
})
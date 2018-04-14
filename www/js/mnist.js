Dropzone.autoDiscover = false;
var current = "";
var clips = [];
var clipFiles = [];
var ratio = 1; 
var canvasWidth = 1;
$(function(){
	/*$("div#upload-dropzone").dropzone({ 
	    url: "index.php", 
	    uploadMultiple:true, 
	    paramName:"upload", 
	    //params: {useremail:JSON.parse(window.localStorage.getItem("elefindUser")).email}, 
	});*/
	var myDropzone = new Dropzone("div#upload-dropzone", { 
	    url: "../uploadHandler.php", 
	    uploadMultiple:false, 
	    paramName:"uploadForMNIST", 
	    //params: {useremail:JSON.parse(window.localStorage.getItem("elefindUser")).email}, 
	});

	myDropzone.on("success", function(file, response) {
    	var file = JSON.parse(response);
    	console.log(file);
    	var img = new Image();   // Create new img element
    	current = '../'+file;
    	clips = [];
		clipFiles = [];
		ratio = 1; 
		canvasWidth = 1;
		img.src = current;
		var canvas = document.getElementById('canvas')
  		var ctx = document.getElementById('canvas').getContext('2d');

  		img.onload = function(){
  			/// set size proportional to image
  			canvasWidth = canvas.width;
  			ratio = canvas.width / img.width;
    		canvas.height = canvas.width * (img.height / img.width);
		    /// step 3, resize to final size
		    ctx.drawImage(img, 0, 0, img.width, img.height, 0, 0, img.width*ratio, img.height*ratio);
		    console.log(ratio,canvas.height,canvas.width,img.height,img.width)
  		};
  	});

	function drawRect(ctx, vertices, r){
		var rect = [Math.min(vertices[0],vertices[1])*r, Math.min(vertices[2],vertices[3])*r,
			Math.abs(vertices[0]-vertices[1])*r, Math.abs(vertices[2]-vertices[3])*r];
		ctx.strokeRect(Math.min(vertices[0],vertices[1])*r, Math.min(vertices[2],vertices[3])*r,
			Math.abs(vertices[0]-vertices[1])*r, Math.abs(vertices[2]-vertices[3])*r);
		clips.push(rect);
	}
	function writeNumber(ctx, clip, val){
		ctx.strokeText(val, clip[0], clip[1]);
	}

  	$("button#start").click(function(){

  		var $btn = $(event.target);
		$btn.text("...");
		$btn.css("pointer-events", "none");
		if(current.length==0){
  			Materialize.toast("Please upload a photo of hand-written digits with white background", 5000);
  		}else if(clips.length==0){
			$.post("recognize_digits.cgi", {requestPath:current}).done(function(data){
			    try{
			        data = JSON.parse(data);
			        console.log(typeof(data));
			        var canvas = document.getElementById('canvas');
	  				var ctx = document.getElementById('canvas').getContext('2d');
	  				ctx.strokeStyle = '#33fddb';
  					ctx.font = '12px serif';
	  				var r = canvasWidth/data.size[1];
			        for(var i in data.clips){
			        	console.log(data.clips[i]);
			        	var box = data.clips[i][1];
			        	clipFiles.push(data.clips[i][0]);
			        	console.log(box);
			        	drawRect(ctx, box, r);
			        }
			    }catch(e){
			        console.log(data);
			    }   
			});
  		}else if(clipFiles.length!=0){//../dummy.cgi
  			$.post("../tensorflow/infer_MNIST.cgi", {digitFilePaths:clipFiles}).done(function(data){
			    try{
			        data = JSON.parse(data);
			        console.log(data);
			        var canvas = document.getElementById('canvas');
	  				var ctx = document.getElementById('canvas').getContext('2d');
	  				ctx.strokeStyle = '#33fddb';
  					ctx.font = '12px serif';
			        for(var i in data){
			        	//console.log(data[i]);
			        	var c = clips[i];
			        	writeNumber(ctx, c, data[i]);
			        }
			    }catch(e){
			        console.log(data);
			    }   
			});
  		}
		$btn.text("识别");
		$btn.css("pointer-events", "initial");
  	});
});
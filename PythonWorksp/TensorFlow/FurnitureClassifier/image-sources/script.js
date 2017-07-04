var links = $('a');

var count=0;

var size = 40; 

function nextSet(){
	var end = count+size;
	while(count<end && count<links.length){
		links[count].click();
		$(links[count]).css("color","green");
		count++;
	}  
	if(count>=links.length){
		return;
	}else{
		setTimeout("nextSet()", 10000);
	}
}



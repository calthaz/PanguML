<?php
$labelSep= '|||';
#worker code
if(isset($_POST['method'])&&isset($_FILES['upload'])){
	$paramName = 'upload';
	$tmp_name_string = ' ';
	$name_map = array();
	foreach ($_FILES[$paramName]["error"] as $key => $error) {
		//echo "in foreach"; 
    	if ($error == UPLOAD_ERR_OK) {
    		//echo "error ok"; 
        	$tmp_name_string .= $_FILES[$paramName]["tmp_name"][$key]." ";
	        //$name = basename($_FILES[$paramName]["name"][$key]);
	        $name_map[$_FILES[$paramName]["tmp_name"][$key]]= $_FILES[$paramName]["name"][$key];
    	}
	}

	if($_POST['method'] =='inception'){
		$commandStr = 'java -jar tensorflow/inception5h.jar '.$tmp_name_string.' ';
	}elseif($_POST['method']=='bed'){
		$commandStr = 'java -jar tensorflow/bedClassifier.jar '.$tmp_name_string.' ';
	}else{
		$commandStr = 'java -jar tensorflow/materialClassifier.jar '.$tmp_name_string.' ';
	}

	$output="";
	exec($commandStr, $output);
	$summary = $output[count($output)-2];
	$resultFile = $output[count($output)-1];
	$results = file($resultFile);
	//print_r($output);
	$returnArray = array();
	$map=array();
	for ($i=0; $i < count($results) ; $i++) { 
		# code...
		list($key, $label, $score) = explode($labelSep, trim($results[$i]));
		$origPath = $name_map[$key];
		$map[$origPath] = $label."(".$score.")"; 
		#$map[$key."-score"] = $score;
	}
	$returnArray["results"] = $map;
	$returnArray["summary"] = $summary;
	echo json_encode($returnArray);
	unlink($resultFile);
}

?>
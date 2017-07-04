<?php
	require "config.inc.php";

	
	if(isset($_FILES['upload'])){//? 
		$paramName = 'upload';
		$returnArray = array();
		foreach ($_FILES[$paramName]["error"] as $key => $error) {
			//echo "in foreach"; 
	    	if ($error == UPLOAD_ERR_OK) {
	    		//echo "error ok"; 
	        	$tmp_name = $_FILES[$paramName]["tmp_name"][$key];
		        $name = basename($_FILES[$paramName]["name"][$key]);

		        $imageFileType = strtolower(pathinfo($name,PATHINFO_EXTENSION)) ;

		        // Check file size
				if ($_FILES[$paramName]["size"][$key] < 10 * 1024 * 1024) { //unit: bytes
					//echo "small enough"; 
				    // Allow certain file formats
					if($imageFileType == "jpg" || $imageFileType == "png" || $imageFileType == "jpeg"
					|| $imageFileType == "gif" ) {
						$title = $name;    
			        	$filePrefix = rand(1000,9999);		        
				        $filename = $filePrefix."_".time().$key.".".$imageFileType;

					    move_uploaded_file($tmp_name, $filesDir."/".$filename);
					    
					    array_push($returnArray, $filesDir.'/'.$filename);
					}
				}
				
	    	}
		}
		echo json_encode($returnArray); 
	}elseif(isset($_POST['action'])){
 	
 		$commandStr = 'java -jar tensorflow/FurnishingClassifier.jar '.$filesDir.' ';
 		$output="";
 		exec($commandStr, $output);
 		$resultFile = $output[count($output)-1];
 		$results = file($resultFile);
 		$map=array();
 		for ($i=0; $i < count($results) ; $i++) { 
 			# code...
 			list($key, $label) = explode($labelSep, trim($results[$i]));
 			$map[$key] = $label; 
 		}

 		echo json_encode($map);

	}elseif(isset($_POST['infer'])){
		//echo $_POST['delete'];
		if(stripos($_POST['infer'], $filesDir)==0){
			$commandStr = 'java -jar tensorflow/FurnishingClassifier.jar '.$_POST['infer'].' ';
	 		$output="";
	 		exec($commandStr, $output);
	 		$resultFile = $output[count($output)-1];
	 		$results = file($resultFile);
	 		$map=array();
	 		for ($i=0; $i < count($results) ; $i++) { 
	 			# code...
	 			list($key, $label) = explode($labelSep, trim($results[$i]));
	 			$map[$key] = $label; 
	 		}

	 		echo json_encode($map);
		}else{
			echo "illegal action";
		}
 		
	}elseif(isset($_POST['delete'])){
		//echo $_POST['delete'];
		if(stripos($_POST['delete'], $filesDir)==0){
			$status = unlink($_POST['delete']);
			if($status) echo "success";
		}else{
			echo "illegal action";
		}
 		
	}
	
?>
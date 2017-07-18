<?php
	require "config.inc.php";

	$cFiles = array();
	$method = "";
	if(isset($_POST['startAll'])){
		//var_dump($_POST);
		$method = $_POST['startAll'];
		$returnArray = array();
		$handle = opendir($filesDir);
		$file="";
		
		if ( $handle ){
			while ( ( $file = readdir ( $handle ) ) !== false ){
				$type  = pathinfo($file, PATHINFO_EXTENSION);//
				if ( $file != '.' && $file != '..' && ($type == "jpg" || $type == "png" || $type == "jpeg" || $type == "gif")){
					if($type == "jpg" || $type == "jpeg"){
						$mimetype = 'image/jpeg';
					}
					if($type == "png"){
						$mimetype = 'image/png';
					}
					if($type == "gif"){
						$mimetype = 'image/gif';
					}
					if (function_exists('curl_file_create')) { // php 5.5+
						$cFile = curl_file_create(realpath($filesDir."/".$file), $mimetype);
					} else { //
						$cFile = '@' . realpath($filesDir."/".$file);
					}
					$cFiles[]=$cFile;
				}
			}
		} 
	}elseif(isset($_POST['infer'])&&isset($_POST['action'])){
		//var_dump($_POST);
		$file = $_POST['infer'];
		$method = $_POST['action'];
		$type  = pathinfo($file, PATHINFO_EXTENSION);
		//echo realpath($file);
		if($type == "jpg" || $type == "jpeg"){
			$mimetype = 'image/jpeg';
		}
		if($type == "png"){
			$mimetype = 'image/png';
		}
		if($type == "gif"){
			$mimetype = 'image/gif';
		}
		if (function_exists('curl_file_create')) { // php 5.5+
			$cFile = curl_file_create(realpath($file), $mimetype);
		} else { //
			$cFile = '@' . realpath($file);
		}
		$cFiles[]=$cFile;
	}
	if($cFiles!=null&&$method!=""){
		$returnArray['method']=$method;
		$params=['dispatch'=>count($cFiles)];
		$defaults = array(
				CURLOPT_URL => $java_supervisor,
				CURLOPT_POST => true,
				CURLOPT_POSTFIELDS => http_build_query($params),
		);
		$ch = curl_init();
		curl_setopt_array($ch, $defaults);
		curl_setopt($ch,CURLOPT_RETURNTRANSFER,true);
		$ip=curl_exec($ch);
		$ip=trim($ip);
		//echo $ip;
		//echo '<br>ip is above<br>';
		if(curl_error($ch)){
			$returnArray['ip'] = curl_error($ch);
			echo json_encode($returnArray);
			return;
		}else{
			$returnArray['ip'] =$ip;
		}
		
		$target_url = $ip;//"localhost/post-dummy.php";//
		$post = array('method' => $method);
		foreach ($cFiles as $key => $file) {
			$post['upload['.$key.']']=$file;
		}
		$ch = curl_init();
		curl_setopt($ch, CURLOPT_URL,$target_url);
		curl_setopt($ch, CURLOPT_POST,1);
		curl_setopt($ch, CURLOPT_POSTFIELDS, $post);
		curl_setopt($ch,CURLOPT_RETURNTRANSFER,true);
		$result=curl_exec ($ch);
		$info = curl_getinfo($ch);
		//echo $result;
		//print_r($post);
		if(curl_error($ch)||$info['http_code']!="200"){
			$returnArray['uploadStatus'] = curl_error($ch)."|http_code: ".$info['http_code'];
		}else{
			$returnArray['uploadStatus'] ="success";
			//parse result
			$returnArray['inferData']=$result;
		}
		
		//print_r($info);
		curl_close ($ch);
		//echo $result;
		//echo '<br>file upload is above<br>';
		
		$params=['finish-count'=>count($cFiles), 'finish-ip'=> $ip];
		$defaults = array(
				CURLOPT_URL => $java_supervisor,
				CURLOPT_POST => true,
				CURLOPT_POSTFIELDS => http_build_query($params),
		);
		$ch = curl_init();
		curl_setopt_array($ch, $defaults);
		curl_setopt($ch,CURLOPT_RETURNTRANSFER,true);
		$result=curl_exec($ch);
		if(curl_error($ch)){
			$returnArray['finishReportStatus'] = curl_error($ch);
		}else{
			$returnArray['finishReportStatus'] ="success";//$result;
			//parse result
		}
		curl_close ($ch);
		echo json_encode($returnArray);
		//echo $result;
		//echo '<br>finish call is above<br>';
		/*
 		*/
	}
?>
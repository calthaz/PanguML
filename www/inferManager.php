<?php
	require "config.inc.php";

	$max_file_upload = (int)(ini_get('max_file_uploads'));
	$cFiles = array();
	$method = "";
	//$batch = array();
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
		/*
		$batchCount = floor(count($cFiles)/$max_file_upload);
		if($batchCount>=1){
			for($i=0;$i<$batchCount;$i++){
				$batch[]=$max_file_upload;
			}
		}
		$batch[]=count($cFiles)%$max_file_upload;
		*/
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
		//$batch[]=1;
	}
	if($cFiles!=null&&$method!=""){
		$returnArray['method']=$method;
		$returnArray['inferData']=array();
		$batches = array_chunk($cFiles, $max_file_upload);
		foreach ($batches as $key => $batch) {
			$params=['dispatch'=> count($batch)];
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
				$returnArray['ip'.$key] = curl_error($ch);
				echo json_encode($returnArray);
				return;
			}else{
				$returnArray['ip'.$key] =$ip;
			}
			curl_close ($ch);
			
			$target_url = $ip;//"localhost/post-dummy.php";//
			$post = array('method' => $method);
			foreach ($batch as $k => $file) {
				$post['upload['.$k.']']=$file;
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
				$returnArray['uploadStatus'.$key] = curl_error($ch)."|http_code: ".$info['http_code'];
			}else{
				$returnArray['uploadStatus'.$key] ="success";
				$returnArray['inferData'][]=$result;
			}
			
			//print_r($info);
			curl_close ($ch);
			//echo $result;
			//echo '<br>file upload is above<br>';
			
			
			$params=['finish-count'=>count($batch), 'finish-ip'=> $ip];
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
				$returnArray['finishReportStatus'.$key] = curl_error($ch);
			}else{
				$returnArray['finishReportStatus'.$key] ="success";//$result;
				//parse result
			}
			curl_close ($ch);
		}
		
		echo json_encode($returnArray);
		//echo $result;
		//echo '<br>finish call is above<br>';

	}
?>
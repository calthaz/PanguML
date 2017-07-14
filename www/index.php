<!DOCTYPE html>
<head>
	<meta charset="utf-8">
	<title>image classifier</title>
	<link rel="stylesheet" href="lib/materialize/css/materialize.css">
	<link rel="stylesheet" href="lib/dropzone/dropzone.css">
	<link rel="stylesheet" href="css/style.css">
</head>
<body>
<?php 	
	require "config.inc.php";
	#clean garbage in upload-files：
	$handle = opendir($filesDir);
	$file=""; 
    if ( $handle ){
        while ( ( $file = readdir ( $handle ) ) !== false ){
        	$type  = pathinfo($file, PATHINFO_EXTENSION);//
        	if ($type == "txt"){	
        		unlink($filesDir.'/'.$file);
            }
        }
    } 

	function printImgDiv($src, $label){?>
		<div class="img-wrapper col s6 m4 l3">
			<img src="<?php echo $src; ?>">
			<div class="img-footer">
				<span class="label"><?php echo $label; ?></span>
				<a class="teal-text infer-pic bed">识别</a>
				<a class="blue-text infer-pic inception">识别</a>
				<a class="purple-text infer-pic fur">识别</a>
				<a class="orange-text delete-pic">删除</a>
				
			</div>
		</div>
		<?php
	}
?>
	<header>
		<h1 id="page-title" class="teal bed">Simple Bed Classifier</h1>
		<h1 id="page-title" class="blue bed">Inception5h Classifier</h1>
		<h1 id="page-title" class="purple inception">Furnishing Classifier</h1>
	</header>
	<main>
		<div class="container">
			<div id="upload-dropzone" class="dropzone"></div>
			<!--<div class="row">
				<button class="btn right ">
			</div>-->
			
			<div class="fixed-action-btn">
				<button class="btn-floating btn-large waves-effect waves-light start-infer teal lighten-1 bed">识别</button>	
				<button class="btn-floating btn-large waves-effect waves-light start-infer blue lighten-1 inception">识别</button>
				<button class="btn-floating btn-large waves-effect waves-light start-infer purple lighten-1 fur">识别</button>				
			</div>
			<div id="gallery" class="row">
				<?php 
					$handle = opendir($filesDir);
					$file=""; 
			        if ( $handle ){
			            while ( ( $file = readdir ( $handle ) ) !== false ){
			            	$type  = pathinfo($file, PATHINFO_EXTENSION);//
			            	if ( $file != '.' && $file != '..' && ($type == "jpg" || $type == "png" || $type == "jpeg" || $type == "gif")){	
			            		printImgDiv($filesDir."/".$file, "No label");
			                }
			            }
			        } 

			    ?>
			</div>
		</div>
	</main>
	<footer class="page-footer grey lighten-2">
		<div class="container">
			<div class=" row">
				<div class="col s12 m6 teal-text">
					<p>Simple Bed Classifier改写自<a class="teal-text" href="https://github.com/tensorflow/models/tree/master/tutorials/image/cifar10" target="_blank">TensorFlow CIFAR10 Tutorial</a></p>
				</div>
				<div class="col s12 m6 blue-text">
					<p>Inception5h来自<a class="blue-text" href="https://github.com/tensorflow/tensorflow/blob/r1.2/tensorflow/java/src/main/java/org/tensorflow/examples/LabelImage.java" target="_blank">TensorFlow Java Example</a></p>
				</div>
			</div>
		</div>
	</footer>
	<script src="lib/jquery.min.js"></script>
	<script src="lib/materialize/js/materialize.js"></script>
	<script src="lib/dropzone/dropzone.js"></script>
	<script src="js/index.js"></script>

</body>


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
	function printImgDiv($src, $label){?>
		<div class="img-wrapper col s6 m4 l3">
			<img src="<?php echo $src; ?>">
			<div class="img-footer">
				<span class="label"><?php echo $label; ?></span>
				<a class="teal-text infer-pic bed">识别</a>
				<a class="blue-text infer-pic inception">识别</a>
				<a class="orange-text delete-pic">删除</a>
				
			</div>
		</div>
		<?php
	}
?>
	<header>
		<h1 id="page-title" class="teal bed">Simple Bed Classifier</h1>
		<h1 id="page-title" class="blue inception">Inception h5 Classifier</h1>
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
	<footer>

	</footer>
	<script src="lib/jquery.min.js"></script>
	<script src="lib/materialize/js/materialize.js"></script>
	<script src="lib/dropzone/dropzone.js"></script>
	<script src="js/index.js"></script>

</body>


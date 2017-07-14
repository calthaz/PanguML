<?php

$ch = curl_init("localhost/foo/tensorflow/");
// returns a cURL handle
$fp = fopen("example_homepage.txt", "w");

curl_setopt($ch, CURLOPT_FILE, $fp);
curl_setopt($ch, CURLOPT_HEADER, 0);

curl_exec($ch);
//Curl_exec function is to execute the curl session. Returns true if session is executed succesffully otherwise false.
//But if you will set option CURL_RETURNTRANSFER true then it will return output of the curl session.

if(curl_error($ch)){
	echo curl_error($ch);
}else{
	echo "Succeeded.";
}
$info = curl_getinfo($ch);
print_r($info);

curl_close($ch);
fclose($fp);

?>
<html>
<body>
<p>It is important to notice that when using curl to post form data and you use an array for CURLOPT_POSTFIELDS option, the post will be in multipart format</p>

<?php
	$params=['name'=>'John', 'surname'=>'Doe', 'age'=>36];
	$defaults = array(
	CURLOPT_URL => 'http://ec2-34-208-42-160.us-west-2.compute.amazonaws.com/post-dummy.php', 
	CURLOPT_POST => true,
	CURLOPT_POSTFIELDS => $params,
	);
	$ch = curl_init();
	curl_setopt_array($ch, ($defaults));#$options + 
	curl_setopt($ch,CURLOPT_RETURNTRANSFER,true);
	$result=curl_exec($ch);
	echo $result;

?>
<p>
This produce the following post header:

--------------------------fd1c4191862e3566
Content-Disposition: form-data; name="name"

Jhon
--------------------------fd1c4191862e3566
Content-Disposition: form-data; name="surnname"

Doe
--------------------------fd1c4191862e3566
Content-Disposition: form-data; name="age"

36
--------------------------fd1c4191862e3566--

Setting CURLOPT_POSTFIELDS as follow produce a standard post header

CURLOPT_POSTFIELDS => http_build_query($params),

Which is:
name=John&surname=Doe&age=36

This caused me 2 days of debug while interacting with a java service which was sensible to this difference, while the equivalent one in php got both format without problem.
</p>
php 
$cSession = curl_init(); 
//step2
curl_setopt($cSession,CURLOPT_URL,"http://www.baidu.com");
curl_setopt($cSession,CURLOPT_RETURNTRANSFER,true);
curl_setopt($cSession,CURLOPT_HEADER, false); 
//step3
$result=curl_exec($cSession);

echo curl_error($cSession);
//step4
curl_close($cSession);
//step5
//echo $result
<?php
//https://stackoverflow.com/questions/15200632/how-to-upload-file-using-curl-with-php
$file_name_with_full_path="D:/TensorFlowDev/www/upload-files/9109_14991642540.jpg";
$target_url="localhost/post-dummy.php";
if (function_exists('curl_file_create')) { // php 5.5+
  $cFile = curl_file_create($file_name_with_full_path);
} else { // 
  $cFile = '@' . realpath($file_name_with_full_path);
}
$post = array('extra_info' => '123456','file_contents'=> $cFile);
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL,$target_url);
curl_setopt($ch, CURLOPT_POST,1);
curl_setopt($ch, CURLOPT_POSTFIELDS, $post);
$result=curl_exec ($ch);
curl_close ($ch);
?>

</body>
</html>
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
<p style="color: red;">It is important to notice that when using curl to post form data and you use an array for CURLOPT_POSTFIELDS option, the post will be in multipart format</p>

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
This produce the following post header:<br>
<br>
--------------------------fd1c4191862e3566<br>
Content-Disposition: form-data; name="name"<br>
<br>
Jhon<br>
--------------------------fd1c4191862e3566<br>
Content-Disposition: form-data; name="surnname"<br>
<br>
Doe<br>
--------------------------fd1c4191862e3566<br>
Content-Disposition: form-data; name="age"<br>
<br>
36<br>
--------------------------fd1c4191862e3566--<br>
<br>
Setting CURLOPT_POSTFIELDS as follow produce a standard post header<br>
<br>
CURLOPT_POSTFIELDS => http_build_query($params),<br>
<br>
Which is:<br>
name=John&surname=Doe&age=36<br>
<br>
This caused me 2 days of debug while interacting with a java service which was sensible to this difference, while the equivalent one in php got both format without problem.
</p>
<pre>
php <br>
$cSession = curl_init(); <br>
//step2<br>
curl_setopt($cSession,CURLOPT_URL,"http://www.baidu.com");<br>
curl_setopt($cSession,CURLOPT_RETURNTRANSFER,true);<br>
curl_setopt($cSession,CURLOPT_HEADER, false); <br>
//step3<br>
$result=curl_exec($cSession);<br>
<br>
echo curl_error($cSession);<br>
//step4<br>
curl_close($cSession);<br>
//step5<br>
//echo $result<br>
</pre>
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
<h2>
<?php 
$defaults = array(
		CURLOPT_URL => 'localhost:8080/TestServer',
		CURLOPT_POST => true,
		CURLOPT_POSTFIELDS => ["dispatch"=>15]
);
//multipart/form-data
$ch = curl_init();
curl_setopt_array($ch, $defaults);
curl_setopt($ch,CURLOPT_RETURNTRANSFER,true);
$ip=curl_exec($ch);
echo $ip;
?>
</h2>
<h2>
<?php 
$defaults = array(
		CURLOPT_URL => 'localhost:8080/TestServer',
		CURLOPT_POST => true,
		CURLOPT_POSTFIELDS => http_build_query(["dispatch"=>15])
);
//application/x-www-form-urlencoded 
$ch = curl_init();
curl_setopt_array($ch, $defaults);
curl_setopt($ch,CURLOPT_RETURNTRANSFER,true);
$ip=curl_exec($ch);
echo $ip;
?>
</h2>
<div>
<?php 
$defaults = array(
		CURLOPT_URL => "localhost/post-dummy.php",
		CURLOPT_POST => true,
		CURLOPT_POSTFIELDS => http_build_query(["dispatch"=>15])
);

$ch = curl_init();
curl_setopt_array($ch, $defaults);
curl_setopt($ch,CURLOPT_RETURNTRANSFER,true);
$ip=curl_exec($ch);
echo $ip;
?>
</div>
<div>
<?php 
$defaults = array(
		CURLOPT_URL => "localhost/post-dummy.php",
		CURLOPT_POST => true
);
$ch = curl_init();
curl_setopt_array($ch, $defaults);
curl_setopt($ch,CURLOPT_RETURNTRANSFER,true);
$ip=curl_exec($ch);
echo $ip;
?>
</div>


</body>
</html>
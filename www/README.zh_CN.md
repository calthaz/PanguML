# www - apache服务器的根目录
这些都是TensorFlow演示页面或其他模板页面，以协助我的开发。
## index.php
index.php包含4个不同分类器的演示。
工作流程：\
*JavaScript => php => Java Servelet（coordinator）=> php => Java => TensorFlow（jni）=> Java => php => JavaScript*
工作流程相当复杂，因为1）我不知道JavaScript可以直接post到python cgi; 2）我们需要一个守护进程
作为在一组服务器之间分配工作负载的协调器。
## MNIST/index.php
MNIST / index.php包含OpenCV和TF一起工作以识别数字的演示。
这一次我使用python cgi，它们更容易调试。
## tensorflow/
此目录包含TensorFlow相关资料，如jar，checkpoints，frozen_graphs，python文件，tensorflow库。
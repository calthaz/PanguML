在PythonWorksp中，有许多文件以一些非常微妙的方式彼此不同，所以这里是这些文件的概述。
## TensorFlow related
>环境和包:
> * Python 3
> * TensorFlow 1.2 with GPU support
> * 和它依赖的包

### CIFAR模型的general部分
  
**[TensorFlow/StyleClassifier/general_cifar.py](TensorFlow/StyleClassifier/general_cifar.py)**  
和  
**[TensorFlow/FurnitureClassifier/general_cifar.py](TensorFlow/FurnitureClassifier/general_cifar.py)**  
只有
`INITIAL_LEARNING_RATE`
不同
  
**[TensorFlow/HardwareClassifier/general.py](TensorFlow/HardwareClassifier/general.py)**  
和  
**[TensorFlow/FurnitureClassifier/general_cifar.py](TensorFlow/FurnitureClassifier/general_cifar.py)**  
和  
**[TensorFlow/distributed/general.py](TensorFlow/distributed/general.py)**  
一样  
  
**[TensorFlow/visualization/general.py](TensorFlow/visualization/general.py)**  
扩展了    
**[TensorFlow/FurnitureClassifier/general_cifar.py](TensorFlow/FurnitureClassifier/general_cifar.py)**  
的图结构    

### General in Zeiler
  
**[TensorFlow/StyleClassifier/general_Zeiler.py](TensorFlow/StyleClassifier/general_Zeiler.py)**  
书写比  
**[TensorFlow/FurnitureClassifier/general_Zeiler.py](TensorFlow/FurnitureClassifier/general_Zeiler.py)**  
更清晰，它们的学习速率也不同 

### Read Image
  
**[TensorFlow/FurnitureClassifier/read_image.py](TensorFlow/FurnitureClassifier/read_image.py)**  
有很多读图片输入的尝试，还有TF原始的注释，所以比   
**[TensorFlow/StyleClassifier/read_image.py](TensorFlow/StyleClassifier/read_image.py)**  
更混乱一些  
  
**[TensorFlow/StyleClassifier/read_image.py](TensorFlow/StyleClassifier/read_image.py)**    
**[TensorFlow/HardwareClassifier/read_image.py](TensorFlow/HardwareClassifier/read_image.py)**  
支持用utf-8读取文件，不过系统是gbk所以也没什么用
然而  
**[TensorFlow/FurnitureClassifier/read_image.py](TensorFlow/FurnitureClassifier/read_image.py)**  
不支持
  
**[TensorFlow/HardwareClassifier/read_image.py](TensorFlow/HardwareClassifier/read_image.py)**  
不支持分开的训练和评估集  
而   
**[TensorFlow/FurnitureClassifier/read_image.py](TensorFlow/FurnitureClassifier/read_image.py)**    
**[TensorFlow/StyleClassifier/read_image.py](TensorFlow/StyleClassifier/read_image.py)**  
支持  
  
**[TensorFlow/HardwareClassifier/read_image.py](TensorFlow/HardwareClassifier/read_image.py)**  
中心/随机裁剪输入图像使得它们大小一致   
**[TensorFlow/FurnitureClassifier/read_image.py](TensorFlow/FurnitureClassifier/read_image.py)**    
**[TensorFlow/StyleClassifier/read_image.py](TensorFlow/StyleClassifier/read_image.py)**  
缩放图片使得大小一致  
  
**[TensorFlow/distributed/read_image.py](TensorFlow/distributed/read_image.py)**  
是最古老的read_image.py, 用的是原先的LABEL_SEP也不支持分开的eval/train数据集\
在其他方面和  
**[TensorFlow/FurnitureClassifier/read_image.py](TensorFlow/FurnitureClassifier/read_image.py)**
一样  
    
**[TensorFlow/visualization/read_image.py](TensorFlow/visualization/read_image.py)**  
延伸  
**[TensorFlow/FurnitureClassifier/read_image.py](TensorFlow/FurnitureClassifier/read_image.py)**  
来包括读取文件进行embedding演示的方法，所以它支持utf-8，但没有不同的评估和训练集支持，因为它不必

### Train
  
**[TensorFlow/FurnitureClassifier/train.py](TensorFlow/FurnitureClassifier/train.py)**  
拓展  
**[TensorFlow/imagenet/CIFAR-10/cifar10_train.py](TensorFlow/imagenet/CIFAR-10/cifar10_train.py)**  
来包括我自己的精度log  
  
**[TensorFlow/FurnitureClassifier/train.py](TensorFlow/FurnitureClassifier/train.py)**    
**[TensorFlow/HardwareClassifier/train.py](TensorFlow/HardwareClassifier/train.py)**    
**[TensorFlow/StyleClassifier/train.py](TensorFlow/StyleClassifier/train.py)**  
基本一样   
**[TensorFlow/distributed/trainer.py](TensorFlow/distributed/trainer.py)**  
也差不多但是没有精度log  

### Eval
  
**[TensorFlow/FurnitureClassifier/my_eval.py](TensorFlow/FurnitureClassifier/my_eval.py)**    
**[TensorFlow/StyleClassifier/my_eval.py](TensorFlow/StyleClassifier/my_eval.py)**    
**[TensorFlow/HardwareClassifier/my_eval.py](TensorFlow/HardwareClassifier/my_eval.py)**  
基本是一样的，注释写法有点不同  

### Save Model
  
**[TensorFlow/HardwareClassifier/save_model.py](TensorFlow/HardwareClassifier/save_model.py)**  
不会裁剪输入图像，因为Java将在将其传递给python之前将其裁剪   
因此和  
**[TensorFlow/FurnitureClassifier/save_model.py](TensorFlow/FurnitureClassifier/save_model.py)**  
差不多，和  
**[TensorFlow/StyleClassifier/save_model.py](TensorFlow/StyleClassifier/save_model.py)**  
一样  

### MNIST
  
**[TensorFlow/MNIST/MNIST_TensorBoardex_hp.py](TensorFlow/MNIST/MNIST_TensorBoardex_hp.py)**  
扩展了   
**[TensorFlow/MNIST/MNIST_TensorBoardex.py](TensorFlow/MNIST/MNIST_TensorBoardex.py)**  
支持运行不同的参数并比较算法  

和 **[TensorFlow/MNIST/mnist_deep.py](TensorFlow/MNIST/mnist_deep.py)** 相比  
**[TensorFlow/MNIST/mnist_deep_saver.py](TensorFlow/MNIST/mnist_deep_saver.py)**  
拓展了它并用summary_writer和Saver来写checkpoint和图  
还和   **[../www/tensorflow/mnist_deep_saver.py](../www/tensorflow/mnist_deep_saver.py)** 基本一样   
**[TensorFlow/MNIST/mnist_deep_builder.py](TensorFlow/MNIST/mnist_deep_builder.py)**   
用builder来保存模型    
**[TensorFlow/MNIST/mnist_deep_graph.py](TensorFlow/MNIST/mnist_deep_graph.py)**   
用TensorBoard来展示图  
**[TensorFlow/MNIST/mnist_deep_output_graph.py](TensorFlow/MNIST/mnist_deep_output_graph.py)**  
用`tf.graph_util.convert_variables_to_constants`
和`output_graph_def.SerializeToString()`保存序列化的图
  
**[TensorFlow/MNIST/mnist_deep-32.py](TensorFlow/MNIST/mnist_deep-32.py)**  
把第一个卷积层的输出从64改成32然后用builder保存
**[TensorFlow/MNIST/mnist_deep-32-output-graph.py](TensorFlow/MNIST/mnist_deep-32-output-graph.py)**  
用`tf.graph_util.convert_variables_to_constants`
和`output_graph_def.SerializeToString()`保存序列化的图
  
**[TensorFlow/MNIST/mnist_deep-512.py](TensorFlow/MNIST/mnist_deep-512.py)**  
把第一个全连接层输出从1024改成512  

我不是很确定  
**[TensorFlow/MNIST/mnist_softmax.py](TensorFlow/MNIST/mnist_softmax.py)**  
和  
**[TensorFlow/MNIST/fully_connected_feed.py](TensorFlow/MNIST/fully_connected_feed.py)**  
是干嘛的，也不确定我有没有改过它们
  
**[TensorFlow/MNIST/infer.py](TensorFlow/MNIST/infer.py)**  
imports mnist_deep_saver然后运行图来分类数字   
**[../www/tensorflow/infer_MNIST.cgi](../www/tensorflow/infer_MNIST.cgi)**  
拓展了  
**[TensorFlow/MNIST/infer.py](TensorFlow/MNIST/infer.py)**  
包括hashbang，发送请求头，获取post数据打印json结果。

## OpenCV related
>环境和包:
> * Python 2
> * OpenCV 3
> * PIL
> * 和它们的依赖。我用的是Anaconda. [参见: 管理packages.](https://conda.io/docs/using/pkgs.html)

**[objectDetector/contour_furn.py](objectDetector/contour_furn.py)**  
用 `cv2.RETR_TREE`
而  
**[objectDetector/contour_digits.py](objectDetector/contour_digits.py)**  
用的是 `cv2.RETR_EXTERNAL` 也不检查 `cv2.contourArea(box)`

**[objectDetector/recognize_digits.py](objectDetector/recognize_digits.py)**  
比   
**[objectDetector/contour_digits.py](objectDetector/contour_digits.py)**  
整洁，因为它离cgi文件又近了一步

**[../www/MNIST/recognize_digits.cgi](../www/MNIST/recognize_digits.cgi)**  
拓展了  
**[objectDetector/recognize_digits.py](objectDetector/recognize_digits.py)**  
包括hashbang，发送请求头，获取post数据打印json结果。\
它还把截取的数字补成正方形方便
**[../www/tensorflow/infer_MNIST.cgi](../www/tensorflow/infer_MNIST.cgi)**
判断
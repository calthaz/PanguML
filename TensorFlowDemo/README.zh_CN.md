# TensorFlowDemo
这是一个非常简单的演示，使用[我的java类](http://null_560_5360.oschina.io/tensorflow/index.html)和[TensorFlow的cifar10模型](https：//github.com/tensorflow/models/tree/master/tutorials/image/cifar10)。
解压training-materials里的文件，
运行train.py，再运行my_eval.py，准确率应该在66%左右。
> ENV 和 LIB：
> * Python 3
> * 具有GPU支持的TensorFlow 1.2(您可能可以运行不支持GPU的相同代码)  
> * 及其依赖项
> * Java 8
> * [TensorFlow libs for java](https://www.tensorflow.org/install/install_java)

## 文件夹结构概述
* __src - Java源文件__
	- prepare - 处理图像并生成用于训练和评估的数据集
	- classifier - 读取"graph_def"的分类器
	- eval - 帮助评估模型
* __logs - TensorFlow训练chackpoints和保存/eval事件文件__
	- train3500 - 在step = 3500停止
	- eval - my_eval日志
	- save - save_model日志
* __models - TensorFlow SavedModelBundle和graph_defs__
	- train3500 - 在step = 3500停止
	- eval - my_eval日志
	- save - save_model日志
* __python - Python代码__
	- read_image.py - 使用`slice_input_producer`读取由java生成的标签文件
	- general_cifar.py - 神经网络的结构
	- train.py - 使用"MonitoredTrainingSession"来训练模型
	- my_eval.py - 使用eval数据集来评估模型
	- save_model.py - 使用`convert_variables_to_constants`和`output_graph_def.SerializeToString()'以及`SavedModelBuilder`保存模型
	- visualize.py - 将deconv网络附加到cifar结构上，并可视化激活图。 (https://arxiv.org/pdf/1311.2901.pdf)
	- visualize_test.py - 使用给定的输入图像运行visualize.py
* __training-materials - 训练材料__
	* raw-data - 从互联网收集的图像
		- bed - 来自[ImageNet](http://image-net.org)和百度的照片
		- flowers - 照片来自[TF：Retrain Inception's Final Layer for New Categories](https://www.tensorflow.org/tutorials/image_retraining)
	* ready
		- eval - 评估数据集
		- train - 训练数据集
		- other - 压缩过的图像
	* result - eval数据集中的图像根据模型的分类被分类到相应的文件夹中
	
## 创建图像数据集
[*src/SampleProcessor.java*](src/SampleProcessor.java)
### `SampleHelper.batchEditImages`：给你想要的各种图片
我发现这个函数非常方便，因为它可以处理一个文件夹下的所有图像，而不会改变原来的图像 - 因为它只是复制这个文件夹...
```java
SampleHelper.batchEditImages("training-materials/raw-data", "training-materials", "scale-"
		, new SampleHelper.ImgProcessor() {
					public BufferedImage process(BufferedImage img) {
						BufferedImage st = null;
						if(img.getWidth()<=MAX_PIC_SIZE&&img.getHeight()<=MAX_PIC_SIZE){
							st = img;
						}else{
							Dimension des = TFUtils.scaleUniformFit(img.getWidth(), img.getHeight(), MAX_PIC_SIZE, MAX_PIC_SIZE);
							st = TFUtils.getScaledImage(img, des.width, des.height);
						}
						return st;
					}
				});
```
对于像我这样每天改变主意的人和对于比较不同的算法来说比较方便 - 如果磁盘空间不是问题的话。这对我来说非常有用。
### `SampleHelper`和`LabelGenerator`是你的朋友
`SampleHelper`处理文件。
`LabelGenerator`显然是处理标签。但更重要的是，`LabelGenerator`是Java和Python之间的桥梁：它指定标签和结果写入txt文件，并从txt文件解析的方式。 `LabelGenerator.LABEL_SEP`不仅在一行中分隔标签，而且还在一行内分隔分数，文件名，索引和任何字段。
`TFUtils`也应该是你的朋友，因为它为杂项任务提供了较低级别的解决方案。   
**有关"SampleHelper"和"LabelGenerator"的更多信息，请阅读[文档](http://null_560_5360.oschina.io/tensorflow/index.html)。**

## 建立深层神经网络
[*python/general_cifar.py*](python/general_cifar.py)

### 只需使用`tf.nn`，`tf.train`等等
只需阅读[此TF教程](https://www.tensorflow.org/tutorials/deep_cnn)

## 读数据
[*python/read_image.py*](python/read_image.py)
### input producers
有多少形式的输入，就有多少种input producer。  
这是我们在这里使用的，因为我们有两个单独的图像和标签列表，"slice_input_producer"将它们彼此关联起来。
```python
tf.train.slice_input_producer([images, labels], shuffle = True)
```
\
如果您只想阅读一个图像，可以使用
```python
filename_queue = tf.train.string_input_producer(["xxx/xxxx.jpg"])
key, value = reader.read(filename_queue)
my_img = tf.image.decode_jpeg(value)
```
如[*python/visualize_test.py*](python/visualize_test.py)  
\
所有的阅读和批处理运行在不同的线程上，所以你需要一个"Coordinator"来监督它们：
```python
#启动输入入队线程
coord = tf.train.Coordinator()
threads = tf.train.start_queue_runners(sess = sess, coord = coord)

try:
    while not coord.should_stop():
        # 运行培训步骤或其他任何操作
        sess.run(train_op)

except tf.errors.OutOfRangeError:
    print('完成训练 - 达到epoch limit限制')
finally:
    # 完成后，请求线程停止。
    coord.request_stop()

# 等待线程结束。
coord.join(threads)
sess.close()
```
**否则你会什么也看不到 ...**

这个很棒的gif解释了tf在做什么：
![文件队列动画](https://www.tensorflow.org/images/AnimatedFileQueues.gif)
有关读取数据的更多信息，请访问[TF：读取数据](https://www.tensorflow.org/programmers_guide/reading_data)。

## 训练
[*python/train.py*](python/train.py)

### `tf.train.MonitoredTrainingSession`和`tf.train.SessionRunHook`
一个`SessionRunHook`可以帮你个性化你的训练  
在我的训练期间，按照以下顺序调用以下方法：
```
begin
  while not mon_sess.should_stop():
	before_run
        actual training: mon_sess.run(train_op)
	after_run
end
```

有时你会在训练期间看到这一切：一切开始都很好，但是...
```
......
2017-08-18 10:38:57.642058: precision @ 1 = 0.992
2017-08-18 10:38:58.800137: step 1310, loss = 6.65 (1100.5 examples/sec; 0.116 sec/batch)
2017-08-18 10:38:59.975262: step 1320, loss = 284377.50 (1089.2 examples/sec; 0.118 sec/batch)
ERROR:tensorflow:Model diverged with loss = NaN.
Traceback (most recent call last):
  File "train.py", line 139, in <module>
    tf.app.run()
  .......
tensorflow.python.training.basic_session_run_hooks.NanLossDuringTrainingError: NaN loss during training.
......
```
BOOM！倒霉啦！
这是`NanTensorHook`为你做的报告。
```python
tf.train.MonitoredTrainingSession(
        checkpoint_dir=FLAGS.train_dir,
        hooks=[tf.train.StopAtStepHook(last_step=FLAGS.max_steps),
               tf.train.NanTensorHook(loss),
               _LoggerHook()],
        config=tf.ConfigProto(
            log_device_placement=FLAGS.log_device_placement))
```
添加NanTensorHook总是一个好主意，因为它可以为您节省时间和磁盘空间。    
这个错误有很多可能的原因。绕开它的最简单的方法之一是将学习率降低一半，并重复直到不会抛出任何这样的错误。  
不，认真的，最简单的方法是再次运行同一段代码代码。  

## 保存
我已经成功地使用了三种保存方式：   
### `tf.train.Saver()`保存"checkpoint"文件
"MonitoredTrainingSession"包含一个隐式的"Saver"，所以在演示中找不到。这是如何使用一个： 
```python
#导入数据
#创建模型
#定义丢失和优化器
#构建神经网络
#定义 loss
checkpoint_dir ="logs"
#创建一个Saver对象来保存变量
saver = tf.train.Saver()#没有参数，保存所有变量
summary_writer = tf.summary.FileWriter(checkpoint_dir)
with tf.Session() as sess:
	sess.run(tf.global_variables_initializer())
	for i in range(2000):
		batch = mnist.train.next_batch(50)
		if i%100 == 0:
			train_accuracy = accuracy.eval(....)
			print('步骤%d，训练准确度%g'%(i, train_accuracy))
		train_step.run(....)
			
	#训练完成，保存变量
	saver.save(sess, checkpoint_dir +'/ newpath', global_step = 2000)
	#"newpath"是文件前缀：newpath-2000.data-00000-of-00001
```
非常反直觉的一点是，如果你不调用`tf.train.import_meta_graph`，但是调用`saver.restore`，**你不会得到你的图**。为了正确使用这些变量，你必须再次构造一个图来使用变量。
如果要扩展图，则必须在checkpoint文件中指定要查找的内容。例如：
```python
#建造旧图和新图
variable_averages = tf.train.ExponentialMovingAverage(
general.MOVING_AVERAGE_DECAY)
variables_to_restore = variable_averages.variables_to_restore()
#下面这些变量是新图里面的所以不应该试图读取
del variables_to_restore ['input/vis-image/ExponentialMovingAverage']
del variables_to_restore ['input/vis-image/Adam_1']
del variables_to_restore ['train/beta2_power']
del variables_to_restore ['train/beta1_power']
del variables_to_restore ['input/vis-image/Adam']
saver = tf.train.Saver(variables_to_restore)
```
而您仍然需要初始化新变量：
```python
with tf.Session() as sess:
	sess.run(tf.global_variables_initializer())
```
### `tf.saved_model.builder.SavedModelBuilder`构建"SavedModel"协议缓冲区
在Java中，使用"SavedModelBundel"加载"SavedModel"，但"SavedModel"包含三个文件，因此不方便打包。最后我用序列化的图替换所有这些构建器。
在Python中：
```python
x = tf.placeholder(tf.float32, [None, 784], name ="input_tensor")
....
y_conv = tf.add(tf.matmul(h_fc1_drop, W_fc2), b_fc2, name ='output_tensor')
with tf.Session() as sess: 
  sess.run(tf.global_variables_initializer())
  builder = tf.saved_model.builder.SavedModelBuilder("model");
  for i in range(1000):
    batch = mnist.train.next_batch(50)
    if i%100 == 0:
        train_accuracy = accuracy.eval(......)
    train_step.run(....)

  print('测试精度%g'%accuracy.eval(....))

  builder.add_meta_graph_and_variables(sess, [ "标签"])
  builder.save(True)
  #或者只是builder.save()，如果你不想读它
```
在Java中：
```java
SavedModelBundle smb = SavedModelBundle.load(modelPath, "标签");
Session sess = smb.session();
Tensor image = Tensor.create(toMatrix(imgs))
Tensor result = sess.runner().feed("input_tensor", image).fetch("output_tensor").run().get(0)
```
### `graph_util.convert_variables_to_constants`和`SerializeToString`
阅读[这篇文章](https://blog.metaflow.fr/tensorflow-how-to-freeze-a-model-and-serve-it-with-a-python-api-d4f3596b3adc)。
或者阅读我的例子，因为这文章很长。  
在Python中：
```python
x = tf.placeholder(tf.float32, [None, 784], name ="input_tensor")
....
with tf.variable_scope('softmax_linear')as scope: 
	y_conv = tf.add(tf.matmul(h_fc1_drop, W_fc2), b_fc2, name ='output_tensor')
			
output_node_names ="softmax_linear/output_tensor"
graph = tf.get_default_graph()
input_graph_def = graph.as_graph_def()
with tf.Session() as sess:
output_graph_def = graph_util.convert_variables_to_constants(
		sess, #该Session用于获取变量
		input_graph_def, #graph_def用于检索节点
		output_node_names.split(",")#输出节点名称用于选择有用的节点
	)
	#最后，我们将输出图序列化并转储到文件系统
	with tf.gfile.GFile("frozen_graph.pb", "wb") as f: 
		f.write(output_graph_def.SerializeToString())
```
在Java中：
```java
byte [] graphDef = Files.readAllBytes(Paths.get(modelPath));
Graph g = new Graph();
g.importGraphDef(graphDef);
Session sess = new Session(g);
Tensor image = Tensor.create(toMatrix(imgs))
Tensor result = sess.runner().feed("input_tensor", image).fetch("softmax_linear/output_tensor").run().get(0)
```
## 用TensorBoard可视化
*python/\*.py*  
[*python/visualize.py*](python/visualize.py)\
[*python/visualize_test.py*](python/visualize_test.py)

TensorBoard很棒。 [这个TF文章](https://www.tensorflow.org/get_started/summaries_and_tensorboard)也很棒。
```
  $ TensorBoard --logdir logs
```
运行每个可运行的python文件，找到相应的日志并自己探索TensorBoard。
我将只是强调一些特殊的功能。
### `tf.summary.image`几乎可以显示任何4维数组
实际上，“任何东西”要是这样的：[batch_size，height，width，channels]，其中通道是1,3或4。  
可以先显示它，然后再想它的含义。
### `name_scope`and`variable_scope`使您的图更加可读
就这样。
## 分布式TensorFlow
请参阅[TF：部署](https://www.tensorflow.org/deploy/)   
或者[我实现的distributed TF](../PythonWorksp/TensorFlow/distributed)\
**注意：** 如果要让每个参数服务器（PS）保存其checkpoint的副本，请务必设置
```python
saver = tf.train.Saver(sharded = True)
#这不是默认的
```
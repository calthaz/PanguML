# TensorFlowDemo
This is a very simple demo to complete a moderately simple classification task with [my java classes](http://null_560_5360.oschina.io/tensorflow/index.html) and a slightly adapted version of [TensorFlow's cifar10 model](https://github.com/tensorflow/models/tree/master/tutorials/image/cifar10).
>Env:
> * Python 3
> * TensorFlow 1.2 with GPU support (you might be able to run the same code without GPU supported)
> * and its dependencies...
> * Java 8
> * [TensorFlow libs for java](https://www.tensorflow.org/install/install_java)  

## Folder Structure Overview
* __src - Java source files__
	- prepare - processing images and generate data sets for training and evaluation
	- classifier - the classifier that reads a `graph_def`
	- eval - helper functions to eval the model
* __logs - TensorFlow training chackpoints and save/eval event files__
	- train3500 - stops at step=3500
	- train7000 - stops at step=7000
	- eval - my_eval logs
	- save - save_model logs
* __models - TensorFlow SavedModelBundle and graph_defs__
	- train3500 - stops at step=3500
	- train7000 - stops at step=7000
* __python - Python code__
	- read_image.py - read label files produced by java with a `slice_input_producer`
	- general_cifar.py - structure of the neuron network
	- train.py - uses a `MonitoredTrainingSession` to train the model
	- my_eval.py - eval the model with eval data set
	- save_model.py - save model(s) with `convert_variables_to_constants` and `output_graph_def.SerializeToString()` as well as `SavedModelBuilder`
	- visualize.py - attatch a deconv net to the cifar structure and visualize activations. (https://arxiv.org/pdf/1311.2901.pdf)
	- visualize_test.py - run visualize.py with a given input image
* __training-materials__
	* raw-data - Images collected from the Internet
		- bed - photos from [ImageNet](http://image-net.org) and Baidu
		- flowers - flower photos from [TF: Retrain Inception's Final Layer for New Categories](https://www.tensorflow.org/tutorials/image_retraining)
	* ready 
		- eval - data set for evaluation
		- train - data set for training
		- others - Compressed images 
	* result - Images in eval data set are sorted into corresponding folders according to the model's classification

## Creating Image Data Sets
[*src/SampleProcessor.java*](src/SampleProcessor.java)
### `SampleHelper.batchEditImages`: one tool for everything you want
I find this fuction very handy, since it can process all the images under a folder without altering the original ones -- because it just makes a copy of this folder... 
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
It's very useful for people like me who change their mind once a day, and it makes comparing different algorithms a breeze -- if disk space is not a problem for you.  
### `SampleHelper` and `LabelGenerator` are your friends
`SampleHelper` deal with files.   
`LabelGenerator` evidently, deal with labels. But more importantly, `LabelGenerator` is a bridge between Java and Python: it specifies the way labels and results should be written into txt file and parsed from txt files. `LabelGenerator.LABEL_SEP` not only separate labels, but scores, file names, indices, and any fields in one line.   
`TFUtils` should also be your friends, for it provide lower-level solutions to miscellaneous tasks.
**For more information about `SampleHelper` and `LabelGenerator` please read [the documents](http://null_560_5360.oschina.io/tensorflow/index.html).**

## Building a Deep Neural Network
[*python/general_cifar.py*](python/general_cifar.py)
### Just use `tf.nn`, `tf.train` and so on
Simply read [this TF tutorial](https://www.tensorflow.org/tutorials/deep_cnn)


## Reading data 
[*python/read_image.py*](python/read_image.py)  
### input producers 
There are as many kinds of input producers as there are forms of input. 
This is what we use here because we have 2 separate list of images and labels, and `slice_input_producer` link them with one another.
```python
tf.train.slice_input_producer([images, labels],shuffle=True)
```
\
If you just want to read a sinlge image, you can use
```python
filename_queue = tf.train.string_input_producer(["xxx/xxxx.jpg"])
key, value = reader.read(filename_queue)
my_img = tf.image.decode_jpeg(value)
```
as in [*python/visualize_test.py*](*python/visualize_test.py)  
\
All the reading and batching runs on different threads, so you need a `Coordinator` to supervise them:
```python
# Start input enqueue threads.
coord = tf.train.Coordinator()
threads = tf.train.start_queue_runners(sess=sess, coord=coord)

try:
    while not coord.should_stop():
        # Run training steps or whatever
        sess.run(train_op)

except tf.errors.OutOfRangeError:
    print('Done training -- epoch limit reached')
finally:
    # When done, ask the threads to stop.
    coord.request_stop()

# Wait for threads to finish.
coord.join(threads)
sess.close()
```
**Otherwise you will see NOTHING...**

This awsome gif explains what tf is doing in the mean time:
![Animated File Queues](https://www.tensorflow.org/images/AnimatedFileQueues.gif)
For more information about reading data, visit [TF: Reading Data](https://www.tensorflow.org/programmers_guide/reading_data).

## Training
[*python/train.py*](python/train.py)

### `tf.train.MonitoredTrainingSession` and `tf.train.SessionRunHook`
A `SessionRunHook` can help you costomize your training
During my training, the following methods are called in this order:   
```
begin
  while not mon_sess.should_stop():
	before_run
        actual training: mon_sess.run(train_op)
	after_run
end
```


Sometimes you will see this during training: Everything starts off good, but ... 
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
BOOM! Bad Luck!
This is what a __`NanTensorHook`__ does for you.
```python
tf.train.MonitoredTrainingSession(
        checkpoint_dir=FLAGS.train_dir,
        hooks=[tf.train.StopAtStepHook(last_step=FLAGS.max_steps),
               tf.train.NanTensorHook(loss),
               _LoggerHook()],
        config=tf.ConfigProto(
            log_device_placement=FLAGS.log_device_placement))
```
It is always a good idea to add NanTensorHook, because it saves time and disk space for you. \
There are many possible causes for this error. One of the easiest ways to avoid it is to reduce the learning rate by half and keep on untill no such error is thrown.  
Nope, seriously, the easiest way is to simply run the code again.  
## Saving
I have successfully used three types of saving methods: 
### `tf.train.Saver()` saves "checkpoint" files
A `MonitoredTrainingSession` contains an implicit `Saver` so you can't find it in the demo. This is how to use one: 
```python
# Import data
# Create the model
# Define loss and optimizer
# Build the graph for the deep net
#define loss
checkpoint_dir = "logs"
# create a Saver object to save your variables
saver = tf.train.Saver()# no params, save all variables
summary_writer = tf.summary.FileWriter(checkpoint_dir)
with tf.Session() as sess:
	sess.run(tf.global_variables_initializer())
	for i in range(2000):
		batch = mnist.train.next_batch(50)
		if i % 100 == 0:
			train_accuracy = accuracy.eval(....)
			print('step %d, training accuracy %g' % (i, train_accuracy))
		train_step.run(....)
        
    #training is finished, save the variables   
    saver.save(sess, checkpoint_dir+'/newpath', global_step=2000)
	#"newpath" is the file prefix: newpath-2000.data-00000-of-00001
```
A very counter intuitive point is that if you don't call `tf.train.import_meta_graph` but just call `saver.restore`, **you will not get your graph**. In order to use the variables properly, you'll have to construct a graph again to use the variables.   
If you want to extend your graph, you will have to specify what to look for in the checkpoint files. For example:
```python
#build old graph and new graph

variable_averages = tf.train.ExponentialMovingAverage(
general.MOVING_AVERAGE_DECAY)
variables_to_restore = variable_averages.variables_to_restore()
# The following variables are inside the new graph so should not try to read
del variables_to_restore['input/vis-image/ExponentialMovingAverage']
del variables_to_restore['input/vis-image/Adam_1']
del variables_to_restore['train/beta2_power']
del variables_to_restore['train/beta1_power']
del variables_to_restore['input/vis-image/Adam']
saver = tf.train.Saver(variables_to_restore)
```
And you still need to initialize your new variables:
```python
with tf.Session() as sess:
	sess.run(tf.global_variables_initializer())
```
### `tf.saved_model.builder.SavedModelBuilder` builds "SavedModel" protocol buffers
In Java, use `SavedModelBundel` to load a "SavedModel", but a "SavedModel" contains three files so is not convenient to distribute. In the end I replace all these builders with serialized graph. 
In Python:
```python
x = tf.placeholder(tf.float32, [None, 784], name="input_tensor")
....
y_conv = tf.add(tf.matmul(h_fc1_drop, W_fc2), b_fc2, name='output_tensor')
with tf.Session() as sess:
  sess.run(tf.global_variables_initializer())
  builder = tf.saved_model.builder.SavedModelBuilder("model");
  for i in range(1000):
  batch = mnist.train.next_batch(50)
  if i % 100 == 0:
    train_accuracy = accuracy.eval(......)
    train_step.run(....)

  print('test accuracy %g' % accuracy.eval(....))

  builder.add_meta_graph_and_variables(sess,["tag"])
  builder.save(True)
  # or just builder.save() if you don't want to read it
```
In Java:
```java
SavedModelBundle smb = SavedModelBundle.load(modelPath, "tag");
Session sess = smb.session();
Tensor image = Tensor.create(toMatrix(imgs))
Tensor result = sess.runner().feed("input_tensor", image).fetch("output_tensor").run().get(0)
```
### `graph_util.convert_variables_to_constants` and `SerializeToString`
Read this [article](https://blog.metaflow.fr/tensorflow-how-to-freeze-a-model-and-serve-it-with-a-python-api-d4f3596b3adc).
Or read my example, since that example is very long.     
In Python:
```python
x = tf.placeholder(tf.float32, [None, 784], name="input_tensor")
....
with tf.variable_scope('softmax_linear') as scope:
	y_conv = tf.add(tf.matmul(h_fc1_drop, W_fc2), b_fc2, name='output_tensor')
    
output_node_names = "softmax_linear/output_tensor"
graph = tf.get_default_graph()
input_graph_def = graph.as_graph_def()
with tf.Session() as sess:
	output_graph_def = graph_util.convert_variables_to_constants(
          sess, # The session is used to retrieve the weights
          input_graph_def, # The graph_def is used to retrieve the nodes 
          output_node_names.split(",") # The output node names are used to select the usefull nodes
      ) 
    # Finally we serialize and dump the output graph to the filesystem
    with tf.gfile.GFile("frozen_graph.pb", "wb") as f:
          f.write(output_graph_def.SerializeToString())
```
In Java:
```java
byte[] graphDef = Files.readAllBytes(Paths.get(modelPath));
Graph g = new Graph();
g.importGraphDef(graphDef);
Session sess = new Session(g);
Tensor image = Tensor.create(toMatrix(imgs))
Tensor result = sess.runner().feed("input_tensor", image).fetch("softmax_linear/output_tensor").run().get(0)
```
## Visualization with TensorBoard
*python/\*.py*\
[*python/visualize.py*](python/visualize.py)\
[*python/visualize_test.py*](python/visualize_test.py)

TensorBoard is simply awsome. As well as [this TF article](https://www.tensorflow.org/get_started/summaries_and_tensorboard).
```
 $ TensorBoard --logdir logs
``` 
Run every runnable python file, find the corresponding logs and explore TensorBoard yourself.   
I will just highlight some special functions
### `tf.summary.image` can display almost anything with rank 4
In fact, anything looks like this: [batch_size, height, width, channels] where channels is 1, 3, or 4.  
First display it, then think about what it means.  
### `name_scope`and `variable_scope` make your graph more readable
That's it. 
## Distributed TensorFlow
See [TF: Deploy](https://www.tensorflow.org/deploy/)    
Or [implemented distributed TF](../PythonWorksp/TensorFlow/distributed)
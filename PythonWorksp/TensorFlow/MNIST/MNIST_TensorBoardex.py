import tensorflow as tf 
from tensorflow.examples.tutorials.mnist import input_data

def conv_layer(input, channels_in, channels_out, name="conv"):
	with tf.name_scope(name):
		#w=tf.Variable(tf.zeros([5,5,channels_in,channels_out]), )
		w=tf.Variable(tf.truncated_normal([5,5,channels_in,channels_out], stddev=0.1),name="W")
		#b=tf.Variable(tf.zeros([channels_out]), name="B");
		b=tf.Variable(tf.constant(0.1, shape=[channels_out]), name="B");
		#if the weights and biases are all zero, 
		#zero gradients will pase through so the model won't be trained
		print("conv_layer %s: input shape %s, w shape %s, b shape %s" % (name, input.shape, w.shape, b.shape))
		conv = tf.nn.conv2d(input, w, strides=[1,1,1,1], padding = "SAME")
		act = tf.nn.relu(conv + b)
		tf.summary.histogram("weights", w)
		tf.summary.histogram('biases', b)
		tf.summary.histogram('activations', act)
		return act

def fc_layer(input, channels_in, channels_out, name):
	with tf.name_scope(name):
		w=tf.Variable(tf.truncated_normal([channels_in,channels_out], stddev=0.1), name="W")
		b=tf.Variable(tf.constant(0.1, shape=[channels_out]), name="B")
		print("fc_layer: input shape %s, w shape %s, b shape %s" % (input.shape, w.shape, b.shape))
		act = tf.nn.relu(tf.matmul(input, w)+b)
		return act

mnist = input_data.read_data_sets('/tmp/tensorflow/mnist/input_data', one_hot=True)

x = tf.placeholder(tf.float32, shape=[None, 784], name = "x")
y = tf.placeholder(tf.float32, shape=[None, 10], name="labels")
x_image = tf.reshape(x, [-1, 28, 28, 1])

conv1 = conv_layer(x_image, 1, 32, "conv1")
pool1 = tf.nn.max_pool(conv1, ksize=[1,2,2,1], strides=[1,2,2,1], padding="SAME")

conv2 = conv_layer(pool1, 32, 64, "conv2")
pool2 = tf.nn.max_pool(conv2, ksize=[1,2,2,1], strides=[1,2,2,1], padding="SAME")
print("pool2 shape: %s" % (pool2.shape))

flattened = tf.reshape(pool2, [-1,7*7*64])
print("flattened shape: %s" % (pool2.shape))

fc1 = fc_layer(flattened, 7*7*64, 1024, "fc1")
logits = fc_layer(fc1,1024,10, "fc2")
print("logits shape: %s" % (logits.shape))

with tf.name_scope("xent"):
	xent = tf.reduce_mean(
		tf.nn.softmax_cross_entropy_with_logits(logits = logits, labels = y))

with tf.name_scope("train"):
	train_step = tf.train.AdamOptimizer(1e-4).minimize(xent)

#200 vs 100
with tf.name_scope("accuracy"):
	correct_prediction = tf.equal(tf.argmax(logits, 1), tf.argmax(y, 1))
	accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32))

sess = tf.InteractiveSession()
sess.run(tf.global_variables_initializer())

writer = tf.summary.FileWriter("./logs/tensorboardex3");
writer.add_graph(sess.graph);
tf.summary.scalar('cross_entropy', xent)
tf.summary.scalar('accuracy', accuracy)

tf.summary.image('input', x_image, 3)

merged_summary = tf.summary.merge_all()

for i in range(2000): 
	batch = mnist.train.next_batch(100)
	#print(dir(batch));
	#print(batch);
	#print("x size"+batch[0].shape)
	#print("y size"+batch[1].shape)
	if i%10 ==0:
		s = sess.run(merged_summary, feed_dict={x: batch[0], y:batch[1]})
		writer.add_summary(s,i)
	if i%200==0:
		[train_accuracy] = sess.run([accuracy], feed_dict={x:batch[0], y:batch[1]})
		print("step %d, training accuracy %g" % (i, train_accuracy))
	sess.run(train_step, feed_dict = {x:batch[0], y:batch[1]})
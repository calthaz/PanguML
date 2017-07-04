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
		#print("conv_layer %s: input shape %s, w shape %s, b shape %s" % (name, input.shape, w.shape, b.shape))
		conv = tf.nn.conv2d(input, w, strides=[1,1,1,1], padding = "SAME")
		act = tf.nn.relu(conv + b)
		#tf.summary.histogram("weights", w)
		#tf.summary.histogram('biases', b)
		#tf.summary.histogram('activations', act)
		return act

def fc_layer(input, channels_in, channels_out, name):
	with tf.name_scope(name):
		w=tf.Variable(tf.truncated_normal([channels_in,channels_out], stddev=0.1), name="W")
		b=tf.Variable(tf.constant(0.1, shape=[channels_out]), name="B")
		#print("fc_layer: input shape %s, w shape %s, b shape %s" % (input.shape, w.shape, b.shape))
		act = tf.nn.relu(tf.matmul(input, w)+b)
		return act

def train_main(lr, u2f, u2c, writer):
	mnist = input_data.read_data_sets('/tmp/tensorflow/mnist/input_data', one_hot=True)

	g = tf.Graph()

	with g.as_default():

		x = tf.placeholder(tf.float32, shape=[None, 784], name = "x")
		y = tf.placeholder(tf.float32, shape=[None, 10], name="labels")
		x_image = tf.reshape(x, [-1, 28, 28, 1])

		if(u2c):
			conv1 = conv_layer(x_image, 1, 32, "conv1")
			pool1 = tf.nn.max_pool(conv1, ksize=[1,2,2,1], strides=[1,2,2,1], padding="SAME")

			conv2 = conv_layer(pool1, 32, 64, "conv2")
			pool2 = tf.nn.max_pool(conv2, ksize=[1,2,2,1], strides=[1,2,2,1], padding="SAME")
			#print("pool2 shape: %s" % (pool2.shape))
			flattened = tf.reshape(pool2, [-1,7*7*64])
			#print("flattened shape: %s" % (pool2.shape))
		else:
			conv = conv_layer(x_image, 1, 64, "conv")
			pool = tf.nn.max_pool(conv, ksize=[1,4,4,1], strides=[1,4,4,1], padding="SAME")
			flattened = tf.reshape(pool, [-1,7*7*64])
		
		keep_prob = tf.placeholder(tf.float32)

		if(u2f):
			fc1 = fc_layer(flattened, 7*7*64, 1024, "fc1")
			# Dropout - controls the complexity of the model, prevents co-adaptation of
  			# features.
			
			fc1_drop = tf.nn.dropout(fc1, keep_prob)
			#logits = fc_layer(fc1,1024,10, "fc2")
			with tf.name_scope('fc2'):
				w_fc2=tf.Variable(tf.truncated_normal([1024,10], stddev=0.1), name="W")
				b_fc2=tf.Variable(tf.constant(0.1, shape=[10]), name="B")
				logits = tf.matmul(fc1_drop, w_fc2) + b_fc2
			#print("logits shape: %s" % (logits.shape))
		else:
			logits = fc_layer(flattened, 7*7*64, 10, "fc1")

		with tf.name_scope("xent"):
			xent = tf.reduce_mean(
				tf.nn.softmax_cross_entropy_with_logits(logits = logits, labels = y))

		with tf.name_scope("train"):
			train_step = tf.train.AdamOptimizer(lr).minimize(xent)

		#200 vs 100
		with tf.name_scope("accuracy"):
			correct_prediction = tf.equal(tf.argmax(logits, 1), tf.argmax(y, 1))
			accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32))

		sess = tf.InteractiveSession()
		sess.run(tf.global_variables_initializer())

		writer.add_graph(sess.graph);
		tf.summary.scalar('cross_entropy', xent)
		tf.summary.scalar('accuracy', accuracy)

		#tf.summary.image('input', x_image, 3)

		merged_summary = tf.summary.merge_all()

		for i in range(500): 
			batch = mnist.train.next_batch(50)
			#print(dir(batch));
			#print(batch);
			#print(dir(batch[0].shape))
			#print(dir(batch[1].shape))
			if i%20 == 0:
				s = sess.run(merged_summary, feed_dict={x:batch[0], y:batch[1], keep_prob:1.0})
				writer.add_summary(s,i)
				[train_accuracy] = sess.run([accuracy], feed_dict={x:batch[0], y:batch[1], keep_prob:1.0})
				print("step %d, training accuracy %g" % (i, train_accuracy))
			sess.run(train_step, feed_dict = {x:batch[0], y:batch[1], keep_prob:0.5})

		print('test accuracy %g' % accuracy.eval(feed_dict={
        x: mnist.test.images, y: mnist.test.labels, keep_prob:1.0}))

		writer.close()
		sess.close()

def make_hparam_string(lr, u2f, u2c, bs):
	return "lr_%.e-2fc_%d-2conv_%d-batch_%d"%(lr,u2f,u2c,bs); 


for learning_rate in [1e-3]:#, 1e-4, 1e-5 玄学
	for use_two_fc in [True]:#, False时非常糟糕
		for use_two_conv in [False]:#,True, False时表现略微下降但是快了很多
			for batch_size in [50, 20, 100]:#好像没什么影响，见/tmp/tensorflow/mnist/logs/compare-batch/
				#construct a hyperparameter string for each one
				hparam_str = make_hparam_string(learning_rate, use_two_fc, use_two_conv, batch_size)
				print(hparam_str)
				writer = tf.summary.FileWriter('/tmp/tensorflow/mnist/logs/random/'+hparam_str)
				train_main(learning_rate, use_two_fc, use_two_conv, writer)

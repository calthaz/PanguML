import tensorflow as tf 
import mnist_deep_saver as mnist_saver
import numpy as np

def infer(pic_list):
	with tf.name_scope('input-processing'):#
		filename_queue = tf.train.string_input_producer(pic_list, num_epochs=1,shuffle=False)#
		reader = tf.WholeFileReader()
		key, value = reader.read(filename_queue)
		orig_img = tf.image.decode_jpeg(value)
		resized_img = tf.image.resize_images(orig_img, [mnist_saver.IMAGE_SIZE, mnist_saver.IMAGE_SIZE])
		#resized_img = tf.image.per_image_standardization(resized_img)
		resized_img.set_shape((mnist_saver.IMAGE_SIZE, mnist_saver.IMAGE_SIZE, 3))
		r_channel = tf.slice(resized_img, [0,0,0], [mnist_saver.IMAGE_SIZE, mnist_saver.IMAGE_SIZE, 1])
		g_channel = tf.slice(resized_img, [0,0,1], [mnist_saver.IMAGE_SIZE, mnist_saver.IMAGE_SIZE, 1])
		b_channel = tf.slice(resized_img, [0,0,2], [mnist_saver.IMAGE_SIZE, mnist_saver.IMAGE_SIZE, 1])
		ones = tf.ones([mnist_saver.IMAGE_SIZE, mnist_saver.IMAGE_SIZE, 1])
		fs = tf.constant(255.0*3, shape=[mnist_saver.IMAGE_SIZE, mnist_saver.IMAGE_SIZE, 1])
		normalized = ones - (r_channel+g_channel+b_channel)/fs
		image = tf.expand_dims(normalized, 0)
		#r_channel = tf.expand_dims(r_channel, 0)
		tf.summary.image('input-image', image)

	y_conv, keep_prob = mnist_saver.deepnn(image)
	#result = tf.argmax(y_conv, 1)
	#variable_averages = tf.train.ExponentialMovingAverage(
	#general.MOVING_AVERAGE_DECAY)
	#variables_to_restore = variable_averages.variables_to_restore()
	saver = tf.train.Saver()
	writer = tf.summary.FileWriter("./logs/demo-infer");
	
	merged_summary = tf.summary.merge_all()

	with tf.Session() as sess:
		sess.run(tf.global_variables_initializer())
		sess.run(tf.local_variables_initializer())
		ckpt = tf.train.get_checkpoint_state(mnist_saver.checkpoint_dir)
		if ckpt and ckpt.model_checkpoint_path:
			saver.restore(sess, ckpt.model_checkpoint_path)
			global_step = ckpt.model_checkpoint_path.split('/')[-1].split('-')[-1]
		else:
			print('No checkpoint file found')
			return
		writer.add_graph(sess.graph);
		# Start the queue runners.
		coord = tf.train.Coordinator()
		try:
			threads = []
			for qr in tf.get_collection(tf.GraphKeys.QUEUE_RUNNERS):
				threads.extend(qr.create_threads(sess, coord=coord, daemon=True,
											 start=True))
			s = sess.run(merged_summary)
			writer.add_summary(s,123)
			while(True):
				predictions = y_conv.eval(feed_dict={keep_prob: 1.0})
				print(predictions)
				result = np.argmax(predictions, 1)
				print(result)
			
		except Exception as e:  # pylint: disable=broad-except
			coord.request_stop(e)
			
		coord.request_stop()
		coord.join(threads, stop_grace_period_secs=10)
def main(_):
	pic_set = [r"F:\TensorFlowDev\www\upload-files\MNIST\crop-11502787517_9239.jpg",
	r"F:\TensorFlowDev\JavaWorksp\TensorFlow\img\0.jpg",
	r"F:\TensorFlowDev\JavaWorksp\TensorFlow\img\1.jpg",
	r"F:\TensorFlowDev\JavaWorksp\TensorFlow\img\2.jpg",
	r"F:\TensorFlowDev\JavaWorksp\TensorFlow\img\5.jpg",
	r"F:\TensorFlowDev\JavaWorksp\TensorFlow\img\7.jpg",
	r"F:\TensorFlowDev\JavaWorksp\TensorFlow\img\9.jpg",
	r"F:\TensorFlowDev\PythonWorksp\objectDetector\img\crop26.png",
				r"F:\TensorFlowDev\PythonWorksp\objectDetector\img\crop15.png",
				r"F:\TensorFlowDev\PythonWorksp\objectDetector\img\crop58.png",
				r"F:\TensorFlowDev\PythonWorksp\objectDetector\img\crop17.png",
				r"F:\TensorFlowDev\PythonWorksp\objectDetector\img\crop79.png",
				r"F:\TensorFlowDev\PythonWorksp\objectDetector\img\crop3.png",
				r"F:\TensorFlowDev\PythonWorksp\objectDetector\img\crop29.png",
				r"F:\TensorFlowDev\PythonWorksp\objectDetector\img\crop11.png",
				r"F:\TensorFlowDev\PythonWorksp\objectDetector\img\crop101.png",
				r"F:\TensorFlowDev\PythonWorksp\objectDetector\img\crop23.png"]
	infer(pic_set)

if __name__ == '__main__':
	tf.app.run()
#!"D:\Python\Python35\python.exe"
import tensorflow as tf 
import mnist_deep_saver as mnist_saver
import numpy as np
import json
import cgi
import cgitb; cgitb.enable()

def infer(pic_list):
	with tf.name_scope('input-processing'):#
		if isinstance(pic_list, str):
			filename_queue = tf.train.string_input_producer([pic_list])
			length = 1;
		else:
			filename_queue = tf.train.string_input_producer(pic_list, num_epochs=1,shuffle=False)#
			length = len(pic_list)

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
	saver = tf.train.Saver()
	#writer = tf.summary.FileWriter("./logs/demo-infer");
	
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
		#writer.add_graph(sess.graph);
		# Start the queue runners.
		coord = tf.train.Coordinator()
		ret = []; 
		try:
			threads = []
			for qr in tf.get_collection(tf.GraphKeys.QUEUE_RUNNERS):
				threads.extend(qr.create_threads(sess, coord=coord, daemon=True,
											 start=True))
			s = sess.run(merged_summary)
			#writer.add_summary(s,123)
			for x in range(length):
				predictions = y_conv.eval(feed_dict={keep_prob: 1.0})
				#print(predictions)
				result = np.squeeze(np.argmax(predictions, 1))
				ret.append(result.tolist())
			
		except Exception as e:  # pylint: disable=broad-except
			coord.request_stop(e)
			
		coord.request_stop()
		coord.join(threads, stop_grace_period_secs=10)

		print(json.dumps(ret))

def main(_):
	form = cgi.FieldStorage()
	path = form.getvalue("digitFilePaths[]")
	if path is not None:
		infer(path)
	else:
		print(path)

if __name__ == '__main__':
	mystatus = "200 OK"
	print("Status: %s\n" % mystatus)
	tf.app.run()
import tensorflow as tf 
import general

IMAGE_SIZE = general.IMAGE_SIZE

FLAGS = tf.app.flags.FLAGS

# Basic model parameters.
tf.app.flags.DEFINE_string('checkpoint_dir', 'F:/TensorFlowDev/PythonWorksp/TensorFlow/StyleClassifier/logs/style128-3-style-only-lr-0.08',
						   """Directory where to read model checkpoints.""")
tf.app.flags.DEFINE_string('vis_dir', './logs/vis-test-style-lr-0.08',
						   """Directory where to write event logs """
						   """and checkpoint.""")
def main(_):

	FLAGS.batch_size = 1
	pic_set = ["F:/TensorFlowDev/PythonWorksp/TensorFlow/furniture/bed/baby-bed/baby-bed356.jpg",
				"F:/TensorFlowDev/PythonWorksp/TensorFlow/furniture/bed/hammock/hammock1476.jpg",
				"F:/TensorFlowDev/PythonWorksp/TensorFlow/RetrainInception/flower_photos/tulips/3150964108_24dbec4b23_m.jpg",
				"F:/TensorFlowDev/PythonWorksp/TensorFlow/RetrainInception/flower_photos/tulips/3105702091_f02ce75226.jpg",
				"F:/TensorFlowDev/www/upload-files/6454_14991605780.jpg",
				"F:/TensorFlowDev/www/upload-files/4589_14991507381.jpg",
				"F:/TensorFlowDev/www/upload-files/7255_14991507381.jpg",
				"F:/TensorFlowDev/PythonWorksp/TensorFlow/furniture/bed/bunk-bed/bunk-bed576.jpg",
				"F:/TensorFlowDev/www/upload-files/1501723500_05517.jpg",
				"F:/TensorFlowDev/www/upload-files/1501723499_19721-n.jpg"]
	with tf.name_scope('input'):#
		img_path=pic_set[9]
		filename_queue = tf.train.string_input_producer([img_path])#
		reader = tf.WholeFileReader()
		key, value = reader.read(filename_queue)
		orig_img = tf.image.decode_jpeg(value)
		print('here1')
		resized_img = tf.image.resize_images(orig_img, [IMAGE_SIZE, IMAGE_SIZE])
		resized_img.set_shape((IMAGE_SIZE, IMAGE_SIZE, 3))

		float_img = tf.image.per_image_standardization(resized_img)
		image = tf.expand_dims(float_img, 0)
		print('here2')
		tf.summary.image('input-image', image)

	#logits = general.inference(image)
	action = general.inference(image)

	# Restore the moving average version of the learned variables for eval.
	variable_averages = tf.train.ExponentialMovingAverage(
	general.MOVING_AVERAGE_DECAY)
	variables_to_restore = variable_averages.variables_to_restore()#moving_avg_variables=tf.moving_average_variables() still lack somthing
	#del variables_to_restore['input/vis-image/ExponentialMovingAverage']
	#del variables_to_restore['input/vis-image/Adam_1']
	#del variables_to_restore['train/beta2_power']
	#del variables_to_restore['train/beta1_power']
	#del variables_to_restore['input/vis-image/Adam']
	saver = tf.train.Saver(variables_to_restore)

	merged_summary = tf.summary.merge_all()
	summary_writer = tf.summary.FileWriter(FLAGS.vis_dir+img_path[img_path.rfind('/'):img_path.rfind('.')])
	

	with tf.Session() as sess:
		sess.run(tf.global_variables_initializer())
		ckpt = tf.train.get_checkpoint_state(FLAGS.checkpoint_dir)
		if ckpt and ckpt.model_checkpoint_path:
			# Restores from checkpoint
			saver.restore(sess, ckpt.model_checkpoint_path)
			# Assuming model_checkpoint_path looks something like:
			#   /my-favorite-path/cifar10_train/model.ckpt-0,
			# extract global_step from it.
			global_step = ckpt.model_checkpoint_path.split('/')[-1].split('-')[-1]
		else:
			print('No checkpoint file found')
			return

		summary_writer.add_graph(sess.graph)
		#print(sess.run(image))
		#hang forever?

		# Start the queue runners.
		coord = tf.train.Coordinator()
		try:
			threads = []
			for qr in tf.get_collection(tf.GraphKeys.QUEUE_RUNNERS):
			  threads.extend(qr.create_threads(sess, coord=coord, daemon=True,
			                                   start=True))
			  print(sess.run(action))
			  '''
			pool1, argmax = sess.run(action)

			raw = [[0 for x in range(FLAGS.batch_size)] for y in range(IMAGE_SIZE*IMAGE_SIZE*64)] 
			for i in range(int(IMAGE_SIZE/2)*int(IMAGE_SIZE/2)*64):
				for j in range(FLAGS.batch_size):
					raw[j][argmax[j][i]] = pool1[j][i]

			unpooled = tf.conver_to_tensor(raw)
			unpooled = tf.reshape(unpooled, [FLAGS.batch_size, IMAGE_SIZE, IMAGE_SIZE, 64])
			unpooled_trans = general.deconv1(unpooled, kernel1)
			tf.summary.image("reverse_pool1_discrete", unpooled_trans, max_outputs=16)
			#neurons = tf.split(argmax, 64, axis=3)
			#neurons = tf.squeeze(neurons)
			#tf.summary.image("argmax_pool1", )
			'''
			summary = sess.run(merged_summary)
			summary_writer.add_summary(summary, 0)

		except Exception as e:  # pylint: disable=broad-except
			coord.request_stop(e)

		coord.request_stop()
		coord.join(threads, stop_grace_period_secs=10)

		print('here3')

	return
'''

'''
if __name__ == '__main__':
	tf.app.run()
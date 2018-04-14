#coding=utf-8
from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

from datetime import datetime
import math
import time
import os
import numpy as np
import tensorflow as tf
from tensorflow.contrib.tensorboard.plugins import projector

import read_image

FLAGS = tf.app.flags.FLAGS

tf.app.flags.DEFINE_string('eval_data', 'test',
						   """Either 'test' or 'train_eval'.""")
#tf.app.flags.DEFINE_string('checkpoint_dir', 'F:\TensorFlowDev\PythonWorksp\TensorFlow\FurnitureClassifier\logs\furniture2',
						   #"""Directory where to read model checkpoints.""")
tf.app.flags.DEFINE_string('log_dir', './logs/embed-proj-hard',
						   """Directory where to save embeddings""")
tf.app.flags.DEFINE_integer('num_examples', 4000,
							"""Number of examples to run.""")
tf.app.flags.DEFINE_integer('eval_batch_size', 1850,#2800,
							"""Number of examples include in the projector, because I haven't figured out a way to include multiple batches.""")
def get_embedding():

	with tf.Graph().as_default() as g:
		# Get images and labels for CIFAR-10.
		eval_data = FLAGS.eval_data == 'test'
		(images, labels), label_list = read_image.static_crop_inputs(eval_data, FLAGS.eval_batch_size)
	
		embedding_var = tf.Variable(images, trainable=False, name='embedding')
	
		def save_metadata(file):
			with open(file, 'w') as f:
				for i in range(len(label_list)):
					c = label_list[i]
					f.write('{}\n'.format(c))
	
		save_metadata(FLAGS.log_dir + '/metadata.tsv')

		summary_writer = tf.summary.FileWriter(FLAGS.log_dir)
	
		with tf.Session() as sess:
	
			summary_writer.add_graph(sess.graph)
			
			merged_summary = tf.summary.merge_all()
			# Start the queue runners.
			coord = tf.train.Coordinator()
			try:
				threads = []
				for qr in tf.get_collection(tf.GraphKeys.QUEUE_RUNNERS):
					threads.extend(qr.create_threads(sess, coord=coord, daemon=True,
												   start=True))
				
				num_iter = 0;

				sess.run(tf.global_variables_initializer())
				# Format: tensorflow/tensorboard/plugins/projector/projector_config.proto
				config = projector.ProjectorConfig()

				# You can add multiple embeddings. Here we add only one.
				embedding = config.embeddings.add()
				embedding.tensor_name = embedding_var.name
				# Link this tensor to its metadata file (e.g. labels).
				embedding.metadata_path = 'metadata.tsv'
				#embed.sprite.image_path = os.path.join('sprite.png')

				# Specify the width and height of a single thumbnail.
				#embed.sprite.single_image_dim.extend([40, 40])

				# Use the same LOG_DIR where you stored your checkpoint.
				saver = tf.train.Saver()
				

				# The next line writes a projector_config.pbtxt in the LOG_DIR. TensorBoard will
				# read this file during startup.
				projector.visualize_embeddings(summary_writer, config)

				saver.save(sess, os.path.join(
      			FLAGS.log_dir, 'a_model.ckpt'), global_step=num_iter)	


				summary = sess.run(merged_summary)
				summary_writer.add_summary(summary, 0)
		
			except Exception as e:  # pylint: disable=broad-except
				coord.request_stop(e)
			
			coord.request_stop()
			coord.join(threads, stop_grace_period_secs=10)

def main(argv=None):  # pylint: disable=unused-argument
	#cifar10.maybe_download_and_extract()
	if tf.gfile.Exists(FLAGS.log_dir ):
		tf.gfile.DeleteRecursively(FLAGS.log_dir )
		tf.gfile.MkDir(FLAGS.log_dir )
	tf.gfile.MakeDirs(FLAGS.log_dir ) 
	get_embedding()


if __name__ == '__main__':
	tf.app.run()
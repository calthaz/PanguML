from __future__ import absolute_import
from __future__ import division
from __future__ import print_function
import tensorflow as tf 
import numpy as np
from tensorflow.python import debug as tf_debug 
import general

FLAGS = tf.app.flags.FLAGS

tf.app.flags.DEFINE_string('vis_dir', './logs/furniture-vis',
						   """Directory where to write event logs """
						   """and checkpoint.""")
tf.app.flags.DEFINE_integer('max_steps', 1000, #1000000,
							"""Number of batches to run.""")
tf.app.flags.DEFINE_string('checkpoint_dir', './logs/furniture2',
						   """Directory where to read model checkpoints.""")
tf.app.flags.DEFINE_integer('log_frequency', 20,
							"""How often to log results to the console.""")

IMAGE_SIZE = 32


def get_noise_image():
	with tf.variable_scope('input'):
		image = tf.get_variable("vis-image", initializer=tf.truncated_normal([1, IMAGE_SIZE, IMAGE_SIZE, 3]))
		label = 1
		return image, label

def loss(logits, label):
	with tf.name_scope('loss'):
		desired_prediction = tf.slice(logits, [0, label], [1, 1])
		loss = 1 - tf.divide(desired_prediction, tf.reduce_sum(logits))
		#top_k_op = tf.nn.in_top_k(logits, labels, 1)
		return loss

def approach(score):
	#https://stackoverflow.com/questions/35298326/freeze-some-variables-scopes-in-tensorflow-stop-gradient-vs-passing-variables
	with tf.name_scope('train'):
		optimizer = tf.train.AdamOptimizer(0.01)

		train_vars = tf.get_collection(tf.GraphKeys.TRAINABLE_VARIABLES,
											 "input")
		train_op = optimizer.minimize(score, var_list=train_vars)
		return train_op

def main_wrong(argv=None):  
	#if tf.gfile.Exists(FLAGS.vis_dir):
		#tf.gfile.DeleteRecursively(FLAGS.vis_dir)
	#tf.gfile.MakeDirs(FLAGS.vis_dir)

	FLAGS.batch_size = 1

	with tf.Session() as sess:
		new_saver = tf.train.import_meta_graph(FLAGS.checkpoint_dir+'/model.ckpt-8093.meta')
		new_saver.restore(sess, FLAGS.checkpoint_dir+'/model.ckpt-8093')
		summary_writer = tf.summary.FileWriter(FLAGS.vis_dir)
		summary_writer.add_graph(sess.graph)
		#what will be in the graph if I do nothing here?
		#well i already get the whole graph. 
		#so the following method will fail

		image, label = get_noise_image()
		#logits = general.inference(image)
		#score = loss(logits, label)
		#train_op = approach(score)

		tf.summary.image('vis-image', image)
		#tf.summary.histogram('logits', logits)
		merged_summary = tf.summary.merge_all()


		sess.run(tf.global_variables_initializer())
	
		for i in range (FLAGS.max_steps):
			
			if (i % FLAGS.log_frequency==0):
				summary, acc = sess.run([merged_summary, score])
				summary_writer.add_summary(summary, i)
				print('Accuracy at step %s: %s' % (i, 1-acc))
			else:
				sess.run(train_op)

def main(argv=None):
	FLAGS.batch_size = 1
	image, label = get_noise_image()
	logits = general.inference(image)
	score = loss(logits, label)
	train_op = approach(score)
	# Restore the moving average version of the learned variables for eval.
	variable_averages = tf.train.ExponentialMovingAverage(
	general.MOVING_AVERAGE_DECAY)
	variables_to_restore = variable_averages.variables_to_restore()#moving_avg_variables=tf.moving_average_variables() still lack somthing
	del variables_to_restore['input/vis-image/ExponentialMovingAverage']
	del variables_to_restore['input/vis-image/Adam_1']
	del variables_to_restore['train/beta2_power']
	del variables_to_restore['train/beta1_power']
	del variables_to_restore['input/vis-image/Adam']
	saver = tf.train.Saver(variables_to_restore)


	tf.summary.image('vis-image', image)
	tf.summary.histogram('logits', logits)
	merged_summary = tf.summary.merge_all()

	summary_writer = tf.summary.FileWriter(FLAGS.vis_dir)

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
		
		for i in range (FLAGS.max_steps):
			
			if (i % FLAGS.log_frequency==0):
				summary, acc = sess.run([merged_summary, score])
				summary_writer.add_summary(summary, i)
				print('Accuracy at step %s: %s' % (i, 1-acc))
			else:
				sess.run(train_op)

if __name__ == '__main__':
	tf.app.run()
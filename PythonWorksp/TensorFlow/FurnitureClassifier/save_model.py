from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

from datetime import datetime
import math
import time

import numpy as np
import tensorflow as tf

import general
import read_image

FLAGS = tf.app.flags.FLAGS

tf.app.flags.DEFINE_string('eval_dir', '/tmp/furniture-eval',
                           """Directory where to write event logs.""")
tf.app.flags.DEFINE_string('eval_data', 'test',
                           """Either 'test' or 'train_eval'.""")
tf.app.flags.DEFINE_string('checkpoint_dir', '/tmp/furniture1',
                           """Directory where to read model checkpoints.""")
tf.app.flags.DEFINE_integer('num_examples', 1000,
                            """Number of examples to run.""")
tf.app.flags.DEFINE_string('model_dir', '.\model', 
                            """Directory where to save model""")
#tf.app.flags.DEFINE_integer('eval_batch_size', 10,
                            #"""Number of examples to run.""")

def build_and_save(images, builder):
  # Build a Graph that computes the logits predictions from the
  # inference model.
  logits = general.inference(images)

  # Restore the moving average version of the learned variables for eval.
  variable_averages = tf.train.ExponentialMovingAverage(
    general.MOVING_AVERAGE_DECAY)
  variables_to_restore = variable_averages.variables_to_restore()
  saver = tf.train.Saver(variables_to_restore)

  merged_summary = tf.summary.merge_all()
  # Build the summary operation based on the TF collection of Summaries.
  #summary_op = tf.summary.merge_all()

  #summary_writer = tf.summary.FileWriter(FLAGS.eval_dir, g)

  with tf.Session() as sess:
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

    writer = tf.summary.FileWriter("/tmp/tensorflow/fc/logs/save-md")
    writer.add_graph(sess.graph)

    # Start the queue runners.
    coord = tf.train.Coordinator()
    try:
      threads = []
      for qr in tf.get_collection(tf.GraphKeys.QUEUE_RUNNERS):
        threads.extend(qr.create_threads(sess, coord=coord, daemon=True,
                                         start=True))
      #什么也不干
      builder.add_meta_graph_and_variables(sess,[tf.saved_model.tag_constants.SERVING])
      builder.save(True)
    except Exception as e:  # pylint: disable=broad-except
      coord.request_stop(e)

    coord.request_stop()
    coord.join(threads, stop_grace_period_secs=10)

def save_model_1():

  with tf.Graph().as_default() as g:

    #这样居然也可以
    FLAGS.batch_size = 1

    height = read_image.IMAGE_SIZE
    width = read_image.IMAGE_SIZE

    orig_image = tf.placeholder(tf.float32, [height, width, 3], name="input_tensor")

    builder = tf.saved_model.builder.SavedModelBuilder(FLAGS.model_dir+str(FLAGS.batch_size))

    with tf.name_scope("img_processing"):
      # Image processing for evaluation.
      # Crop the central [height, width] of the image. no, this shall not work
      resized_image = tf.image.resize_images(orig_image, [height, width])

      # Subtract off the mean and divide by the variance of the pixels.
      float_image = tf.image.per_image_standardization(resized_image)

      # Set the shapes of tensors.
      float_image.set_shape([height, width, 3])
    
      image = tf.expand_dims(float_image, 0)

    build_and_save(image, builder)
    

def save_model_50():
  with tf.Graph().as_default() as g:

    #这样居然也可以
    FLAGS.batch_size = 50

    height = read_image.IMAGE_SIZE
    width = read_image.IMAGE_SIZE

    orig_images = tf.placeholder(tf.float32, [FLAGS.batch_size, height, width, 3], name="input_tensor")

    builder = tf.saved_model.builder.SavedModelBuilder(FLAGS.model_dir+str(FLAGS.batch_size))

    with tf.name_scope("img_processing"):
      # Image processing for evaluation.
      # Crop the central [height, width] of the image. no, this shall not work
      resized_images = tf.image.resize_images(orig_images, [height, width])
      #print(resized_images)

      split_images = tf.split(resized_images, num_or_size_splits=FLAGS.batch_size, axis=0)
      #print(split_images)

      float_images = []

      for i in range(FLAGS.batch_size):
        # Subtract off the mean and divide by the variance of the pixels.
        float_images.append(tf.image.per_image_standardization(tf.squeeze(split_images[i])))

        # Set the shapes of tensors.
        float_images[i].set_shape([height, width, 3])

      #print(float_images)

      images = tf.stack(float_images, axis=0)
      #print(images)

    build_and_save(images, builder)

def main(argv=None):  # pylint: disable=unused-argument
  #cifar10.maybe_download_and_extract()
  if tf.gfile.Exists(FLAGS.eval_dir):
    tf.gfile.DeleteRecursively(FLAGS.eval_dir)
  tf.gfile.MakeDirs(FLAGS.eval_dir)
  save_model_50()


if __name__ == '__main__':
  tf.app.run()
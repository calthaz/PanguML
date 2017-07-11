# Copyright 2015 The TensorFlow Authors. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ==============================================================================

"""Builds the CIFAR-10 network.

Summary of available functions:

 # Compute input images and labels for training. If you would like to run
 # evaluations, use inputs() instead.
 inputs, labels = distorted_inputs()

 # Compute inference on the model inputs to make a prediction.
 predictions = inference(inputs)

 # Compute the total loss of the prediction with respect to the labels.
 loss = loss(predictions, labels)

 # Create a graph to run one step of training with respect to the loss.
 train_op = train(loss, global_step)
"""
# pylint: disable=missing-docstring
from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import os
import re
import sys
import tarfile

from six.moves import urllib
import tensorflow as tf

FLAGS = tf.app.flags.FLAGS

# Basic model parameters.
tf.app.flags.DEFINE_integer('batch_size', 128,
                            """Number of images to process in a batch.""")
#tf.app.flags.DEFINE_string('data_dir', '/tmp/cifar10_data',
                           #"""Path to the CIFAR-10 data directory.""")
tf.app.flags.DEFINE_boolean('use_fp16', False,
                            """Train the model using fp16.""")

# Global constants describing the CIFAR-10 data set.
IMAGE_SIZE = 128
NUM_CLASSES = 6
NUM_EXAMPLES_PER_EPOCH_FOR_TRAIN = 500
NUM_EXAMPLES_PER_EPOCH_FOR_EVAL = 100


# Constants describing the training process.
MOVING_AVERAGE_DECAY = 0.9999     # The decay to use for the moving average.
NUM_EPOCHS_PER_DECAY = 350.0      # Epochs after which learning rate decays.
LEARNING_RATE_DECAY_FACTOR = 0.1  # Learning rate decay factor.
INITIAL_LEARNING_RATE = 0.1       # Initial learning rate.

# If a model is trained with multiple GPUs, prefix all Op names with tower_name
# to differentiate the operations. Note that this prefix is removed from the
# names of the summaries when visualizing a model.
TOWER_NAME = 'tower'

DATA_URL = 'http://www.cs.toronto.edu/~kriz/cifar-10-binary.tar.gz'

def _activation_summary(x):
  """Helper to create summaries for activations.

  Creates a summary that provides a histogram of activations.
  Creates a summary that measures the sparsity of activations.

  Args:
    x: Tensor
  Returns:
    nothing
  """
  # Remove 'tower_[0-9]/' from the name in case this is a multi-GPU training
  # session. This helps the clarity of presentation on tensorboard.
  tensor_name = re.sub('%s_[0-9]*/' % TOWER_NAME, '', x.op.name)
  tf.summary.histogram(tensor_name + '/activations', x)
  tf.summary.scalar(tensor_name + '/sparsity',
                                       tf.nn.zero_fraction(x))


def _variable_on_cpu(name, shape, initializer):
  """Helper to create a Variable stored on CPU memory.

  Args:
    name: name of the variable
    shape: list of ints
    initializer: initializer for Variable

  Returns:
    Variable Tensor
  """
  with tf.device('/cpu:0'):
    dtype = tf.float16 if FLAGS.use_fp16 else tf.float32
    var = tf.get_variable(name, shape, initializer=initializer, dtype=dtype)
  return var


def _variable_with_weight_decay(name, shape, stddev, wd):
  """Helper to create an initialized Variable with weight decay.

  Note that the Variable is initialized with a truncated normal distribution.
  A weight decay is added only if one is specified.

  Args:
    name: name of the variable
    shape: list of ints
    stddev: standard deviation of a truncated Gaussian
    wd: add L2Loss weight decay multiplied by this float. If None, weight
        decay is not added for this Variable.

  Returns:
    Variable Tensor
  """
  dtype = tf.float16 if FLAGS.use_fp16 else tf.float32
  var = _variable_on_cpu(
      name,
      shape,
      tf.truncated_normal_initializer(stddev=stddev, dtype=dtype))
  if wd is not None:
    weight_decay = tf.multiply(tf.nn.l2_loss(var), wd, name='weight_loss')
    tf.add_to_collection('losses', weight_decay)
  return var

def deconv1(output, kernel):
  trans = tf.nn.conv2d_transpose(output, kernel, output_shape=[FLAGS.batch_size, IMAGE_SIZE, IMAGE_SIZE, 3], strides=[1,1,1,1], padding='SAME')
  return trans

def unpool1(pool, kernel1):
  #let me use image resize to temporarily achieve this, since I can't find a tf method to do it
  unpooled = tf.image.resize_images(pool, [IMAGE_SIZE, IMAGE_SIZE])
  pool1_trans = deconv1(unpooled, kernel1)
  return pool1_trans

def devonv2(output, kernel2, kernel1):
  trans = tf.nn.conv2d_transpose(output, kernel2, output_shape=[FLAGS.batch_size, int(IMAGE_SIZE/2), int(IMAGE_SIZE/2), 64], strides=[1,1,1,1], padding='SAME')
  trans = unpool1(trans, kernel1)
  return trans

def unpool2(pool, kernel2, kernel1):
  #let me use image resize to temporarily achieve this, since I can't find a tf method to do it
  unpooled = tf.image.resize_images(pool, [int(IMAGE_SIZE/2), int(IMAGE_SIZE/2)])
  pool2_trans = deconv2(unpooled, kernel2, kernel1)
  return pool2_trans

def inference(images):
  """Build the CIFAR-10 model.
  Args:
    images: Images returned from distorted_inputs() or inputs().
  Returns:
    Logits.
  """
  # conv1 
  with tf.variable_scope('conv1') as scope:
    kernel1 = _variable_with_weight_decay('weights',
                                         shape=[5, 5, 3, 64],
                                         stddev=5e-2,
                                         wd=0.0)
    conv = tf.nn.conv2d(images, kernel1, [1, 1, 1, 1], padding='SAME')
    biases1 = _variable_on_cpu('biases', [64], tf.constant_initializer(0.0))
    pre_activation = tf.nn.bias_add(conv, biases1)
    conv1 = tf.nn.relu(pre_activation, name=scope.name)
    _activation_summary(conv1)
    
  with tf.variable_scope('conv1_visualization'):
    '''
    # scale weights to [0 1], type is still float
    x_min = tf.reduce_min(kernel1)
    x_max = tf.reduce_max(kernel1)
    kernel_0_to_1 = (kernel1 - x_min) / (x_max - x_min)
    # to tf.image_summary format [batch_size, height, width, channels]
    kernel_transposed = tf.transpose (kernel_0_to_1, [3, 0, 1, 2])
    # this will display random 3 filters from the 64 in conv1
    tf.summary.image('conv1/filters', kernel_transposed, max_outputs=3)
    layer1_image1 = conv1[0:1, :, :, 0:16]
    layer1_image1 = tf.transpose(layer1_image1, perm=[3,1,2,0])
    tf.summary.image("filtered_images_layer1", layer1_image1, max_outputs=16)
    '''
    #conv1 = tf.subtract(conv1, biases1)
    #conv1 = tf.nn.relu(conv1, name=scope.name)
    conv_trans = tf.nn.conv2d_transpose(conv1, kernel1, output_shape=[FLAGS.batch_size, IMAGE_SIZE, IMAGE_SIZE, 3], strides=[1,1,1,1], padding='SAME')
    tf.summary.image("reverse_conv1", conv_trans)
    #find strongest activation here?
    #what is a strongest activation? contrast?
    #how? anyway let's play
    flattened_conv1 = tf.reshape(conv1, [FLAGS.batch_size, IMAGE_SIZE*IMAGE_SIZE, 64])#[1,1024,64]
    mean, variance = tf.nn.moments(flattened_conv1, 1)
    max_index = tf.argmax(variance, 1)#[1,64] in, out [1, 1]
    #remove all other filters and revert
    zeros = tf.zeros([FLAGS.batch_size, IMAGE_SIZE, IMAGE_SIZE, 64])
    size_temp = tf.convert_to_tensor([FLAGS.batch_size,IMAGE_SIZE,IMAGE_SIZE])
    max_index = tf.cast(max_index, tf.int32)
    lower_half_size = tf.concat([size_temp, max_index], 0)
    upper_half_size = tf.concat([size_temp, 64-max_index-1], 0)
    lower_half = tf.slice(zeros, [0,0,0,0], lower_half_size)
    max_index = tf.squeeze(max_index)# out int
    upper_half = tf.slice(zeros, [0,0,0,max_index+1], upper_half_size)
    max_slice = tf.slice(conv1, [0,0,0,max_index], [FLAGS.batch_size,IMAGE_SIZE,IMAGE_SIZE,1])    
    #can't use max_index in these places
    #lower_half = tf.concat([zeros for x in range(64-max_index-1)], axis=3)
    #upper_half = tf.concat([zeros for x in range(max_index)], axis=3)
    #lower_half = tf.slice(conv1, [0,0,0,0], [FLAGS.batch_size,IMAGE_SIZE,IMAGE_SIZE,max_index])
    #upper_half = tf.slice(conv1, [0,0,0,max_index+1], [FLAGS.batch_size,IMAGE_SIZE,IMAGE_SIZE,64-max_index-1])
    #lower_half = tf.zeros(shape=[FLAGS.batch_size,IMAGE_SIZE,IMAGE_SIZE, max_index], dtype=tf.float32)
    #upper_half = tf.zeros(shape=[FLAGS.batch_size,IMAGE_SIZE,IMAGE_SIZE, 64-max_index-1], dtype=tf.float32)    
    max_filters = tf.concat([lower_half, max_slice, upper_half], axis=3)
    #if stack: output shape:[1,32,32,64,1]
    #max_filters = tf.concat([max_slice for x in range(64)], axis=3)
    #虽然说这样看起来更有道理，但是图片看起来实在没什么区别
    max_trans = tf.nn.conv2d_transpose(max_filters, kernel1, output_shape=[FLAGS.batch_size, IMAGE_SIZE, IMAGE_SIZE, 3], strides=[1,1,1,1], padding='SAME')
    tf.summary.image("reverse_max_conv1", max_trans, max_outputs=16)
    #image input: [batch_size, height, width, channels]

  # pool1
  pool1, argmax= tf.nn.max_pool_with_argmax(conv1, ksize=[1, 3, 3, 1], strides=[1, 2, 2, 1],
                         padding='SAME', name='pool1')
  with tf.name_scope('pool1_visualization'):
    '''
    flattened_pool1 = tf.reshape(pool1, [FLAGS.batch_size, 16*16*64])
    flattened_argmax = tf.reshape(pool1, [FLAGS.batch_size, 16*16*64])
    raw = [[0 for x in range(FLAGS.batch_size)] for y in range(32*32*64)] 
    for i in range(16*16*64):
      for j in range(FLAGS.batch_size):
        raw[j][flattened_argmax[j][i]] = flattened_pool1[j][i]
    unpooled = tf.reshape([FLAGS.batch_size, 32, 32, 64])
    #neurons = tf.split(argmax, 64, axis=3)
    #neurons = tf.squeeze(neurons)
    #tf.summary.image("argmax_pool1", )
    '''
    #return tf.reshape(pool1, [FLAGS.batch_size, 16*16*64]), tf.reshape(pool1, [FLAGS.batch_size, 16*16*64])
    #let me use image resize to temporarily achieve this, since I can't find a tf method to do it
    unpooled = tf.image.resize_images(pool1, [IMAGE_SIZE, IMAGE_SIZE])
    pool1_trans = tf.nn.conv2d_transpose(unpooled, kernel1, output_shape=[FLAGS.batch_size, IMAGE_SIZE, IMAGE_SIZE, 3], strides=[1,1,1,1], padding='SAME')
    tf.summary.image("reverse_pool1", pool1_trans, max_outputs=16)
    
  # norm1
  norm1 = tf.nn.lrn(pool1, 4, bias=1.0, alpha=0.001 / 9.0, beta=0.75,
                    name='norm1')
  #???what is it? just ignore it and see what will happen

  # conv2
  with tf.variable_scope('conv2') as scope:
    kernel2 = _variable_with_weight_decay('weights',
                                         shape=[5, 5, 64, 64],
                                         stddev=5e-2,
                                         wd=0.0)
    conv = tf.nn.conv2d(norm1, kernel2, [1, 1, 1, 1], padding='SAME')
    biases2 = _variable_on_cpu('biases', [64], tf.constant_initializer(0.1))
    pre_activation = tf.nn.bias_add(conv, biases2)
    conv2 = tf.nn.relu(pre_activation, name=scope.name)
    _activation_summary(conv2)
  with tf.name_scope("conv2_visualization"):
    #find strongest activation here?
    #what is a strongest activation? contrast?
    #how? anyway let's play
    #conv2 = tf.subtract(conv2, biases2)
    flattened_conv2 = tf.reshape(conv2, [FLAGS.batch_size, int(IMAGE_SIZE/2)**2, 64])#[1,1024,64]
    mean, variance = tf.nn.moments(flattened_conv2, 1)
    max_index = tf.argmax(variance, 1)#[1,64] in, int out
    #remove all other filters and revert
    max_index = tf.squeeze(max_index)
    max_slice = tf.slice(conv2, [0,0,0,max_index], [FLAGS.batch_size,int(IMAGE_SIZE/2),int(IMAGE_SIZE/2),1])
    #max_filters = tf.concat([max_slice for x in range(64)], axis=3) output shape:[1,32,32,64,1]
    max_filters = tf.concat([max_slice for x in range(64)], axis=3)
    max_trans = tf.nn.conv2d_transpose(max_filters, kernel2, 
      output_shape=[FLAGS.batch_size, int(IMAGE_SIZE/2), int(IMAGE_SIZE/2), 64], strides=[1,1,1,1], padding='SAME')
    trans = unpool1(max_trans, kernel1)
    tf.summary.image("reverse_max_conv2", trans, max_outputs=16)
      #image input: [batch_size, height, width, channels]

  # norm2
  norm2 = tf.nn.lrn(conv2, 4, bias=1.0, alpha=0.001 / 9.0, beta=0.75,
                    name='norm2')
  # pool2
  pool2 = tf.nn.max_pool(norm2, ksize=[1, 3, 3, 1],
                         strides=[1, 2, 2, 1], padding='SAME', name='pool2')
  with tf.name_scope('pool2_visualization'):
    unpooled = tf.image.resize_images(pool2, [int(IMAGE_SIZE/2), int(IMAGE_SIZE/2)])
    pool2_trans = devonv2(unpooled, kernel2, kernel1)
    tf.summary.image("reverse_pool2", pool2_trans, max_outputs=16)

  # local3
  with tf.variable_scope('local3') as scope:
    # Move everything into depth so we can perform a single matrix multiply.
    reshape = tf.reshape(pool2, [FLAGS.batch_size, -1])
    dim = reshape.get_shape()[1].value
    weights = _variable_with_weight_decay('weights', shape=[dim, 384],
                                          stddev=0.04, wd=0.004)
    biases = _variable_on_cpu('biases', [384], tf.constant_initializer(0.1))
    local3 = tf.nn.relu(tf.matmul(reshape, weights) + biases, name=scope.name)
    _activation_summary(local3)
    '''
  with tf.name_scope('local3_visualization'):
    delocal3 = tf.subtract(local3, biases)
    #weights are not invertable
    delocal3 = tf.matmul(delocal3, tf.matrix_inverse(weights))
    delocal3 = tf.reshape(delocal3, [FLAGS.batch_size, 8, 8, 64])
    tf.summary.image("reverse_local3", unpool2(delocal3, kernel2, kernel1), max_outputs=16)
    '''
  # local4
  with tf.variable_scope('local4') as scope:
    weights = _variable_with_weight_decay('weights', shape=[384, 192],
                                          stddev=0.04, wd=0.004)
    biases = _variable_on_cpu('biases', [192], tf.constant_initializer(0.1))
    local4 = tf.nn.relu(tf.matmul(local3, weights) + biases, name=scope.name)
    _activation_summary(local4)

  # linear layer(WX + b),
  # We don't apply softmax here because
  # tf.nn.sparse_softmax_cross_entropy_with_logits accepts the unscaled logits
  # and performs the softmax internally for efficiency.
  with tf.variable_scope('softmax_linear') as scope:
    weights = _variable_with_weight_decay('weights', [192, NUM_CLASSES],
                                          stddev=1/192.0, wd=0.0)
    biases = _variable_on_cpu('biases', [NUM_CLASSES],
                              tf.constant_initializer(0.0))
    softmax_linear = tf.add(tf.matmul(local4, weights), biases, name=scope.name)
    _activation_summary(softmax_linear)

  return softmax_linear


def loss(logits, labels):
  """Add L2Loss to all the trainable variables.

  Add summary for "Loss" and "Loss/avg".
  Args:
    logits: Logits from inference().
    labels: Labels from distorted_inputs or inputs(). 1-D tensor
            of shape [batch_size]

  Returns:
    Loss tensor of type float.
  """
  # Calculate the average cross entropy loss across the batch.
  labels = tf.cast(labels, tf.int64)
  cross_entropy = tf.nn.sparse_softmax_cross_entropy_with_logits(
      labels=labels, logits=logits, name='cross_entropy_per_example')
  cross_entropy_mean = tf.reduce_mean(cross_entropy, name='cross_entropy')
  tf.add_to_collection('losses', cross_entropy_mean)

  # The total loss is defined as the cross entropy loss plus all of the weight
  # decay terms (L2 loss).
  return tf.add_n(tf.get_collection('losses'), name='total_loss')


def _add_loss_summaries(total_loss):
  """Add summaries for losses in CIFAR-10 model.

  Generates moving average for all losses and associated summaries for
  visualizing the performance of the network.

  Args:
    total_loss: Total loss from loss().
  Returns:
    loss_averages_op: op for generating moving averages of losses.
  """
  # Compute the moving average of all individual losses and the total loss.
  loss_averages = tf.train.ExponentialMovingAverage(0.9, name='avg')
  losses = tf.get_collection('losses')
  loss_averages_op = loss_averages.apply(losses + [total_loss])

  # Attach a scalar summary to all individual losses and the total loss; do the
  # same for the averaged version of the losses.
  for l in losses + [total_loss]:
    # Name each loss as '(raw)' and name the moving average version of the loss
    # as the original loss name.
    tf.summary.scalar(l.op.name + ' (raw)', l)
    tf.summary.scalar(l.op.name, loss_averages.average(l))

  return loss_averages_op

def accuracy(logits, labels):
  #labels=tf.convert_
  #labels_tensor.cast(labels_tensor, tf.int64)
  correct_prediction = tf.equal(tf.argmax(logits, 1), labels)
  accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32))
  #top_k_op = tf.nn.in_top_k(logits, labels, 1)
  return accuracy

def train(total_loss, global_step):
  """Train CIFAR-10 model.

  Create an optimizer and apply to all trainable variables. Add moving
  average for all trainable variables.

  Args:
    total_loss: Total loss from loss().
    global_step: Integer Variable counting the number of training steps
      processed.
  Returns:
    train_op: op for training.
  """
  # Variables that affect learning rate.
  num_batches_per_epoch = NUM_EXAMPLES_PER_EPOCH_FOR_TRAIN / FLAGS.batch_size
  decay_steps = int(num_batches_per_epoch * NUM_EPOCHS_PER_DECAY)

  # Decay the learning rate exponentially based on the number of steps.
  lr = tf.train.exponential_decay(INITIAL_LEARNING_RATE,
                                  global_step,
                                  decay_steps,
                                  LEARNING_RATE_DECAY_FACTOR,
                                  staircase=True)
  tf.summary.scalar('learning_rate', lr)

  # Generate moving averages of all losses and associated summaries.
  loss_averages_op = _add_loss_summaries(total_loss)

  # Compute gradients.
  with tf.control_dependencies([loss_averages_op]):
    opt = tf.train.GradientDescentOptimizer(lr)
    grads = opt.compute_gradients(total_loss)

  # Apply gradients.
  apply_gradient_op = opt.apply_gradients(grads, global_step=global_step)

  # Add histograms for trainable variables.
  for var in tf.trainable_variables():
    tf.summary.histogram(var.op.name, var)

  # Add histograms for gradients.
  for grad, var in grads:
    if grad is not None:
      tf.summary.histogram(var.op.name + '/gradients', grad)

  # Track the moving averages of all trainable variables.
  variable_averages = tf.train.ExponentialMovingAverage(
      MOVING_AVERAGE_DECAY, global_step)
  variables_averages_op = variable_averages.apply(tf.trainable_variables())

  with tf.control_dependencies([apply_gradient_op, variables_averages_op]):
    train_op = tf.no_op(name='train')

  return train_op


def maybe_download_and_extract():
  """Download and extract the tarball from Alex's website."""
  dest_directory = FLAGS.data_dir
  if not os.path.exists(dest_directory):
    os.makedirs(dest_directory)
  filename = DATA_URL.split('/')[-1]
  filepath = os.path.join(dest_directory, filename)
  if not os.path.exists(filepath):
    def _progress(count, block_size, total_size):
      sys.stdout.write('\r>> Downloading %s %.1f%%' % (filename,
          float(count * block_size) / float(total_size) * 100.0))
      sys.stdout.flush()
    filepath, _ = urllib.request.urlretrieve(DATA_URL, filepath, _progress)
    print()
    statinfo = os.stat(filepath)
    print('Successfully downloaded', filename, statinfo.st_size, 'bytes.')
  extracted_dir_path = os.path.join(dest_directory, 'cifar-10-batches-bin')
  if not os.path.exists(extracted_dir_path):
    tarfile.open(filepath, 'r:gz').extractall(dest_directory)


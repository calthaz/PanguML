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

# see also https://arxiv.org/pdf/1311.2901.pdf - Matthew D. Zeiler and Rob Fergus
from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import os
import re
import sys

import tensorflow as tf
import read_image

FLAGS = tf.app.flags.FLAGS

# Basic model parameters.
tf.app.flags.DEFINE_integer('batch_size', 128,
                            """Number of images to process in a batch.""")
#tf.app.flags.DEFINE_string('data_dir', '/tmp/cifar10_data',
                           #"""Path to the CIFAR-10 data directory.""")
tf.app.flags.DEFINE_boolean('use_fp16', False,
                            """Train the model using fp16.""")

# Global constants 
IMAGE_SIZE = read_image.IMAGE_SIZE
NUM_CLASSES = read_image.NUM_CLASS
MOVING_AVERAGE_DECAY = 0.9999     # The decay to use for the moving average.

def _activation_summary(x):
  """Helper to create summaries for activations.

  Creates a summary that provides a histogram of activations.
  Creates a summary that measures the sparsity of activations.

  Args:
    x: Tensor
  Returns:
    nothing
  """
  tensor_name = x.op.name
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
  #let me use resize_image to temporarily achieve this, since I can't find a tf function to do it
  unpooled = tf.image.resize_images(pool, [IMAGE_SIZE, IMAGE_SIZE])
  pool1_trans = deconv1(unpooled, kernel1)
  return pool1_trans

def devonv2(output, kernel2, kernel1):
  trans = tf.nn.conv2d_transpose(output, kernel2, output_shape=[FLAGS.batch_size, int(IMAGE_SIZE/2), int(IMAGE_SIZE/2), 64], strides=[1,1,1,1], padding='SAME')
  trans = unpool1(trans, kernel1)
  return trans

def unpool2(pool, kernel2, kernel1):
  #let me use resize_image to temporarily achieve this, since I can't find a tf function to do it
  unpooled = tf.image.resize_images(pool, [int(IMAGE_SIZE/2), int(IMAGE_SIZE/2)])
  pool2_trans = deconv2(unpooled, kernel2, kernel1)
  return pool2_trans

def inference(images):
  """Build the model.
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
    # save 16 activations to summary
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
    
    conv_trans = tf.nn.conv2d_transpose(conv1, kernel1, output_shape=[FLAGS.batch_size, IMAGE_SIZE, IMAGE_SIZE, 3], strides=[1,1,1,1], padding='SAME')
    tf.summary.image("reverse_conv1", conv_trans)
    #find strongest activation here?
    # what is a strongest activation? contrast?
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
    #another way is to simply make 64 copies of the max activation
    #max_filters = tf.concat([max_slice for x in range(64)], axis=3)
    #虽然说这样看起来更有道理，但是图片看起来实在没什么区别
    #reverse conv2d
    max_trans = tf.nn.conv2d_transpose(max_filters, kernel1, output_shape=[FLAGS.batch_size, IMAGE_SIZE, IMAGE_SIZE, 3], strides=[1,1,1,1], padding='SAME')
    tf.summary.image("reverse_max_conv1", max_trans, max_outputs=16)
    #image input: [batch_size, height, width, channels]

  # pool1
  pool1, argmax= tf.nn.max_pool_with_argmax(conv1, ksize=[1, 3, 3, 1], strides=[1, 2, 2, 1],
                         padding='SAME', name='pool1')
  with tf.name_scope('pool1_visualization'):
    #return tf.reshape(pool1, [FLAGS.batch_size, 16*16*64]), tf.reshape(pool1, [FLAGS.batch_size, 16*16*64])
    #let me use resize_image to temporarily achieve this, since I can't find a tf function to do it
    unpooled = tf.image.resize_images(pool1, [IMAGE_SIZE, IMAGE_SIZE])
    pool1_trans = tf.nn.conv2d_transpose(unpooled, kernel1, output_shape=[FLAGS.batch_size, IMAGE_SIZE, IMAGE_SIZE, 3], strides=[1,1,1,1], padding='SAME')
    tf.summary.image("reverse_pool1", pool1_trans, max_outputs=16)
    
  # norm1
  norm1 = tf.nn.lrn(pool1, 4, bias=1.0, alpha=0.001 / 9.0, beta=0.75,
                    name='norm1')

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
    #find strongest activation
    flattened_conv2 = tf.reshape(conv2, [FLAGS.batch_size, int(IMAGE_SIZE/2)**2, 64])#[1,1024,64]
    mean, variance = tf.nn.moments(flattened_conv2, 1)
    max_index = tf.argmax(variance, 1)#[1,64] in, int out
    #remove all other filters and revert
    max_index = tf.squeeze(max_index)
    max_slice = tf.slice(conv2, [0,0,0,max_index], [FLAGS.batch_size,int(IMAGE_SIZE/2),int(IMAGE_SIZE/2),1])
    max_filters = tf.concat([max_slice for x in range(64)], axis=3)
    max_trans = tf.nn.conv2d_transpose(max_filters, kernel2, 
      output_shape=[FLAGS.batch_size, int(IMAGE_SIZE/2), int(IMAGE_SIZE/2), 64], strides=[1,1,1,1], padding='SAME')
    trans = unpool1(max_trans, kernel1)
    tf.summary.image("reverse_max_conv2", trans, max_outputs=16)

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



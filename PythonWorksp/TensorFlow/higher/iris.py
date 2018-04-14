#this script has multiple deprecated features, i.e., Python 2.X sytax, deprecated TF calls
from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import os
import urllib.request

import numpy as np
import tensorflow as tf

# Data sets
IRIS_TRAINING = "iris_training.csv"
IRIS_TRAINING_URL = "http://download.tensorflow.org/data/iris_training.csv"

IRIS_TEST = "iris_test.csv"
IRIS_TEST_URL = "http://download.tensorflow.org/data/iris_test.csv"

def main():
  # If the training and test sets aren't stored locally, download them.
  if not os.path.exists(IRIS_TRAINING):
    #Also note that the urllib.request.urlopen() function in Python 3 is equivalent to urllib2.urlopen() and that urllib.urlopen() has been removed.
    raw = urllib.request.urlopen(IRIS_TRAINING_URL).read()
    #The outfile should be in binary mode.
    with open(IRIS_TRAINING, "wb") as f:
      f.write(raw)

  if not os.path.exists(IRIS_TEST):
    raw = urllib.request.urlopen(IRIS_TEST_URL).read()
    with open(IRIS_TEST, "wb") as f:
      f.write(raw)

  # Load datasets.
  training_set = tf.contrib.learn.datasets.base.load_csv_with_header(
      filename=IRIS_TRAINING,
      target_dtype=np.int,
      features_dtype=np.float32)
  test_set = tf.contrib.learn.datasets.base.load_csv_with_header(
      filename=IRIS_TEST,
      target_dtype=np.int,
      features_dtype=np.float32)

  # Specify that all features have real-value data
  feature_columns = [tf.contrib.layers.real_valued_column("", dimension=4)]

  # Build 3 layer DNN with 10, 20, 10 units respectively.
  classifier = tf.contrib.learn.DNNClassifier(feature_columns=feature_columns,
                                              hidden_units=[10, 20, 10],
                                              n_classes=3,
                                              model_dir="logs/iris_model")
  # Define the training inputs
  def get_train_inputs():
    x = tf.constant(training_set.data)
    y = tf.constant(training_set.target)

    return x, y

  # Fit model.
  classifier.fit(input_fn=get_train_inputs, steps=2000)

  # Define the test inputs
  def get_test_inputs():
    x = tf.constant(test_set.data)
    y = tf.constant(test_set.target)

    return x, y

  # Evaluate accuracy.
  accuracy_score = classifier.evaluate(input_fn=get_test_inputs,
                                       steps=1)["accuracy"]

  print("\nTest Accuracy: {0:f}\n".format(accuracy_score))
'''
WARNING:tensorflow:From D:\Python\Python35\lib\site-packages\tensorflow\contrib\learn\python\learn\estimators\head.py:625: scalar_summary 
(from tensorflow.python.ops.logging_ops) is deprecated and will be removed after 2016-11-30.
Instructions for updating:
Please switch to tf.summary.scalar. Note that tf.summary.scalar uses the node name instead of the tag. 
This means that TensorFlow will automatically de-duplicate summary names based on the scope they are created in. 
Also, passing a tensor or list of tags to a scalar summary op is no longer supported.
2017-07-18 17:04:23.013676: I c:\tf_jenkins\home\workspace\release-win\m\windows-gpu\py\35\tensorflow\core\common_runtime\gpu\gpu_device.cc:1030] 
Creating TensorFlow device (/gpu:0) -> (device: 0, name: GeForce GTX 1080, pci bus id: 0000:01:00.0)
'''
Test Accuracy: 0.966667
  # Classify two new flower samples.
  def new_samples():
    return np.array(
      [[6.4, 3.2, 4.5, 1.5],
       [5.8, 3.1, 5.0, 1.7]], dtype=np.float32)

  predictions = list(classifier.predict(input_fn=new_samples))

  print(
      "New Samples, Class Predictions:    {}\n"
      .format(predictions))
'''
WARNING:tensorflow:From D:\Python\Python35\lib\site-packages\tensorflow\python\util\deprecation.py:347: calling DNNClassifier.predict 
(from tensorflow.contrib.learn.python.learn.estimators.dnn) with outputs=None is deprecated and will be removed after 2017-03-01.
Instructions for updating:
Please switch to predict_classes, or set `outputs` argument.
2017-07-18 17:04:23.915666: I c:\tf_jenkins\home\workspace\release-win\m\windows-gpu\py\35\tensorflow\core\common_runtime\gpu\gpu_device.cc:1030] 
Creating TensorFlow device (/gpu:0) -> (device: 0, name: GeForce GTX 1080, pci bus id: 0000:01:00.0)
New Samples, Class Predictions:    [1, 2]
'''
if __name__ == "__main__":
    main()
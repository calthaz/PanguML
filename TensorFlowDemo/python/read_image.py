'''
from StyleClassifier
'''
import tensorflow as tf 
import numpy as np
from tensorflow.python import debug as tf_debug 

LABEL_SEP = "|||"
image_dic = {}
NUM_CLASS = 8
IMAGE_SIZE = 64
TRAIN_LABEL_FILE = "../training-materials/ready/train/tf-images-with-labels.txt"
EVAL_LABEL_FILE = "../training-materials/ready/eval/tf-images-with-labels.txt"
NUM_EXAMPLES_PER_EPOCH_FOR_TRAIN = 500
NUM_EXAMPLES_PER_EPOCH_FOR_EVAL = 100

def read_labels_dict(filename):
	with open(filename, encoding='utf8') as f: 
		lines = [line.rstrip('/\n') for line in f]
		for x in range(len(lines)):
			if(lines[x].find(LABEL_SEP)):
				path, _, label = lines[x].partition(LABEL_SEP)
				image_dic[path]=label


def get_num_examples():
	read_labels_dict(TRAIN_LABEL_FILE)
	return len(list(image_dic.keys()))

def distorted_inputs( batch_size):
	"""Construct distorted input for CIFAR training using the Reader ops.

  Args:
	data_dir: Path to the CIFAR-10 data directory.
	batch_size: Number of images per batch.

  Returns:
	images: Images. 4D tensor of [batch_size, IMAGE_SIZE, IMAGE_SIZE, 3] size.
	labels: Labels. 1D tensor of [batch_size] size.
  """
	image_list, label_list = read_labeled_image_list(TRAIN_LABEL_FILE)
	for f in image_list:
		if not tf.gfile.Exists(f):
			raise ValueError('Failed to find file: ' + f)


	images = tf.convert_to_tensor(image_list, dtype=tf.string)
	labels = tf.convert_to_tensor(label_list, dtype=tf.int64)
	
	# Makes an input queue, produces the filenames to read and labels along with them
	input_queue = tf.train.slice_input_producer([images, labels],
												#num_epochs=num_epochs,
												shuffle=True)

	image, label = read_images_from_disk(input_queue)

	reshaped_image = tf.cast(image, tf.float32)

	height = IMAGE_SIZE
	width = IMAGE_SIZE

	# Image processing for training the network. Note the many random
	# distortions applied to the image.
	distorted_image = tf.image.resize_images(reshaped_image, [height, width])
	#note: per_image! input_queue is a list of single examples, so image is also a single image, 
	#shuffle_batch(enqueue_many=False) is still appropriate. and so on

	# Randomly flip the image horizontally.
	distorted_image = tf.image.random_flip_left_right(distorted_image)

	# Because these operations are not commutative, consider randomizing
	# the order their operation.
	# NOTE: since per_image_standardization zeros the mean and makes
	# the stddev unit, this likely has no effect see tensorflow#1458.
	distorted_image = tf.image.random_brightness(distorted_image,
											   max_delta=63)
	distorted_image = tf.image.random_contrast(distorted_image,
											 lower=0.2, upper=1.8)

	# Subtract off the mean and divide by the variance of the pixels.
	#mean = 0 afterwards
	float_image = tf.image.per_image_standardization(distorted_image)

	# Set the shapes of tensors.
	float_image.set_shape([height, width, 3])

	# Ensure that the random shuffling has good mixing properties.
	min_fraction_of_examples_in_queue = 0.4
	min_queue_examples = int(NUM_EXAMPLES_PER_EPOCH_FOR_TRAIN *
						   min_fraction_of_examples_in_queue)
	print ('Filling queue with %d images before starting to train. '
		 'This will take a few minutes.' % min_queue_examples)

	# Generate a batch of images and labels by building up a queue of examples.
	return _generate_image_and_label_batch(float_image, label,
										 min_queue_examples, batch_size,
										 shuffle=True)

def inputs(eval_data, batch_size):
	"""Construct input for CIFAR evaluation using the Reader ops.
		 no random constrast, random brightness, etc       
	Args:
	eval_data: bool, indicating if one should use the train or eval data set.
	data_dir: Path to the CIFAR-10 data directory.
	batch_size: Number of images per batch.

	Returns:
	images: Images. 4D tensor of [batch_size, IMAGE_SIZE, IMAGE_SIZE, 3] size.
	labels: Labels. 1D tensor of [batch_size] size.
	"""
	if not eval_data:
		image_list, label_list = read_labeled_image_list(TRAIN_LABEL_FILE)
		num_examples_per_epoch = NUM_EXAMPLES_PER_EPOCH_FOR_TRAIN
	else:
		image_list, label_list = read_labeled_image_list(EVAL_LABEL_FILE)
		num_examples_per_epoch = NUM_EXAMPLES_PER_EPOCH_FOR_EVAL

	for f in image_list:
		if not tf.gfile.Exists(f):
			raise ValueError('Failed to find file: ' + f)

	images = tf.convert_to_tensor(image_list, dtype=tf.string)
	labels = tf.convert_to_tensor(label_list, dtype=tf.int64)#todo use 64 from the start?

	num_examples_per_epoch = NUM_EXAMPLES_PER_EPOCH_FOR_EVAL

	# Makes an input queue
	input_queue = tf.train.slice_input_producer([images, labels],
												#num_epochs=num_epochs,
												shuffle=True)

	image, label = read_images_from_disk(input_queue)

	reshaped_image = tf.cast(image, tf.float32)

	height = IMAGE_SIZE
	width = IMAGE_SIZE

	# Image processing for evaluation.
	# no distortions!
	resized_image = tf.image.resize_images(reshaped_image, [height, width])
	float_image = tf.image.per_image_standardization(resized_image)
	float_image.set_shape([height, width, 3])

	# Ensure that the random shuffling has good mixing properties.
	min_fraction_of_examples_in_queue = 0.4
	min_queue_examples = int(num_examples_per_epoch *
						   min_fraction_of_examples_in_queue)

	# Generate a batch of images and labels by building up a queue of examples.
	return _generate_image_and_label_batch(float_image, label,
										 min_queue_examples, batch_size,
										 shuffle=False)

def eval_all_inputs(eval_data):
	sample_count = get_num_examples()
	return inputs(eval_data, sample_count)

def _generate_image_and_label_batch(image, label, min_queue_examples,
									batch_size, shuffle):
	"""Construct a queued batch of images and labels.

	Args:
	image: 3-D Tensor of [height, width, 3] of type.float32.
	label: 1-D Tensor of type.int32
	min_queue_examples: int32, minimum number of samples to retain
	  in the queue that provides of batches of examples.
	batch_size: Number of images per batch.
	shuffle: boolean indicating whether to use a shuffling queue.

	Returns:
	images: Images. 4D tensor of [batch_size, height, width, 3] size.
	labels: Labels. 1D tensor of [batch_size] size.
	"""
	# Create a queue that shuffles the examples, and then
	# read 'batch_size' images + labels from the example queue.
	num_preprocess_threads = 16
	if shuffle:
		images, label_batch = tf.train.shuffle_batch(
			[image, label],
			batch_size=batch_size,
			num_threads=num_preprocess_threads,
			capacity=min_queue_examples + 3 * batch_size,
			min_after_dequeue=min_queue_examples)
	else:
		images, label_batch = tf.train.batch(
			[image, label],
			batch_size=batch_size,
			num_threads=num_preprocess_threads,
			capacity=min_queue_examples + 3 * batch_size)

	# Display the training images in the visualizer.
	tf.summary.image('images', images)

	return images, tf.reshape(label_batch, [batch_size])

#https://stackoverflow.com/questions/34340489/tensorflow-read-images-with-labels
def read_labeled_image_list(image_list_file):
	"""Reads a .txt file containing pathes and labeles
	Args:
	   image_list_file: a .txt file with one /path/to/image per line
	   label: optionally, if set label will be pasted after each line
	Returns:
	   List with all filenames in file image_list_file
	"""
	f = open(image_list_file, 'r', encoding='utf8')
	filenames = []
	labels = []
	for line in f:
		line = line.rstrip('\n')

		filename, _, label = line.partition(LABEL_SEP)#line[:-1].split(LABEL_SEP)
		filenames.append(filename)
		labels.append(int(label))
		
	return filenames, labels

def read_images_from_disk(input_queue):
	"""Consumes a single filename and label as a ' '-delimited string.
	Args:
	  filename_and_label_tensor: A scalar string tensor.
	Returns:
	  Two tensors: the decoded image, and the string label.
	"""
	label = input_queue[1]
	file_contents = tf.read_file(input_queue[0])
	example = tf.image.decode_jpeg(file_contents, channels=3)
	return example, label


import tensorflow as tf 
import numpy as np
from tensorflow.python import debug as tf_debug 

LABEL_SEP = "[*v*]"
image_dic = {}
IMAGE_SIZE = 128
LABEL_FILE = r"..\furniture\bed\tf-images-with-labels.txt"
NUM_EXAMPLES_PER_EPOCH_FOR_TRAIN = 500
NUM_EXAMPLES_PER_EPOCH_FOR_EVAL = 100

def read_labels_dict(filename):
	with open(filename) as f: 
		lines = [line.rstrip('/\n') for line in f]
		for x in range(len(lines)):
			if(lines[x].find(LABEL_SEP)):
				path, _, label = lines[x].partition(LABEL_SEP)
			#有点像 find()和 split()的结合体,从 str 出现的第一个位置起,
			#把 字 符 串 string 分 成 一 个 3 元 素 的 元 组 (string_pre_str,str,string_post_str),
			#如果 string 中不包含str 则 string_pre_str == string.
				image_dic[path]=label
			else:
				pass

def get_num_classes():
	return 6

def get_num_examples():
	read_labels_dict(LABEL_FILE)
	return len(list(image_dic.keys()))
'''
def read_image(filename_queue):
	result = Object()

	# Read a record, getting filenames from the filename_queue.  No
	# header or footer in the CIFAR-10 format, so we leave header_bytes
	# and footer_bytes at their default of 0.
	reader = tf.WholeFileReader()
	result.key, value = reader.read(filename_queue)

	# Convert from a string to a vector of uint8 that is record_bytes long.
	#record_bytes = tf.decode_raw(value, tf.uint8)
	img_orig = tf.image.decode_jpeg(value)

	# The first bytes represent the label, which we convert from uint8->int32.
	#result.label = tf.cast(tf.strided_slice(record_bytes, [0], [label_bytes]), tf.int32)
	#result.label = 3 #todo

	#result.image = tf.image.resize_images(img_orig, [64, 64])
	#result.image.set_shape((64, 64, 3))

	return result
'''
def distorted_inputs( batch_size):
	"""Construct distorted input for CIFAR training using the Reader ops.

  Args:
	data_dir: Path to the CIFAR-10 data directory.
	batch_size: Number of images per batch.

  Returns:
	images: Images. 4D tensor of [batch_size, IMAGE_SIZE, IMAGE_SIZE, 3] size.
	labels: Labels. 1D tensor of [batch_size] size.
  """
	#read_labels_dict(r"D:\PythonWorksp\TensorFlow\furniture\bed\tf-labels.txt")
	image_list, label_list = read_labeled_image_list(LABEL_FILE)
	for f in image_list:
		if not tf.gfile.Exists(f):
			raise ValueError('Failed to find file: ' + f)

	#print(label_list)

	images = tf.convert_to_tensor(image_list, dtype=tf.string)
	labels = tf.convert_to_tensor(label_list, dtype=tf.int64)
	
	print(labels)
	# Makes an input queue
	input_queue = tf.train.slice_input_producer([images, labels],
												#num_epochs=num_epochs,
												shuffle=True)

	image, label = read_images_from_disk(input_queue)

	print(label)
	# Create a queue that produces the filenames to read.
	#filename_queue = tf.train.string_input_producer(filenames)

	# Read examples from files in the filename queue.
	#read_input = read_image(filename_queue)
	reshaped_image = tf.cast(image, tf.float32)

	height = IMAGE_SIZE
	width = IMAGE_SIZE

	# Image processing for training the network. Note the many random
	# distortions applied to the image.

	# Randomly crop a [height, width] section of the image.
	# distorted_image = tf.random_crop(reshaped_image, [height, width, 3])
	distorted_image = tf.image.resize_images(reshaped_image, [height, width])

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
	float_image = tf.image.per_image_standardization(distorted_image)

	# Set the shapes of tensors.
	float_image.set_shape([height, width, 3])
	#label.set_shape([1])#todo

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

	Args:
	eval_data: bool, indicating if one should use the train or eval data set.
	data_dir: Path to the CIFAR-10 data directory.
	batch_size: Number of images per batch.

	Returns:
	images: Images. 4D tensor of [batch_size, IMAGE_SIZE, IMAGE_SIZE, 3] size.
	labels: Labels. 1D tensor of [batch_size] size.
	"""
	image_list, label_list = read_labeled_image_list(LABEL_FILE)
	for f in image_list:
		if not tf.gfile.Exists(f):
			raise ValueError('Failed to find file: ' + f)

	#print(label_list)

	images = tf.convert_to_tensor(image_list, dtype=tf.string)
	labels = tf.convert_to_tensor(label_list, dtype=tf.int64)#todo use 64 from the start?

	print(labels)

	num_examples_per_epoch = NUM_EXAMPLES_PER_EPOCH_FOR_EVAL

	# Makes an input queue
	input_queue = tf.train.slice_input_producer([images, labels],
												#num_epochs=num_epochs,
												shuffle=True)

	image, label = read_images_from_disk(input_queue)

	print(label)
	# Create a queue that produces the filenames to read.
	#filename_queue = tf.train.string_input_producer(filenames)

	# Read examples from files in the filename queue.
	#read_input = read_image(filename_queue)
	reshaped_image = tf.cast(image, tf.float32)

	height = IMAGE_SIZE
	width = IMAGE_SIZE

	# Image processing for evaluation.
	# Crop the central [height, width] of the image. no, this shall not work
	resized_image = tf.image.resize_images(reshaped_image, [height, width])

	# Subtract off the mean and divide by the variance of the pixels.
	float_image = tf.image.per_image_standardization(resized_image)
	#note per_image! input_queue is a list of single examples, so image is also a single image, 
	#shuffle_batch(enqueue_many=False) is still appropriate. and so on

	# Set the shapes of tensors.
	float_image.set_shape([height, width, 3])
	#label.set_shape([1])#todo

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
'''
read_labels_dict(r"D:\PythonWorksp\TensorFlow\furniture\bed\tf-labels.txt")
filename_queue = tf.train.string_input_producer(list(image_dic.keys()), shuffle=True) #  list of files to read
#file_list = tf.train.match_filenames_once(r"D:\PythonWorksp\TensorFlow\furniture\bed\baby-bed\*.jpg")
#filename_queue = tf.train.string_input_producer(file_list)

reader = tf.WholeFileReader()

#try:
key, value = reader.read(filename_queue)
my_img_orig = tf.image.decode_jpeg(value) # use png or jpg decoder based on your files.
#except InvalidArgumentError:

my_img = tf.image.resize_images(my_img_orig, [IMAGE_SIZE, IMAGE_SIZE])
my_img.set_shape((IMAGE_SIZE, IMAGE_SIZE, 3))

init_op = tf.global_variables_initializer()
with tf.Session() as sess:
	#sess = tf_debug.LocalCLIDebugWrapperSession(sess)
	sess.run(init_op)
	# Start populating the filename queue.
	#print(file_count)
	coord = tf.train.Coordinator()
	threads = tf.train.start_queue_runners(sess=sess, coord=coord)

	try:
		while not coord.should_stop():
			for i in range(len(image_dic.keys())): #length of your filename list len(file_list)??
				image = my_img.eval() #here is your image Tensor :) 
				print(image.shape)
	except tf.errors.InvalidArgumentError as error:
		print(error)
	finally: 
		coord.request_stop()
	coord.join(threads)
'''
#https://stackoverflow.com/questions/34340489/tensorflow-read-images-with-labels
def read_labeled_image_list(image_list_file):
	"""Reads a .txt file containing pathes and labeles
	Args:
	   image_list_file: a .txt file with one /path/to/image per line
	   label: optionally, if set label will be pasted after each line
	Returns:
	   List with all filenames in file image_list_file
	"""
	f = open(image_list_file, 'r')
	filenames = []
	labels = []
	for line in f:
		line = line.rstrip('\n')

		filename, _, label = line.partition(LABEL_SEP)#line[:-1].split(LABEL_SEP)
		filenames.append(filename)
		labels.append(int(label))
		#print (filename+LABEL_SEP+":) "+label)
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
'''
# Reads pfathes of images together with their labels
image_list, label_list = read_labeled_image_list(filename)

images = ops.convert_to_tensor(image_list, dtype=dtypes.string)
labels = ops.convert_to_tensor(label_list, dtype=dtypes.int32)

# Makes an input queue
input_queue = tf.train.slice_input_producer([images, labels],
											#num_epochs=num_epochs,
											shuffle=True)

image, label = read_images_from_disk(input_queue)

# Optional Preprocessing or Data Augmentation
# tf.image implements most of the standard image augmentation
#image = preprocess_image(image)
#label = preprocess_label(label)

# Optional Image and Label Batching
image_batch, label_batch = tf.train.batch([image, label],
										  batch_size=batch_size)
'''

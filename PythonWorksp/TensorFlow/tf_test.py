import tensorflow as tf 

x = tf.constant(1.0, shape=[100,10])
y = tf.constant(1.0, shape=[100,10])

cross_entropy = tf.reduce_mean(
	tf.nn.softmax_cross_entropy_with_logits(logits = x, labels = y))

sess=tf.InteractiveSession()
print(sess.run(cross_entropy))

b = False
print("1E-3: %.e, 1e-3: %.2e; bool %d"%(1E-3, 1e-3, b));

class Fish:
	kingdom = 'animal'
	def __init__(self, species, age=1, name=''):
		self.species = species
		self.age = age
		self.name = name

def age_print(fish):
	fish.age += 1
	print('I am a %d-year-old %s named %s. A %s'%(fish.age, fish.species,fish.name,fish.kingdom))

fish = Fish('goldfish', name='goldy')
#fish2 = Fish('starfish', name='starry')

age_print(fish)

for i in range(5):
	fish = Fish("cod", age=4)
	age_print(fish)

age_print(fish)

#https://www.tensorflow.org/tutorials/using_gpu
# Creates a graph.
a = tf.constant([1.0, 2.0, 3.0, 4.0, 5.0, 6.0], shape=[2, 3], name='a')
b = tf.constant([1.0, 2.0, 3.0, 4.0, 5.0, 6.0], shape=[3, 2], name='b')
c = tf.matmul(a, b)
# Creates a session with log_device_placement set to True.
#sess = tf.Session(config=tf.ConfigProto(log_device_placement=True))
# Runs the op.
print(sess.run(c))

# Creates a graph.
with tf.device('/cpu:0'):
  a = tf.constant([1.0, 2.0, 3.0, 4.0, 5.0, 6.0], shape=[2, 3], name='a')
  b = tf.constant([1.0, 2.0, 3.0, 4.0, 5.0, 6.0], shape=[3, 2], name='b')
c = tf.matmul(a, b)
# Creates a session with log_device_placement set to True.
#sess = tf.Session(config=tf.ConfigProto(log_device_placement=True))
# Runs the op.
print(sess.run(c))

# Creates a graph.
with tf.device('/gpu:0'):
  a = tf.constant([1.0, 2.0, 3.0, 4.0, 5.0, 6.0], shape=[2, 3], name='a')
  b = tf.constant([1.0, 2.0, 3.0, 4.0, 5.0, 6.0], shape=[3, 2], name='b')
  c = tf.matmul(a, b)
# Creates a session with log_device_placement set to True.
#sess = tf.Session(config=tf.ConfigProto(log_device_placement=True))
# Runs the op.
print(sess.run(c))

f = tf.constant([[7,0],[2,4],[0,1],[1,3]])
i = tf.constant([1,2,3,4,5,6], shape=[2,3])
conv1 = tf.matmul(f,i)
reverse = tf.matmul(tf.transpose(f), conv1)
sess = tf.Session(config=tf.ConfigProto(log_device_placement=True))
# Runs the op.
print(sess.run(conv1))
print(sess.run(reverse))

index = tf.argmax(i, 0)
zeros = tf.zeros([4, index])
concat = tf.concat([zeros, i], axis = 1)
print(concat.eval())
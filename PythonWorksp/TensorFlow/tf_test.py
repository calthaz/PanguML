import tensorflow as tf 

x = tf.constant(1.0, shape=[100,10])
y = tf.constant(1.0, shape=[100,10])

cross_entropy = tf.reduce_mean(
	tf.nn.softmax_cross_entropy_with_logits(logits = x, labels = y))

sess=tf.InteractiveSession()

b = False

print(sess.run(cross_entropy))

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






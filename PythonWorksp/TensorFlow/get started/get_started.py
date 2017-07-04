import tensorflow as tf

#Each node takes zero or more tensors as inputs and produces a tensor as an output. One type of node is a constant. 
#Like all TensorFlow constants, it takes no inputs, and it outputs a value it stores internally.
node1 = tf.constant(3.0, tf.float32)
node2 = tf.constant(4.0) # also tf.float32 implicitly
print(node1, node2)
#To actually evaluate the nodes, we must run the computational graph within a session. A session encapsulates the control and state of the TensorFlow runtime.
sess = tf.Session()
print(sess.run([node1, node2]))

node3 = tf.add(node1, node2)
print("node3: ", node3)
print("sess.run(node3): ",sess.run(node3))

a = tf.placeholder(tf.float32)
b = tf.placeholder(tf.float32)
adder_node = a + b  # + provides a shortcut for tf.add(a, b)
#A graph can be parameterized to accept external inputs, known as placeholders. A placeholder is a promise to provide a value later.

print(sess.run(adder_node, {a: 3, b:4.5}))
print(sess.run(adder_node, {a: [1,3], b: [2, 4]}))

add_and_triple = adder_node * 3.
print(sess.run(add_and_triple, {a: 3, b:4.5}))


print("Test ended. My first model below")
#------------------------------------------------------------------------setting up a real model-----------------------------------------------------------------------------------
W = tf.Variable([.3], tf.float32)
b = tf.Variable([-.3], tf.float32)
#print("initial W and b:", sess.run([W, b]))Aha, not initialized here. causes exceptions
#Constants are initialized when you call tf.constant, and their value can never change. By contrast, variables are not initialized when you call tf.Variable.

# training data
x_train = [1,2,3,4]
y_train = [0,-1,-2,-3]

x = tf.placeholder(tf.float32)
linear_model = W * x + b

#Constants are initialized when you call tf.constant, and their value can never change. 
#By contrast, variables are not initialized when you call tf.Variable. 
#To initialize all the variables in a TensorFlow program, you must explicitly call a special operation as follows:

init = tf.global_variables_initializer()
sess.run(init)
print("initial W and b:", sess.run([W, b]))

#It is important to realize init is a handle to the TensorFlow sub-graph that initializes all the global variables. 
#Until we call sess.run, the variables are uninitialized.
print("print the results from the primitive linear model:")
print(sess.run(linear_model, {x:x_train}))
#We'll use a standard loss model for linear regression, 
#which sums the squares of the deltas between the current model and the provided data. 
#linear_model - y creates a vector where each element is the corresponding example's error delta. 

y = tf.placeholder(tf.float32)
squared_deltas = tf.square(linear_model - y)
loss = tf.reduce_sum(squared_deltas)

print("print the squared_deltas of the primitive linear model:")
print(sess.run(loss, {x:x_train, y:y_train}))

fixW = tf.assign(W, [-1.])
fixb = tf.assign(b, [1.])
#print("Altered W and b??:", sess.run([W, b])) no, W and b are not altered.
sess.run([fixW, fixb])
print("print the squared_deltas of the ideal linear model:")
print(sess.run(loss, {x:x_train, y:y_train}))

optimizer = tf.train.GradientDescentOptimizer(0.01)
train = optimizer.minimize(loss)
sess.run(init) # reset values to incorrect defaults.
for i in range(1000):
  sess.run(train, {x:x_train, y:y_train})

print(sess.run([W, b]))
from tensorflow.examples.tutorials.mnist import input_data
mnist = input_data.read_data_sets("MNIST_data/", one_hot=True)
import tensorflow as tf

x = tf.placeholder(tf.float32, [None, 784])
W = tf.Variable(tf.zeros([784, 10]))
b = tf.Variable(tf.zeros([10]))

tf.summary.histogram("weights", W)
tf.summary.histogram('biases', b)


#Notice that W has a shape of [784, 10] 
#because we want to multiply the 784-dimensional image vectors by it to produce 10-dimensional vectors of evidence for the difference classes. 
#b has a shape of [10] so we can add it to the output.

#y = tf.nn.softmax(tf.matmul(x, W) + b)#what does softmax do here? 
y = tf.matmul(x, W) + b

#First, we multiply x by W with the expression tf.matmul(x, W). 
#This is flipped from when we multiplied them in our equation, where we had Wx, as a small trick to deal with x being a 2D tensor with multiple inputs. 
#We then add b, and finally apply tf.nn.softmax.

#Here softmax is serving as an "activation" or "link" function, shaping the output of our linear function into the form we want 
#-- in this case, a probability distribution over 10 cases. You can think of it as converting tallies of evidence into probabilities of our input being in each class. It's defined as:
#softmax(x)=normalize(exp(x))
#If you expand that equation out, you get:
#softmax(x)i=exp(xi)/sumj(exp(xj))
#But it's often more helpful to think of softmax the first way: exponentiating its inputs and then normalizing them. 

y_ = tf.placeholder(tf.float32, [None, 10])
#cross_entropy = tf.reduce_mean(-tf.reduce_sum(y_ * tf.log(y), reduction_indices=[1]))

cross_entropy = tf.reduce_mean(
      tf.nn.softmax_cross_entropy_with_logits(labels=y_, logits=y))
tf.summary.scalar('cross_entropy', cross_entropy)

train_step = tf.train.GradientDescentOptimizer(0.5).minimize(cross_entropy)

correct_prediction = tf.equal(tf.argmax(y,1), tf.argmax(y_,1))
accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32))

tf.summary.scalar('accuracy', accuracy)



sess = tf.InteractiveSession()
#We first have to create an operation to initialize the variables we created:

tf.global_variables_initializer().run()
#Let's train -- we'll run the training step 1000 times!

writer = tf.summary.FileWriter("/tmp/tensorflow/mnist/logs/beginners_linear");
writer.add_graph(sess.graph);

merged_summary = tf.summary.merge_all()
writer = tf.summary.FileWriter('/tmp/tensorflow/mnist/logs/beginners_linear')
for i in range(1000):
  batch_xs, batch_ys = mnist.train.next_batch(100)
  sess.run(train_step, feed_dict={x: batch_xs, y_: batch_ys})
  if(i%10==0):
    s = sess.run(merged_summary, feed_dict={x: batch_xs, y_:batch_ys})
    writer.add_summary(s,i)



print(sess.run(accuracy, feed_dict={x: mnist.test.images, y_: mnist.test.labels}))
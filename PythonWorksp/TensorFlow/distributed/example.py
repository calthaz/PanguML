import tensorflow as tf

#in-graph replication
#large graph
with tf.device("/job:ps/task:0/cpu:0"):
	W1 = tf.get_variable('weights_1', [784, 100])
	b1 = tf.get_variable('biases_1', [100])
	W1 = tf.get_variable('weights_2', [100,10])
	b1 = tf.get_variable('biases_2', [10])
inputs = tf.split(input, num_workers, 0)
outputs = []
for i in range(num_workers):
	with tf.device("/job:worker/task:%d/gpu:0"%i):
		outputs.append(tf.matmul(input[i], W)+b)
loss = some_func(outputs)

#between-graph replication
#smaller graph, independently
#client
with tf.device("/job:ps/task:0/cpu:0"):
	W = tf.Variable(...)
	b = tf.Variable(...)

with tf.device("/job:worker/task:0/gpu:0"):
	outputs.append(tf.matmul(input[i], W)+b)
	loss = some_func(outputs)

#client
with tf.device("/job:ps/task:0/cpu:0"):
	W = tf.Variable(...)
	b = tf.Variable(...)

with tf.device("/job:worker/task:1/gpu:0"):
	outputs.append(tf.matmul(input[i], W)+b)
	loss = some_func(outputs)

with tf.device(tf.train.replica_device_setter(ps_tasks=3)):
	#assign tasks in round-robin fashion
	W1 = tf.get_variable('weights_1', [784, 100])
	b1 = tf.get_variable('biases_1', [100])
	W1 = tf.get_variable('weights_2', [100,10])
	b1 = tf.get_variable('biases_2', [10])

greedy = tf.contrib.training.GreedyLoadBalancingStrategy(_)
with tf.device(tf.train.replica_device_setter(ps_tasks=3, ps_strategy=greedy)):
	#assign tasks in round-robin fashion
	W1 = tf.get_variable('weights_1', [784, 100])
	b1 = tf.get_variable('biases_1', [100])
	W1 = tf.get_variable('weights_2', [100,10])
	b1 = tf.get_variable('biases_2', [10])

	embedding = tf.get_variable(embedding, [1000000000, 20], partitioner = tf.fixed_size_partitioner(3))

saver = tf.train.Saver(sharded=True)
#each PS task writed in parallel, this is not by default

#distributed code for a worker task
cluster = tf.train.ClusterSpec({"workers":["192.168.0.1:2222", ...],
								"ps":["192.168.1.1:2222", ...]})
#cluster mamager called Borg
server = tf.train.Server(cluster, job_name="worker", task_index=0)
#server represents a particular task
with tf.Session(server.target) as sess:
	...
	if is_chief and step % 1000 ==0:
		saver.save(sess, "path/to/dir")
		#"gs://mrry/model/..." Google cloud storage, hadoop, Cloud ML
		#easier to evaluate

#distributed code for a PS task
cluster = tf.train.ClusterSpec({"workers":["192.168.0.1:2222", ...],
								"ps":["192.168.1.1:2222", ...]})
server = tf.train.Server(cluster, job_name="ps", task_index=0)
#Wait for incoming connection
server.join()

#worker 0 is the chief
#initializing, writing checkpoints, generating summary

#configuration management
#apache zookeeper etcd choose a chief by leader election
#so when the chief fails, there will be another chief

#MonitoredTrainingSession automates the recovery process

server = tf.train.Server(...)
is_chief = FLAGS.task_index == 0
#automatically initializes and/or restores variables before returning
#if not a chief it waits untill the chief does his work
with tf.train.MonitoredTrainingSession(server.target, is_chief) as sess:
	while not sess.should_stop():
		sess.run(train_op)
# www - root directory of an apache server
These are all TensorFlow demo pages or other template pages to support my development. 
## index.php 
index.php contains demos for 4 different classifiers.  
Workflow:\
*JavaScript => php => Java Servelet(coordinator) => php => Java => TensorFlow(jni) => Java => php =>JavaScript*\
The workflow is quite complicated because 1) I didn't know that JavaScript can post to python cgi directly; 2)We need a daemon 
process that acts as a coordinator to distribute workload among a group of servers. 
## MNIST/index.php 
MNIST/index.php contains a demo in which OpenCV and TF work together to recognize digits.\
This time I use python cgi and they are much easier to debug. 
## tensorflow/
This directory contains tensorflow related materials, such as jars, checkpoints, frozen_graphs, python files, tensorflow native libraries. 
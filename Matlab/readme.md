# [Deformed Lattice Detection](http://vision.cse.psu.edu/research/deformedLattice/Deformed_Lattice_Detection.html)
Authors: Minwoo Park, Kyle Brocklehurst, Robert T. Collins, and Yanxi Liu.
the package is downloaded at\
http://vision.cse.psu.edu/research/deformedLattice/Deformed_Lattice_Detection.html    
I have edited some matlab files and DEMO1, extended it to DEMO2.   
Run DEMO1 or DEMO2 in MATLAB to understand what it does. 

## 原来的readme
Everything begins with \
\
"compile_CPPsMAC.m"\
\
==========================1================================\
If you are using MAC please note that you need to set proper path to opencv library  (\**.dylib).\
\
If you are using PC please note that you need to set proper path to opencv library  (\**.lib) and you also put corresponding opencv \**.dll in the same directory.\
\
==========================2================================\
Open "MatlabToOpenCV.h" comment line3 if you use PC.\
\
==========================3================================\
Compile the C++ source by run "compile_CPPsMAC.m".\
\
==========================4================================\
You might want to change some non-compatible C function between MAC and PC according to the error message. \
\
==========================5=================================\
\
IF compile succeed after 4, then you should be able to run the algorithm.\
\
you should be able to run DEMO1.m


## 识别贴图的构想
1. 用C++或Python重写MATLAB
2. 用OpenCV实现三维空间墙面、地面的视角转换，然后再用此算法识别贴图
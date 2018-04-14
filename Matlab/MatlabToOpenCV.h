#pragma once
#include "mex.h"
//#define MAC 0

#ifdef MAC
    #include "/Users/opencv/cv.h"
    #include "/Users/opencv/highgui.h"
    #include "/Users/opencv/cxcore.h"
#else
    #include "opencv/cv.h"
    #include "opencv/highgui.h"
    #include "opencv/cxcore.h"
#endif




class CMatlabToOpenCV
{
public:
	CMatlabToOpenCV(void);
	static void ipl_to_matlab(const IplImage* ipl, mxArray* mxOut);
	static IplImage*  convert_copy(const mxArray* mxIn);
	static IplImage*  convert_copy_DblTo32F(const mxArray* mxIn);
	static IplImage*  convert_copy_DblTo64F(const mxArray* mxIn);
	static IplImage*  create_iplimage(int h,int w,int depth);
	static void       release_iplimage(IplImage* ipl);
	static void  DivS(IplImage* in,float scalar);
	static IplImage*  convert_copy_DblTo8U(const mxArray* mxIn);
	~CMatlabToOpenCV(void);
};

// InverseTPS.cpp : mex-function interface implentation file


#include "mex.h"
#include <math.h>
#include "MexAndCpp.h"
#include "MatlabToOpenCV.h"
#include <time.h>
#include <pthread.h>

#define MAX_THREADS 4
typedef struct
{
	double* pReg_coordsX;
	double* pReg_coordsY;
	double* pDist_coordsX;
	double* pDist_coordsY;
	double* pCX;
	double* pCY;
	double* pDescent_new_xcoords;
	double* pDescent_new_ycoords;
	int nStartIdx;
	int nEndIdx;
	int nNumGoodPeaks;
}THREADPARAM;
double* dist2(double* X1,double* X2,int nXLen,double* C1,double* C2,int nCLen);
void dist2(double* X1,double* X2,int nXLen,double C1,double C2,double* out);

static void*  ThreadProc( void* lpParameter);



THREADPARAM param[MAX_THREADS];
pthread_t hThread[MAX_THREADS];


void mexFunction(int nargout, mxArray *out[], int nargin, const mxArray	*in[])
{  
	srand( (unsigned)time( NULL ) );

	double EPS=2.2204e-016;
	// TODO:  add your function code here
	double* pCX=mxGetPr(in[0]);//mcx  this should be computed from 0 based...
	double* pCY=mxGetPr(in[1]);//mcy   this should be computed from 0 based....
	double* pReg_coordsX=mxGetPr(in[2]);//reg_coords M by 2 matrix   col1  this should be 0 based...
	double* pReg_coordsY=mxGetPr(in[3]);//reg_coords M by 2 matrix   col2 this should be 0 based.....
	double* pDist_coordsX=mxGetPr(in[4]);//dist_coords M by 2 matrix   col2  this should be 0 based...
	double* pDist_coordsY=mxGetPr(in[5]);//dist_coords M by 2 matrix   col2  this should be 0 based.....
	const mxArray* mPrevMapX=in[6];
	const mxArray* mPrevMapY=in[7];
	const mxArray* mOriginalR=in[8];//3 channel color image......how to convert it to ipl????
	//const mxArray* mOriginalG=in[9];//3 channel color image......how to convert it to ipl????
	//const mxArray* mOriginalB=in[10];//3 channel color image......how to convert it to ipl????



	int h=mxGetM(mOriginalR);
	int w=mxGetN(mOriginalR);
	IplImage* iplR=CMatlabToOpenCV::convert_copy_DblTo32F(mOriginalR);
	//IplImage* iplG=CMatlabToOpenCV::convert_copy_DblTo32F(mOriginalG);
	//IplImage* iplB=CMatlabToOpenCV::convert_copy_DblTo32F(mOriginalB);

	IplImage* iplWarpedR=cvCreateImage(cvSize(w,h),IPL_DEPTH_32F,1);
	//IplImage* iplWarpedG=cvCreateImage(cvSize(w,h),IPL_DEPTH_32F,1);
	//IplImage* iplWarpedB=cvCreateImage(cvSize(w,h),IPL_DEPTH_32F,1);

	int nRegCorLen=mxGetM(in[2]);
	int nCxyLen=mxGetM(in[0]);

	int nNumGoodPeaks=nRegCorLen;

	double ss_factor=4;
	int subH=(int)(double)ceil(((double)h)/ss_factor);
	int subW=(int)(double)ceil(((double)w)/ss_factor);

	//mxArray* mWarped_x_coords=mxCreateDoubleMatrix(subH,subW,mxREAL);
	//mxArray* mWarped_y_coords=mxCreateDoubleMatrix(subH,subW,mxREAL);
	IplImage* iplWarped_x_coords=cvCreateImage(cvSize(subW,subH),IPL_DEPTH_32F,1);
	IplImage* iplWarped_y_coords=cvCreateImage(cvSize(subW,subH),IPL_DEPTH_32F,1);

	int extra_cols=w%(int)ss_factor;
	int extra_rows=h%(int)ss_factor;
	double x_nudge=(ss_factor-1)/((double)ceil((double)w/ss_factor)-1);
	double y_nudge=(ss_factor-1)/((double)ceil((double)h/ss_factor)-1);

	
	int nWidthStep=iplWarped_x_coords->widthStep/sizeof(float);
	float* pTmpXCoords=(float*)iplWarped_x_coords->imageData;
	float* pTmpYCoords=(float*)iplWarped_y_coords->imageData;
	double* d2=(double*)malloc(sizeof(double)*nNumGoodPeaks);

	
	for(int yy=0;yy<subH;yy++)
	{		
		//double ty=-4+(yy+1)*(ss_factor+y_nudge);
		
		for(int xx=0;xx<subW;xx++)
		{
			double tx=xx*(ss_factor+x_nudge);//0 based indexing.....
			double ty=yy*(ss_factor+y_nudge);
			dist2(pReg_coordsX,pReg_coordsY,nRegCorLen,tx,ty,d2);
			double sum1=pCX[nNumGoodPeaks]+pCX[nNumGoodPeaks+1]*tx+pCX[nNumGoodPeaks+2]*ty;
			double sum2=0;
			for(int m=0;m<nNumGoodPeaks;m++)
			{
				sum2+=pCX[m]*d2[m]*log(d2[m]+EPS);
			}			
			pTmpXCoords[yy*nWidthStep+xx]=sum1+sum2;

			sum1=pCY[nNumGoodPeaks]+pCY[nNumGoodPeaks+1]*tx+pCY[nNumGoodPeaks+2]*ty;
			sum2=0;
			for(int m=0;m<nNumGoodPeaks;m++)
			{
				sum2+=pCY[m]*d2[m]*log(d2[m]+EPS);
			}			
			pTmpYCoords[yy*nWidthStep+xx]=sum1+sum2;
		}
		
	}
	free(d2);
	CvPoint2D32f src[3];
	CvPoint2D32f dst[3];
	src[0].x=0;
	src[0].y=0;

	src[1].x=subW-1;
	src[1].y=0;

	src[2].x=subW-1;
	src[2].y=subH-1;

	dst[0].x=0;dst[0].y=0;
	dst[1].x=subW*ss_factor-1;dst[1].y=0;
	dst[2].x=subW*ss_factor-1;dst[2].y=subH*ss_factor-1;

	CvMat* mapping=cvCreateMat(2,3,CV_32FC1);	
	cvGetAffineTransform(src,dst,mapping);	
		


	IplImage* iplBigWarped_x_coords=cvCreateImage(cvSize(subW*ss_factor,subH*ss_factor),IPL_DEPTH_32F,1);
	IplImage* iplBigWarped_y_coords=cvCreateImage(cvSize(subW*ss_factor,subH*ss_factor),IPL_DEPTH_32F,1);
	cvWarpAffine(iplWarped_x_coords,iplBigWarped_x_coords,mapping);
	cvWarpAffine(iplWarped_y_coords,iplBigWarped_y_coords,mapping);
	//cvNamedWindow("test",0);
	//cvShowImage("test",iplBigWarped_x_coords);cvWaitKey(-1);
	//cvShowImage("test",iplBigWarped_y_coords);cvWaitKey(-1);

	cvReleaseMat(&mapping);



	cvSetImageROI(iplBigWarped_x_coords,cvRect(0,0,w,h));
	cvSetImageROI(iplBigWarped_y_coords,cvRect(0,0,w,h));
	cvReleaseImage(&iplWarped_x_coords);
	cvReleaseImage(&iplWarped_y_coords);

	iplWarped_x_coords=cvCreateImage(cvSize(w,h),IPL_DEPTH_32F,1);
	iplWarped_y_coords=cvCreateImage(cvSize(w,h),IPL_DEPTH_32F,1);
	cvCopy(iplBigWarped_x_coords,iplWarped_x_coords);
	cvCopy(iplBigWarped_y_coords,iplWarped_y_coords);

	cvReleaseImage(&iplBigWarped_x_coords);
	cvReleaseImage(&iplBigWarped_y_coords);

	IplImage* iplmapx;
	IplImage* iplmapy;
	const int* dim=mxGetDimensions(mPrevMapX);
	if(dim[2]>0)
	{// I need to aggregate all the mapping....
		//we need to convert mPrevMapX to iplprev_x_coords_warp
		//we need to convert mPrevMapY to iplprev_y_coords_warp

		IplImage* iplPrevMapX=cvCreateImage(cvSize(w,h),IPL_DEPTH_32F,1);
		IplImage* iplPrevMapY=cvCreateImage(cvSize(w,h),IPL_DEPTH_32F,1);
		float* pTmpMapX=(float*)iplPrevMapX->imageData;
		float* pTmpMapY=(float*)iplPrevMapY->imageData;

		int nWidthStep=iplPrevMapX->widthStep/sizeof(float);
		int mh=mxGetM(mPrevMapX);
		int mw=mxGetN(mPrevMapX);
		for(int y=0;y<iplPrevMapX->height;y++)
		{
			for(int x=0;x<iplPrevMapX->width;x++)
			{
				pTmpMapX[y*nWidthStep+x]=CMexAndCpp::GetMA(mPrevMapX,y,x,mh,mw);
				pTmpMapY[y*nWidthStep+x]=CMexAndCpp::GetMA(mPrevMapY,y,x,mh,mw);
			}
		}

		iplBigWarped_x_coords=cvCreateImage(cvSize(w,h),IPL_DEPTH_32F,1);
		iplBigWarped_y_coords=cvCreateImage(cvSize(w,h),IPL_DEPTH_32F,1);
		cvRemap(iplPrevMapX,iplBigWarped_x_coords,iplWarped_x_coords,iplWarped_y_coords,1+8,cvScalar(0));
		cvRemap(iplPrevMapY,iplBigWarped_y_coords,iplWarped_x_coords,iplWarped_y_coords,1+8,cvScalar(0));
		iplmapx=(IplImage*)cvClone(iplBigWarped_x_coords);
		iplmapy=(IplImage*)cvClone(iplBigWarped_y_coords);
		cvReleaseImage(&iplBigWarped_x_coords);
		cvReleaseImage(&iplBigWarped_y_coords);
		cvReleaseImage(&iplPrevMapX);
		cvReleaseImage(&iplPrevMapY);
	}
	else
	{
		iplmapx=(IplImage*)cvClone(iplWarped_x_coords);
		iplmapy=(IplImage*)cvClone(iplWarped_y_coords);		
	}
	cvReleaseImage(&iplWarped_x_coords);
	cvReleaseImage(&iplWarped_y_coords);
	//so far active ipl array is iplmapx, iplmapy
	cvRemap(iplR,iplWarpedR,iplmapx,iplmapy,1+8,cvScalar(-100));
//	cvRemap(iplG,iplWarpedG,iplmapx,iplmapy,1+8,cvScalar(-100));
//	cvRemap(iplB,iplWarpedB,iplmapx,iplmapy,1+8,cvScalar(-100));
	

	
	


	CMatlabToOpenCV::release_iplimage(iplR);
//	CMatlabToOpenCV::release_iplimage(iplG);
//	CMatlabToOpenCV::release_iplimage(iplB);
	
	nWidthStep=iplWarpedR->widthStep;
	//adding noise.....
	for(int y=0;y<h;y++)
	{
		for(int x=0;x<w;x++)
		{
			if(*(float*)(&iplWarpedR->imageData[y*nWidthStep+x*sizeof(float)])<-1)
			{
				*(float*)(&iplWarpedR->imageData[y*nWidthStep+x*sizeof(float)])=((double) rand())/(double)RAND_MAX  ;
			//	*(float*)(&iplWarpedG->imageData[y*nWidthStep+x*sizeof(float)])=((double) rand())/(double)RAND_MAX  ;
			//	*(float*)(&iplWarpedB->imageData[y*nWidthStep+x*sizeof(float)])=((double) rand())/(double)RAND_MAX  ;
			}			
		}
	}//

	double* pDescent_new_xcoords=(double*)malloc(sizeof(double)*nNumGoodPeaks);
	double* pDescent_new_ycoords=(double*)malloc(sizeof(double)*nNumGoodPeaks);
	


	
	int nNumThread=(int)(double)ceil(((double)nNumGoodPeaks)/20.0);
	if( nNumThread>MAX_THREADS)
		nNumThread=MAX_THREADS;
	int nSize=(int)floor(((double)nNumGoodPeaks)/(double)nNumThread);
	for(int ii=0;ii<nNumThread;ii++)
	{
		param[ii].nStartIdx=nSize*ii;
		param[ii].nEndIdx=nSize*(ii+1);
		param[ii].nNumGoodPeaks=nNumGoodPeaks;
		param[ii].pCX=pCX;
		param[ii].pCY=pCY;
		param[ii].pDescent_new_xcoords=pDescent_new_xcoords;
		param[ii].pDescent_new_ycoords=pDescent_new_ycoords;
		param[ii].pDist_coordsX=pDist_coordsX;
		param[ii].pDist_coordsY=pDist_coordsY;
		param[ii].pReg_coordsX=pReg_coordsX;
		param[ii].pReg_coordsY=pReg_coordsY;
	}
	param[nNumThread-1].nEndIdx=nNumGoodPeaks;

	

	for(int i=0;i<nNumThread;i++)
	{
		pthread_create(&hThread[i],NULL,ThreadProc,&param[i]);
	}


//	WaitForMultipleObjects(nNumThread, hThread, TRUE, INFINITE);

	for(int i=0; i<nNumThread; i++)
	{
		if(pthread_join(hThread[i],NULL))
		{
			mexPrintf("error\n");
			exit(1);
		}
	}
	mexPrintf("Thread join all\n");

	out[0]=mxCreateDoubleMatrix(nNumGoodPeaks,2,mxREAL);
	for(int i=0;i<nNumGoodPeaks;i++)
	{
		CMexAndCpp::SetMA(out[0],i,0,pDescent_new_xcoords[i]);
		CMexAndCpp::SetMA(out[0],i,1,pDescent_new_ycoords[i]);
	}

	free(pDescent_new_xcoords);
	free(pDescent_new_ycoords);
	out[1]=mxCreateDoubleMatrix(iplmapx->height,iplmapx->width,mxREAL);
	out[2]=mxCreateDoubleMatrix(iplmapx->height,iplmapx->width,mxREAL);
	out[3]=mxCreateDoubleMatrix(iplmapx->height,iplmapx->width,mxREAL);//R
	//out[4]=mxCreateDoubleMatrix(iplmapx->height,iplmapx->width,mxREAL);//G
	//out[5]=mxCreateDoubleMatrix(iplmapx->height,iplmapx->width,mxREAL);//B
	
	nWidthStep=iplmapx->widthStep;
	int nRWidthStep=iplWarpedR->widthStep;
	for(int y=0;y<iplmapx->height;y++)
	{
		for(int x=0;x<iplmapx->width;x++)
		{
			float* pVal=(float*)&(iplmapx->imageData[y*nWidthStep+x*sizeof(float)]);
			CMexAndCpp::SetMA(out[1],y,x,*pVal);
			pVal=(float*)&(iplmapy->imageData[y*nWidthStep+x*sizeof(float)]);
			CMexAndCpp::SetMA(out[2],y,x,*pVal);
			//For rectified image....
			pVal=(float*)&(iplWarpedR->imageData[y*nRWidthStep+x*sizeof(float)]);
			CMexAndCpp::SetMA(out[3],y,x,*pVal);
			//pVal=(float*)&(iplWarpedG->imageData[y*nRWidthStep+x*sizeof(float)]);
			//CMexAndCpp::SetMA(out[4],y,x,*pVal);
			//pVal=(float*)&(iplWarpedB->imageData[y*nRWidthStep+x*sizeof(float)]);
			//CMexAndCpp::SetMA(out[5],y,x,*pVal);
		}
	}
	//Free all the allocated....
	cvReleaseImage(&iplmapx);
	cvReleaseImage(&iplmapy);

	cvReleaseImage(&iplWarpedR);
//	cvReleaseImage(&iplWarpedG);
//	cvReleaseImage(&iplWarpedB);

	return;
}




static void*  ThreadProc( void* lpParam)
{
		//test_M = (2*search_radius+1)^2;
	//nearby_x_values = zeros(search_radius*2 +1);
	//nearby_y_values = zeros(search_radius*2 +1);
	double EPS=2.2204e-016;
	THREADPARAM* param=(THREADPARAM*)lpParam;
	double* pReg_coordsX=param->pReg_coordsX;
	double* pReg_coordsY=param->pReg_coordsY;
	double* pDist_coordsX=param->pDist_coordsX;
	double* pDist_coordsY=param->pDist_coordsY;
	double* pCX=param->pCX;
	double* pCY=param->pCY;
	double* pDescent_new_xcoords=param->pDescent_new_xcoords;
	double* pDescent_new_ycoords=param->pDescent_new_ycoords;
	int nNumGoodPeaks=param->nNumGoodPeaks;
	int nStartIdx=param->nStartIdx;
	int nEndIdx=param->nEndIdx;
	mexPrintf("Thread start %d\n",nEndIdx);

	double search_radius = 5;
	//search for correct lattice locations..........
	for(int i=nStartIdx;i<nEndIdx;i++)
	{
		bool keepsearching=true;
		double current_x=(double)floor(pReg_coordsX[i]+0.5);//already 0 based...
		double current_y=(double)floor(pReg_coordsY[i]+0.5);// already 0 based...
		double target_sx=pDist_coordsX[i];// already 0 based...
		double target_sy=pDist_coordsY[i];// already 0 based...
		double current_window=64;

		while(keepsearching)
		{
			int nSize=(int)(search_radius*2+1);
			int nSizeXY=nSize*nSize;
			double* xs=(double*)malloc(sizeof(double)*nSizeXY);
			double* ys=(double*)malloc(sizeof(double)*nSizeXY);


			int cnt=0;
			for(int ix=0;ix<2*search_radius+1;ix++)				
			{
				double x=current_x-current_window+ix*(current_window/search_radius);
				for(int iy=0;iy<2*search_radius+1;iy++)
				{
					double y=current_y-current_window+iy*(current_window/search_radius);
					xs[cnt]=x;
					ys[cnt]=y;
					cnt++;
				}
			}

			double* d2=dist2( pReg_coordsX,pReg_coordsY ,nNumGoodPeaks,xs,ys,nSizeXY);

			
			double minerror=1000*1000;

			int nMinIdx=-1;
			for(int xx=0;xx<nSizeXY;xx++)
			{				
				double sum1=pCX[nNumGoodPeaks]+pCX[nNumGoodPeaks+1]*xs[xx]+pCX[nNumGoodPeaks+2]*ys[xx];
				double sum2=0;
				double error=0;
				for(int m=0;m<nNumGoodPeaks;m++)
				{
					sum2+=(pCX[m]*d2[nSizeXY*m+xx]*log(d2[nSizeXY*m+xx]+EPS));
				}			
				error=abs(sum1+sum2-target_sx);

				sum1=pCY[nNumGoodPeaks]+pCY[nNumGoodPeaks+1]*xs[xx]+pCY[nNumGoodPeaks+2]*ys[xx];
				sum2=0;
				for(int m=0;m<nNumGoodPeaks;m++)
				{
					sum2+=pCY[m]*d2[nSizeXY*m+xx]*log(d2[nSizeXY*m+xx]+EPS);
				}			
				error+=abs(sum1+sum2-target_sy);
				//printf("%f\t",error);
				if(error<minerror)
				{
					minerror=error;
					nMinIdx=xx;
				}
			}//
			//printf("\n");
			free(d2);
			free(xs);
			free(ys);
			double xoffset=(int)((float)nMinIdx/(float)nSize);
			double yoffset=(int)(nMinIdx%nSize);
			yoffset=yoffset-search_radius;
			xoffset=xoffset-search_radius;

			yoffset = (current_window/search_radius) * yoffset; //scale the offset based on our current zoom
			xoffset = (current_window/search_radius) * xoffset;

			current_x = current_x + xoffset;
			current_y = current_y + yoffset;

			current_window = current_window/2;  //decrease the search range (zoom in)

			if(current_window < .01) //if error is less than 1 hundredth pixel
			{
				keepsearching = false;
			}
		}
		pDescent_new_xcoords[i]=current_x;
		pDescent_new_ycoords[i]=current_y;
	}
	//now return iplmapx and iplmapy using conversion to MATLAB DoubleMatrix...
	//and return rectified_3channel images to matlab
	mexPrintf("Thread Exit %d\n",nEndIdx);
	pthread_exit(NULL);
	
}




void dist2(double* X1,double* X2,int nXLen,double C1,double C2,double* out)
{
	for(int y=0;y<nXLen;y++)
	{		
		out[y]=pow(X1[y]-C1,2)+pow(X2[y]-C2,2);		
	}
}


double* dist2(double* X1,double* X2,int nXLen,double* C1,double* C2,int nCLen)
{
	double* pRet=(double*)malloc(sizeof(double)*nXLen*nCLen);
	for(int y=0;y<nXLen;y++)
	{
		for(int x=0;x<nCLen;x++)
		{
			pRet[y*nCLen+x]=pow(X1[y]-C1[x],2)+pow(X2[y]-C2[x],2);
		}
	}
	return pRet;
}



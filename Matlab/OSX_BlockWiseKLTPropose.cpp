// ProposalTvectors.cpp : mex-function interface implentation file


#include "mex.h"
#include "MatlabToOpenCV.h"

#include "matrix.h"

#define min(a,b) a>=b?b:a


mxArray* GetMC(const mxArray *in,int y,int x);
void SetMC(mxArray *in,int y,int x,mxArray* val,int h,int w);
void SetMA(mxArray *in,int y,int x,double val,int h,int w);

void mexFunction(int nargout, mxArray *out[], int nargin, const mxArray	*in[])
{  	
	const mxArray* str=in[0];
    double  mindistance=mxGetScalar(in[1]);
	double qual=mxGetScalar(in[2]);
	int maxNum=mxGetScalar(in[3]);
	int option=(int)mxGetScalar(in[4]);

	char strImgPath[1024];

	mxGetString(str,strImgPath,1024);
	IplImage* iplInput=cvLoadImage(strImgPath,0);


	float h=iplInput->height;
	float w=iplInput->width;
	int nWidth=50;
	
	int nCnt= ceil(h/nWidth)*ceil(w/nWidth);

	IplImage* eig=cvCreateImage(cvSize(iplInput->width,iplInput->height),IPL_DEPTH_32F,1);
	IplImage* tmp=cvCreateImage(cvSize(iplInput->width,iplInput->height),IPL_DEPTH_32F,1);
	//We want nCnt number of corner to start with......
	
	int nCorner=maxNum*nCnt;
	
	CvPoint2D32f* ptr=(CvPoint2D32f*)mxMalloc(sizeof(CvPoint2D32f)*nCorner*10);


	int nCornerFound=maxNum;
	int nPos=0;
	
	for(int m=0;m<(int)ceil(h/nWidth);m++)
	{
		for(int n=0;n<(int)ceil(w/nWidth);n++)
		{
			int ix=n*nWidth;
			int iy=m*nWidth;
			nCornerFound=maxNum;
			cvSetImageROI(iplInput,cvRect(ix,iy,min(ix+nWidth,w)-ix,min(iy+nWidth,h)-iy));
			cvSetImageROI(eig,cvRect(ix,iy,min(ix+nWidth,w)-ix,min(iy+nWidth,h)-iy));
			cvSetImageROI(tmp,cvRect(ix,iy,min(ix+nWidth,w)-ix,min(iy+nWidth,h)-iy));
			double tmpQual=qual;
			cvGoodFeaturesToTrack( iplInput, eig,tmp,&ptr[nPos],&nCornerFound,tmpQual,mindistance,NULL,3,1,qual);

			if(option==1)
			{
				while(nCornerFound<=maxNum*0.9 && tmpQual >= 0.005)
				{
					tmpQual*=0.8;
					nCornerFound=maxNum;
					cvGoodFeaturesToTrack( iplInput, eig,tmp,&ptr[nPos],&nCornerFound,tmpQual,mindistance,NULL,3,1,tmpQual);
				}
			}
			for(int k=0;k<nCornerFound;k++)
			{
				ptr[nPos+k].x+=ix;
				ptr[nPos+k].y+=iy;
			}
			nPos+=nCornerFound;
			nCornerFound=maxNum;
		}
	}	
	

	out[0]=mxCreateDoubleMatrix(2,nPos,mxREAL);

	for(int i=0;i<nPos;i++)
	{		
		SetMA(out[0],0,i,ptr[i].x+1,2,nPos);
		SetMA(out[0],1,i,ptr[i].y+1,2,nPos);
	}
	mxFree(ptr);
	
	cvReleaseImage(&iplInput);
	cvReleaseImage(&eig);
	cvReleaseImage(&tmp);
	
    return;
}

void SetMA(mxArray *in,int y,int x,double val,int h,int w)
{
	double* pTmp=mxGetPr(in);	
	pTmp[h*x+y]=val;
}


void SetMC(mxArray *in,int y,int x,mxArray* val,int h,int w)
{
	mxArray* pTmp=GetMC(in,y,x);
	if(pTmp==NULL)
		mxSetCell(in,h*x+y,mxDuplicateArray(val));	
	else
	{
		mxDestroyArray(pTmp);
		mxSetCell(in,h*x+y,mxDuplicateArray(val));	
	}
}

mxArray* GetMC(const mxArray *in,int y,int x)
{
	int h=(int)mxGetM(in);
	int w=(int)mxGetN(in);
	mxArray* pTmp=mxGetCell(in,h*x+y);
	return pTmp;
}
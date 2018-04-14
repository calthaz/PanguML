// MSBPLocalAlignByPass.cpp : mex-function interface implentation file

// MSBPLocalAlign.cpp : mex-function interface implentation file


#include "mex.h"
#include <float.h>
#include <math.h>
#define PI  (atan(1.0)*4.0)

#define eps2 2.2204e-016
#define max(A,B) ((A>=B)? (A):(B))
#define min(A,B) ((A<=B)? (A):(B))

typedef struct
{
	double x;
	double y;
}PTR;
typedef struct
{
	mxArray* ret1;
	mxArray* ret2;
	mxArray* ret3;
}mxArrayReturn;

double* GetMSBPMSGObsAbs(const int isCBypass,const PTR* ZeroPtr,int nMSGSize,const mxArray* mLocXT1,const mxArray* mLocC,const mxArray* mLocT2,const mxArray* mInitXT1,const mxArray* mInitC,const mxArray* mInitT2,const mxArray* mOBS,double isMap,double beta,double alpha);


double ComputeCompatibility(PTR curpos1,PTR curpos2, PTR prevpos1,PTR prevpos2,double beta);
double GetObs(const mxArray* imlikely,PTR pt,double alpha);
double GetObs2(const mxArray* imlikely,PTR pt,double alpha);
double TwoDInterpolation(const mxArray* w,double x,double y,int* Trust);

mxArray* update_state(int MAP,double* pB,const PTR* ZeroPtr,int nMSGSize,double* center,double* pr_Belief,double kernelsize,int kdxdy);
mxArray* generatePTS(const PTR* ZeroPtr,double cy,double cx);
int findClosest(double meanx,double meany,mxArray* ptsx,mxArray* ptsy);
int findClosest(double meanx,double meany,double* ptsx,double* ptsy,int nLen);


void SetMC(mxArray *in,int y,int x,mxArray* val);
void SetMA(mxArray *in,int y,int x,double val);
mxArray* GetMC(const mxArray *in,int y,int x);
double GetMA(const mxArray *in,int y,int x);
double GetMA(const mxArray *in,int y,int x,int h,int w);

double GetMax(double* A,int nLen,int* index);
double GetMax(mxArray* mIn,int* pOutY,int* pOutX);
double GetSum(mxArray* mIn);
double GetSum(double* pIn,int nLen);



void Selfmultiplication(const double* A,double* C,int nLen);
void multiplication(const double* A,const double* B,double* C,int nLen);
mxArray* multiplication(const mxArray* A,const mxArray* B);
mxArray* division(mxArray* A,mxArray* B);
mxArray* division(mxArray* A,double B);
void division(double* A,double B,int nLen);



void MultivariateMeanShift(const mxArray* pts,mxArray* w,double kernelsize,double* cx,double* cy);

void TwoDMeanShiftInterpolation(const PTR* pts,int nMSGSize,mxArray* w,double kernelsize,double* cx,double* cy,double dx,double dy);

double TwoDMeanShift(const PTR* pts,int nMSGSize,double* w,double kernelsize,double* cx,double* cy,double dx,double dy);
double norm(PTR pt);

double norm(PTR pt)
{
	return sqrt(pow(pt.x,2)+pow(pt.y,2));
}


PTR ptminus(PTR pt1,PTR pt2);
double TvectorError(PTR pt10,PTR pt11,PTR pt12,PTR ptInit0,PTR ptInit1,PTR ptInit2);

double Tryout(PTR ptC,PTR ptXT1,PTR initC,PTR initXT1)
{
	PTR t11=ptminus(ptC,initC);
	PTR t12=ptminus(ptXT1,initXT1);
	PTR ttt=ptminus(initC,initXT1);
	double error=(norm(t11)+norm(t12))/norm(ttt);
	return pow(error,2);
}


double TvectorError(PTR pt10,PTR pt11,PTR pt12,PTR ptInit0,PTR ptInit1,PTR ptInit2)
{
	PTR t11=ptminus(pt11,pt10);
	PTR t12=ptminus(pt12,pt10);

	PTR tInit1=ptminus(ptInit1,ptInit0);
	PTR tInit2=ptminus(ptInit2,ptInit0);

	//printf("t11 (%f,%f), t12 (%f,%f), tInit1 (%f,%f) tInit2 (%f,%f) \n",t11.x,t11.y, t12.x,t12.y,tInit1.x,tInit1.y,tInit2.x,tInit2.y);
	PTR err1=ptminus(t11,tInit1);
	PTR err2=ptminus(t12,tInit2);

	double error=norm(err1)/norm(tInit1);//+norm(err2)/norm(tInit2);
	//error=(norm(t11)-norm(tInit1))/norm(tInit1);
	//error/=2;
	//printf("norm(error1) %f, norm(error2) %f, norm(t1):%f norm(t2):%f  error^2: %f \n",norm(err1),norm(err2),norm(tInit1),norm(tInit2),pow(error,2));
	return pow(error,2);	
}

PTR ptminus(PTR pt1,PTR pt2)
{
	PTR ret;
	ret.x=pt1.x-pt2.x;
	ret.y=pt1.y-pt2.y;
	return ret;
}



void mexFunction(int nargout, mxArray *out[], int nargin, const mxArray	*in[])
{  
	// TODO:  add your function code here

	const mxArray* cMPT=in[0]; //each cell has 2 by 1 [x;y] mxArray
	const mxArray* cMPTInit=in[1];// each cell has 2 by 1 [x;y] mxArray

	const double kdxdy=mxGetScalar(in[2]); 
	double MAP=mxGetScalar(in[3]);
	double beta=mxGetScalar(in[4]);
	double alpha=mxGetScalar(in[5]);
	double range=mxGetScalar(in[6]);//Search Range.....
	const mxArray* mOBS=in[7];//OBS mxArray....imagelikelihood image for whole image...

	const double option=mxGetScalar(in[8]);
	const double kernelsize=mxGetScalar(in[9]);
	const mxArray* ByPassInfo=in[10];
	

	int mh=mxGetM(cMPT);
	int mw=mxGetN(cMPT);


	PTR* ptrZero=(PTR*)mxMalloc(sizeof(PTR)*(range*2+1)*(range*2+1));
	int nMSGSize=0;
	for(int x=-range;x<=range;x++)
	{
		for(int y =-range;y<=range;y++)
		{	
			if( pow((double)x,2)+pow((double)y,2)< pow((double)range,2))
			{
				ptrZero[nMSGSize].x=x;
				ptrZero[nMSGSize++].y=y;
			}
		}
	}
	//We need to assign memory for message with size equal to nMSGSize....




	double** pMLR=(double**)mxMalloc(sizeof(double*)*mh*mw);
	double** pMRL=(double**)mxMalloc(sizeof(double*)*mh*mw);
	double** pMUD=(double**)mxMalloc(sizeof(double*)*mh*mw);
	double** pMDU=(double**)mxMalloc(sizeof(double*)*mh*mw);

	for(int i=0;i<mh*mw;i++)
	{
		pMLR[i]=(double*)mxMalloc(sizeof(double)*nMSGSize);
		pMRL[i]=(double*)mxMalloc(sizeof(double)*nMSGSize);
		pMUD[i]=(double*)mxMalloc(sizeof(double)*nMSGSize);
		pMDU[i]=(double*)mxMalloc(sizeof(double)*nMSGSize);
		for(int k=0;k<nMSGSize;k++)
		{
			pMLR[i][k]=1;
			pMRL[i][k]=1;
			pMUD[i][k]=1;
			pMDU[i][k]=1;
		}
	}

	out[0]=mxCreateDoubleMatrix(mh,mw,mxREAL);//return belief....
	out[1]=mxDuplicateArray(cMPT);//return location of lattice....

	mxArray* r_mB=out[0];
	mxArray* r_cMPT=out[1];

	int outerit=1;


	for(int mmm=0;mmm<outerit;mmm++)//total iteration....
	{

		//printf("1\n");
		for(int m=0;m<mh;m++)
		{
			for(int n=1;n<mw;n++)
			{
				if(GetMA(ByPassInfo,m,n)!=1)
				{
					int isCBypass=0;
					if(GetMA(ByPassInfo,m,n-1)==1)
					{
						isCBypass=1;
					}
					double* pMsg_product=pMLR[m*mw+n-1];//GetMC(r_cMLR,m,n-1);
					mxArray* mLocXT1=GetMC(cMPT,m,n);
					mxArray* mLocC=GetMC(cMPT,m,n-1);
					mxArray* mLocT2=NULL;
					mxArray* mInitT2=NULL;
					if(m<mh-1)//
					{
						mLocT2=GetMC(cMPT,m+1,n);
					}
					else
					{
						mLocT2=GetMC(cMPT,m-1,n);
					}

					const mxArray* mInitXT1=GetMC(cMPTInit,m,n);
					const mxArray* mInitC=GetMC(cMPTInit,m,n-1);

					if(m<mh-1)//
					{
						mInitT2=GetMC(cMPTInit,m+1,n);
					}
					else
					{
						mInitT2=GetMC(cMPTInit,m-1,n);
					}

					//const mxArray* STXOffset=GetMC(cOffset,m,n);
					//const mxArray* STXlikely=GetMC(cINOBS,m,n);
					//mMsg is allocated.....it should be freed

					double* pMsg=GetMSBPMSGObsAbs(isCBypass,ptrZero,nMSGSize,mLocXT1,mLocC,mLocT2,mInitXT1,mInitC,mInitT2,mOBS,MAP,beta,alpha);
					multiplication(pMsg,pMsg_product,pMLR[m*mw+n],nMSGSize);
					mxFree(pMsg);
				}
			}
		}

		//printf("2\n");
		for(int m=0;m<mh;m++)
		{
			for(int n=mw-2;n>=0;n--)
			{			
				if(GetMA(ByPassInfo,m,n)!=1)
				{
					int isCBypass=0;
					if(GetMA(ByPassInfo,m,n+1)==1)
					{
						isCBypass=1;
					}
					double* pMsg_product=pMRL[m*mw+n+1];
					mxArray* mLocXT1=GetMC(cMPT,m,n);
					mxArray* mLocC=GetMC(cMPT,m,n+1);
					mxArray* mLocT2=NULL;
					mxArray* mInitT2=NULL;
					if(m<mh-1)//
					{
						mLocT2=GetMC(cMPT,m+1,n);
					}
					else
					{
						mLocT2=GetMC(cMPT,m-1,n);
					}


					const mxArray* mInitXT1=GetMC(cMPTInit,m,n);
					const mxArray* mInitC=GetMC(cMPTInit,m,n+1);

					if(m<mh-1)//
					{
						mInitT2=GetMC(cMPTInit,m+1,n);
					}
					else
					{
						mInitT2=GetMC(cMPTInit,m-1,n);
					}

					//const mxArray* STXOffset=GetMC(cOffset,m,n);
					//const mxArray* STXlikely=GetMC(cINOBS,m,n);
					//mMsg is allocated.....it should be freed
					double* pMsg=GetMSBPMSGObsAbs(isCBypass,ptrZero,nMSGSize,mLocXT1,mLocC,mLocT2,mInitXT1,mInitC,mInitT2,mOBS,MAP,beta,alpha);
					multiplication(pMsg,pMsg_product,pMRL[m*mw+n],nMSGSize);
					mxFree(pMsg);
				}
			}
		}
		//printf("update\n");

		
		//for up down....
		//printf("3 updown\n");
		for(int n=0;n<mw;n++)
		{
			for(int m=1;m<mh;m++)
			{
				if(GetMA(ByPassInfo,m,n)!=1)
				{
					int isCBypass=0;
					if(GetMA(ByPassInfo,m-1,n)==1)
					{
						isCBypass=1;
					}
					double* pMsg_product=pMUD[mw*(m-1)+n];
					//mxArray* mLocXT1=GetMC(r_cMPT,m,n);
					mxArray* mLocXT1=GetMC(cMPT,m,n);
					//mxArray* mLocC=GetMC(r_cMPT,m-1,n);
					mxArray* mLocC=GetMC(cMPT,m-1,n);
					mxArray* mLocT2=NULL;
					mxArray* mInitT2=NULL;

					if(n<mw-1)//
					{
						//mLocT2=GetMC(r_cMPT,m,n+1);
						mLocT2=GetMC(cMPT,m,n+1);
					}
					else
					{
						//mLocT2=GetMC(r_cMPT,m,n-1);
						mLocT2=GetMC(cMPT,m,n-1);
					}

					const mxArray* mInitXT1=GetMC(cMPTInit,m,n);
					const mxArray* mInitC=GetMC(cMPTInit,m-1,n);

					if(n<mw-1)//
					{
						mInitT2=GetMC(cMPTInit,m,n+1);
					}
					else
					{
						mInitT2=GetMC(cMPTInit,m,n-1);
					}

					//mMsg is allocated.....it should be freed
					double* pMsg=GetMSBPMSGObsAbs(isCBypass,ptrZero,nMSGSize,mLocXT1,mLocC,mLocT2,mInitXT1,mInitC,mInitT2,mOBS,MAP,beta,alpha);
					multiplication(pMsg,pMsg_product,pMUD[m*mw+n],nMSGSize);
					mxFree(pMsg);
				}
			}
		}

		//printf("4 down up\n");
		for(int n=0;n<mw;n++)
		{
			for(int m=mh-2;m>=0;m--)
			{	
				if(GetMA(ByPassInfo,m,n)!=1)
				{
					int isCBypass=0;
					if(GetMA(ByPassInfo,m+1,n)==1)
					{
						isCBypass=1;
					}
					double* pMsg_product=pMDU[mw*(m+1)+n];
					mxArray* mLocXT1=GetMC(cMPT,m,n);
					//mxArray* mLocXT1=GetMC(r_cMPT,m,n);
					//mxArray* mLocC=GetMC(r_cMPT,m+1,n);
					mxArray* mLocC=GetMC(cMPT,m+1,n);
					mxArray* mLocT2=NULL;
					mxArray* mInitT2=NULL;

					if(n<mw-1)//
					{
						//mLocT2=GetMC(r_cMPT,m,n+1);
						mLocT2=GetMC(cMPT,m,n+1);
					}
					else
					{
						//mLocT2=GetMC(r_cMPT,m,n-1);
						mLocT2=GetMC(cMPT,m,n-1);

					}
					const mxArray* mInitXT1=GetMC(cMPTInit,m,n);
					const mxArray* mInitC=GetMC(cMPTInit,m+1,n);

					if(n<mw-1)//
					{
						mInitT2=GetMC(cMPTInit,m,n+1);
					}
					else
					{
						mInitT2=GetMC(cMPTInit,m,n-1);
					}

					//mMsg is allocated.....it should be freed
					double* pMsg=GetMSBPMSGObsAbs(isCBypass,ptrZero,nMSGSize,mLocXT1,mLocC,mLocT2,mInitXT1,mInitC,mInitT2,mOBS,MAP,beta,alpha);
					multiplication(pMsg,pMsg_product,pMDU[m*mw+n],nMSGSize);
					mxFree(pMsg);
				}
			}
		}
	}
	//update....
	//printf("5 update \n");

	if(option!=1)
	{
		for(int m=0;m<mh;m++)
		{
			for(int n=0;n<mw;n++)
			{
				if(GetMA(ByPassInfo,m,n)!=1)
				{
					double* pObs=(double*)mxMalloc(sizeof(double)*nMSGSize);
					mxArray* mLocXT1=GetMC(cMPT,m,n);
					double* center=mxGetPr(mLocXT1);
					for(int i=0;i<nMSGSize;i++)
					{
						PTR ptX;
						ptX.x=ptrZero[i].x+center[0]-1;
						ptX.y=ptrZero[i].y+center[1]-1;		

						pObs[i]=GetObs2(mOBS,ptX,alpha);
					}

					double* pBelief=(double*)mxMalloc(sizeof(double)*nMSGSize);

					multiplication(pMUD[m*mw+n],pMDU[m*mw+n],pBelief,nMSGSize);
					Selfmultiplication(pMLR[m*mw+n],pBelief,nMSGSize);
					Selfmultiplication(pMRL[m*mw+n],pBelief,nMSGSize);				
					Selfmultiplication(pObs,pBelief,nMSGSize);


					mxArray* r_mMPT=mxCreateDoubleMatrix(2,1,mxREAL);
					double sum=GetSum(pBelief,nMSGSize);
					division(pBelief,sum,nMSGSize);

					double belief=0;
					mxArray* retCell=update_state(MAP,pBelief,ptrZero,nMSGSize,center,&belief,kernelsize,kdxdy);



					SetMC(r_cMPT,m,n,GetMC(retCell,0,0));
					SetMA(r_mB,m,n,belief);
					mxFree(pObs);
					mxFree(pBelief);
				}

			}
		}
	}

	for(int i=0;i<mh*mw;i++)
	{
		mxFree(pMLR[i]);
		mxFree(pMRL[i]);
		mxFree(pMUD[i]);
		mxFree(pMDU[i]);
	}
	mxFree(pMLR);
	mxFree(pMRL);
	mxFree(pMUD);
	mxFree(pMDU);

	return;
}

//return array is 1 by nLen.....
double* GetMSBPMSGObsAbs(const int isCBypass,const PTR* ZeroPtr,int nMSGSize,const mxArray* mLocXT1,const mxArray* mLocC,const mxArray* mLocT2,const mxArray* mInitXT1,const mxArray* mInitC,const mxArray* mInitT2,const mxArray* mOBS,double isMap,double beta,double alpha)
{
	double* pLocXT1=mxGetPr(mLocXT1);//2 by 1 [x;y] mxArray
	double* pLocC=mxGetPr(mLocC);//2 by 1 [x;y] mxArray
	double* pLocT2=mxGetPr(mLocT2);

	double* pInitXT1=mxGetPr(mInitXT1);//2 by 1 [x;y] mxArray
	double* pInitC=mxGetPr(mInitC);//2 by 1 [x;y] mxArray
	double* pInitT2=mxGetPr(mInitT2);//2 by 1 [x;y] mxArray

	double* pIMlikely=mxGetPr(mOBS);	

	PTR initXT1;
	initXT1.x=pInitXT1[0]-1;
	initXT1.y=pInitXT1[1]-1;

	PTR initC;
	initC.x=pInitC[0]-1;
	initC.y=pInitC[1]-1;


	PTR initT2;
	initT2.x=pInitT2[0]-1;
	initT2.y=pInitT2[1]-1;

	PTR ptT2;
	ptT2.x=pLocT2[0]-1;
	ptT2.y=pLocT2[1]-1;

	int nLen=nMSGSize;	

	int h=(int)mxGetM(mOBS);
	int w=(int)mxGetN(mOBS);

	double* msgOut= (double*)mxMalloc(sizeof(double)*nLen);   

	double* pmsg=msgOut;
	for(int x=0;x<nLen;x++)
	{
		PTR ptXT1;
		ptXT1.x=ZeroPtr[x].x+pLocXT1[0]-1;
		ptXT1.y=ZeroPtr[x].y+pLocXT1[1]-1;
		//printf("(%.2f,%.2f)\n",ptX.x,ptX.y);
		//double* pTmp=(double*)mxMalloc(sizeof(double)*nLen);
		double nTotal=0;
		double nMax=0;
		if(isCBypass!=1)
		{
			for(int c=0;c<nLen;c++)
			{
				PTR ptC;
				ptC.x=ZeroPtr[c].x+pLocC[0]-1;
				ptC.y=ZeroPtr[c].y+pLocC[1]-1;	

				double obs=GetObs2(mOBS,ptC,alpha);

				double error=TvectorError(ptC,ptXT1,ptT2,initC,initXT1,initT2);
				//double error=Tryout(ptC,ptXT1,initC,initXT1);
				//double com=ComputeCompatibility(ptX,ptC,prevX,prevC,beta);
				double com=exp(-beta*error);
				if(com<0.8)
				{
					//	printf("compatibility %f, Tvector error:%f   OBS:  %f\n",com,error,obs);

				}

				double msg=com*obs;
				nTotal+=(msg);
				if(nMax<msg)
				{
					nMax=msg;
				}
			}

			if(isMap==1 )
			{
				pmsg[x]=nMax;
			}
			else 
			{
				pmsg[x]=nTotal;
			}
		}
		else
		{
			//

			double error=TvectorError(initC,ptXT1,ptT2,initC,initXT1,initT2);
			//double error=Tryout(initC,ptXT1,initC,initXT1);
			//double com=ComputeCompatibility(ptX,ptC,prevX,prevC,beta);
			double com=exp(-beta*error);


			double msg=com;

			pmsg[x]=msg;//regardless of MAX,SUM product rule....

		}
	}
	return msgOut;
}

mxArray* division(mxArray* A,mxArray* B)
{
	int h=mxGetM(A);
	int w=mxGetN(A);
	mxArray* mxOut=mxDuplicateArray(A);
	for(int y=0;y<h;y++)
	{
		for(int x=0;x<w;x++)
		{
			double val=GetMA(A,y,x)/GetMA(B,y,x);
			SetMA(mxOut,y,x,val);
		}
	}
	return mxOut;
}

void division(double* A,double B,int nLen)
{

	for(int i=0;i<nLen;i++)
	{
		A[i]=A[i]/B;	
	}
}


mxArray* division(mxArray* A,double B)
{
	int h=mxGetM(A);
	int w=mxGetN(A);
	mxArray* mxOut=mxDuplicateArray(A);
	for(int y=0;y<h;y++)
	{
		for(int x=0;x<w;x++)
		{
			double val=GetMA(A,y,x)/B;
			SetMA(mxOut,y,x,val);
		}
	}
	return mxOut;
}
mxArray* multiplication(const mxArray* A,const mxArray* B)
{
	int h=mxGetM(A);
	int w=mxGetN(A);
	mxArray* mxOut=mxDuplicateArray(A);
	
	for(int y=0;y<h;y++)
	{
		for(int x=0;x<w;x++)
		{
			double val=GetMA(A,y,x)*GetMA(B,y,x);
			SetMA(mxOut,y,x,val);
			
		}
	}
	return mxOut;
}

void multiplication(const double* A,const double* B,double* C,int nLen)
{
	double sum1=0;
	double sum2=0;
	for(int i=0;i<nLen;i++)
	{
		sum1+=A[i];
		sum2+=B[i];
	}
	double sum3=0;
	for(int i=0;i<nLen;i++)
	{
		C[i]=(A[i]/max(sum1,eps2))*(B[i]/max(sum2,eps2));
		sum3+=C[i];
	}	
	for(int i=0;i<nLen;i++)
	{
		C[i]/=max(sum3,eps2);
	}
}


void Selfmultiplication(const double* A,double* C,int nLen)
{
	double sum1=0;
	double sum2=0;
	for(int i=0;i<nLen;i++)
	{
		sum1+=A[i];
		sum2+=C[i];
	}
	double sum3=0;
	for(int i=0;i<nLen;i++)
	{
		C[i]=(A[i]/max(sum1,eps2))*(C[i]/max(sum2,eps2));
		sum3+=C[i];
	}	
	for(int i=0;i<nLen;i++)
	{
		C[i]/=max(sum3,eps2);
	}		
}


double GetObs(const mxArray* imlikely,PTR pt,double alpha)
{
	int h=(int)mxGetM(imlikely);
	int w=(int)mxGetN(imlikely);
	if( pt.x>=0 && pt.x<w && pt.y>=0 && pt.y<h)
	{

		double obs=GetMA(imlikely,(int)pt.y,(int)pt.x);
		//printf("obs = %f\n",alpha*(obs/255-1));
		return exp( -alpha*(1-obs));
	}
	else
		return 0.5;
}

double GetObs2(const mxArray* imlikely,PTR pt,double alpha)
{
	int h=(int)mxGetM(imlikely);
	int w=(int)mxGetN(imlikely);
	if( pt.x>=0 && pt.x<w && pt.y>=0 && pt.y<h)
	{

		double cx=pt.x;
		double cy=pt.y;
		int lx=(int)floor(cx);
		int hx=(int)floor(cx+0.5);
		int ly=(int)floor(cy);
		int hy=(int)floor(cy+0.5);
		if(cx==lx&&cy==ly)
			return	GetObs(imlikely,pt,alpha);
		else
		{
			double dx=abs(cx-lx);
			double dy=abs(cy-ly);

			double ly_val=(1-dx)*GetMA(imlikely,ly,lx)+dx*GetMA(imlikely,ly,hx);
			double hy_val=(1-dx)*GetMA(imlikely,hy,lx)+dx*GetMA(imlikely,hy,hx);		


			double obs=(ly_val*(1-dy)+hy_val*dy);
			return exp( -alpha*(1-obs) );
		}
	}
	else
		return 0.5;
}

double ComputeCompatibility(PTR curpos1,PTR curpos2, PTR prevpos1,PTR prevpos2,double beta)
{
	double curdis=sqrt( pow((curpos1.x-curpos2.x),2)+ pow((curpos1.y-curpos2.y),2) );
	double prevdis=sqrt( pow((prevpos1.x-prevpos2.x),2)+ pow((prevpos1.y-prevpos2.y),2));
	double absdiffdis=abs(curdis-prevdis);
	double cur_angle=0;
	double prev_angle=0;

	if( curpos1.x ==curpos2.x)
	{
		cur_angle=90;
	}
	else
	{	
		cur_angle=atan((curpos2.y-curpos1.y)/(curpos2.x-curpos1.x));
		cur_angle=cur_angle*180/PI;
	}

	if( prevpos1.x ==prevpos2.x)
	{
		prev_angle=90;
	}
	else
	{	
		prev_angle=atan((prevpos2.y-prevpos1.y)/(prevpos2.x-prevpos1.x));
		prev_angle=prev_angle*180/PI;
	}
	double 	da1=abs(prev_angle-cur_angle);
	double	da2=abs(180.00-da1);
	double diff_angle =0;
	if(da1>=da2)
	{
		diff_angle=da2;
	}
	else
	{
		diff_angle=da1;
	}
	double pdis=exp(-beta*pow(absdiffdis,2));
	double pang=exp(-0.005*pow(diff_angle,2));
	double penalty=0.8;
	double total=pdis*pang;
	//if(curdis>=0 && curdis<=2)
	//{
	//	total*=penalty;
	//}	
	return total;
}



double GetMA(const mxArray *in,int y,int x)
{
	int h=(int)mxGetM(in);
	int w=(int)mxGetN(in);
	double* pTmp=mxGetPr(in);	
	if(_isnan(pTmp[h*x+y]))
	{
		return 0.1;
	}
	else
		return pTmp[h*x+y];
}

double GetMA(const mxArray *in,int y,int x,int h,int w)
{
	double* pTmp=mxGetPr(in);	
	if(_isnan(pTmp[h*x+y]))
	{
		return 0.1;
	}
	else
		return pTmp[h*x+y];
}

mxArray* GetMC(const mxArray *in,int y,int x)
{
	int h=(int)mxGetM(in);
	int w=(int)mxGetN(in);
	mxArray* pTmp=mxGetCell(in,h*x+y);
	return pTmp;
}

void SetMA(mxArray *in,int y,int x,double val)
{
	int h=(int)mxGetM(in);
	int w=(int)mxGetN(in);
	double* pTmp=mxGetPr(in);	
	pTmp[h*x+y]=val;
}

void SetMC(mxArray *in,int y,int x,mxArray* val)
{
	int h=(int)mxGetM(in);
	int w=(int)mxGetN(in);	
	mxArray* pTmp=GetMC(in,y,x);
	if(pTmp==NULL)
		mxSetCell(in,h*x+y,mxDuplicateArray(val));	
	else
	{
		mxDestroyArray(pTmp);
		mxSetCell(in,h*x+y,mxDuplicateArray(val));	
	}
}

double GetMax(mxArray* mIn,int* pOutY,int* pOutX)
{
	int h=(int)mxGetM(mIn);
	int w=(int)mxGetN(mIn);	
	double max=-1000000;
	for(int y=0;y<h;y++)
	{
		for(int x=0;x<w;x++)
		{
			double tmp=GetMA(mIn,y,x,h,w);
			if(tmp>max)
			{
				max=tmp;
				pOutY[0]=y;
				pOutX[0]=x;
			}
		}
	}
	return max;
}


double GetMax(double* A,int nLen,int* index)
{
	double max=-1000000;
	for(int i=0;i<nLen;i++)
	{

		if(A[i]>max)
		{
			max=A[i];
			index[0]=i;
		}
	}

	return max;
}

mxArray* generatePTS(const mxArray* ZeroPts,double cy,double cx)
{
	int h=mxGetM(ZeroPts);// 2
	int nCnt=mxGetN(ZeroPts);// cnt

	mxArray* pPts=mxCreateDoubleMatrix(2,nCnt,mxREAL);

	for(int i=0;i<nCnt;i++)
	{
		double X=GetMA(ZeroPts,0,i,h,nCnt);
		double Y=GetMA(ZeroPts,1,i,h,nCnt);
		SetMA(pPts,0,i,X+cx);
		SetMA(pPts,1,i,Y+cy);
	}
	return pPts;

}

mxArray* update_state(int MAP,double* pBelief,const PTR* ZeroPtr,int nMSGSize,double* center,double* pr_Belief,double kernelsize,int kdxdy)
{
	mxArray* ret=mxCreateCellMatrix(1,2);
	mxArray* r_mMPT=mxCreateDoubleMatrix(2,1,mxREAL);

	if(MAP==1)
	{//find maximum belief and its argument

		int idx=-1;
		double val=GetMax(pBelief,nMSGSize,&idx);		
		double maxx=ZeroPtr[idx].x+center[0];
		double maxy=ZeroPtr[idx].y+center[1];
		SetMA(r_mMPT,0,0,maxx);
		SetMA(r_mMPT,1,0,maxy);		

		//update belief
		*pr_Belief=val;
		//update state of the node		
		SetMC(ret,0,0,r_mMPT);		
	}
	else if(MAP==0)
	{
		//find mean belief and its argument
		int len=nMSGSize;
		double* ptsx=(double*)mxMalloc(sizeof(double)*len);
		double* ptsy=(double*)mxMalloc(sizeof(double)*len);


		double sum=GetSum(pBelief,len);
		for(int i=0;i<len;i++)
		{
			double tmpx=ZeroPtr[i].x;
			double tmpy=ZeroPtr[i].y;
			ptsx[i]=tmpx*pBelief[i]/sum;
			ptsy[i]=tmpy*pBelief[i]/sum;
		}

		double meanx=GetSum(ptsx,len)+center[0];
		double meany=GetSum(ptsy,len)+center[1];

		//update belief
		int idx=findClosest(meanx,meany,ptsx,ptsy,len);
		*pr_Belief=pBelief[idx];;
		//update state of the node

		SetMA(r_mMPT,0,0,meanx);
		SetMA(r_mMPT,1,0,meany);		

		SetMC(ret,0,0,r_mMPT);		
	}
	else
	{
		//find mean belief and its argument

		int len=nMSGSize;

		double mx=0;
		double my=0;

		double belief=TwoDMeanShift(ZeroPtr,len,pBelief,kernelsize,&mx,&my,kdxdy,kdxdy);
		//update state of the node
		mx+=center[0];
		my+=center[1];

		SetMA(r_mMPT,0,0,mx);
		SetMA(r_mMPT,1,0,my);		

		//update belief

		*pr_Belief=belief;
		SetMC(ret,0,0,r_mMPT);

	}
	return ret;
}

int findClosest(double meanx,double meany,mxArray* ptsx,mxArray* ptsy)
{
	double mindistance=100000;
	int len=mxGetN(ptsx);
	int minIdx=-1;
	for(int i=0;i<len;i++)
	{
		double tmp=sqrt(pow(meanx-GetMA(ptsx,0,i,1,len),2)+pow(meany-GetMA(ptsy,0,i,1,len),2));
		if(tmp<mindistance)
		{
			mindistance=tmp;
			minIdx=i;
		}
	}
	return minIdx;
}


int findClosest(double meanx,double meany,double* ptsx,double* ptsy,int nLen)
{
	double mindistance=100000;

	int minIdx=-1;
	for(int i=0;i<nLen;i++)
	{
		double tmp=sqrt(pow(meanx-ptsx[i],2)+pow(meany-ptsy[i],2));
		if(tmp<mindistance)
		{
			mindistance=tmp;
			minIdx=i;
		}
	}
	return minIdx;
}


double GetSum(mxArray* mIn)
{
	int h=mxGetM(mIn);
	int w=mxGetN(mIn);
	double total=0;
	for(int y=0;y<h;y++)
	{
		for(int x=0;x<w;x++)
		{
			total+=GetMA(mIn,y,x);	
		}
	}
	return total;
}

double GetSum(double* pIn,int nLen)
{
	double total=0;
	for(int i=0;i<nLen;i++)
	{
		total+=pIn[i];	
	}
	return total;
}

void MultivariateMeanShift(const mxArray* pts,mxArray* w,double kernelsize,double* cx,double* cy)
{
	int nCnt=mxGetN(pts);
	int d=mxGetM(pts);
	double s=pow((double)kernelsize,2);

	double* nw=(double*)mxMalloc(sizeof(double)*nCnt);
	double sum=0;


	for(int i=0;i<nCnt;i++)
	{
		double tw=0;
		for(int k=1;k<nCnt;k++)
		{
			double total=0;
			for(int m=0;m<d;m++)
			{
				total+=( 1/s*pow( (GetMA(pts,m,k)-GetMA(pts,m,i)) ,2));
			}
			double p=exp(-0.5*total);
			tw+=p*GetMA(w,0,k);
		}
		nw[i]=tw;  
		sum+=tw;
	}

	for(int i=0;i<nCnt;i++)
	{
		nw[i]/=sum;
	}

	double mx=0;
	double my=0;

	for(int i=0;i<nCnt;i++)
	{
		mx+=GetMA(pts,0,i)*nw[i];
		my+=GetMA(pts,1,i)*nw[i];
	}

	*cx=mx;
	*cy=my;

	mxFree(nw);
}


double TwoDInterpolation(const mxArray* w,double x,double y,int* Trust)
{	
	int nLen=mxGetN(w);
	int range=(sqrt((double)nLen)-1)/2;

	double arrayx=x+range;
	double arrayy=y+range;
	int lx=(int)floor(arrayx);
	int hx=(int)floor(arrayx+0.5);
	int ly=(int)floor(arrayy);
	int hy=(int)floor(arrayy+0.5);	

	if(lx>=0 && hx < (2*range+1) &&ly >=0 && hy <(2*range+1))
	{
		if(arrayx==lx&&arrayy==ly)
		{
			*Trust=1;
			return	GetMA(w,arrayy,arrayx);
		}
		else
		{
			double diffx=abs(arrayx-lx);
			double diffy=abs(arrayy-ly);
			double ly_val=(1-diffx)*GetMA(w,ly,lx)+diffx*GetMA(w,ly,hx);
			double hy_val=(1-diffx)*GetMA(w,hy,lx)+diffx*GetMA(w,hy,hx);		
			*Trust=1;
			return (ly_val*(1-diffy)+hy_val*diffy);
		}
	}
	else
	{
		*Trust=0;
		return -1;
	}
}



void TwoDMeanShiftInterpolation(const PTR* pts,int nMSGSize,mxArray* w,double kernelsize,double* cx,double* cy,double dx,double dy)
{
	int nCnt=nMSGSize;
	int d=2;
	double s=pow((double)kernelsize,2);

	double* nw=(double*)mxMalloc(sizeof(double)*nCnt);


	PTR* sample_points=(PTR*)mxMalloc(sizeof(PTR)*(dx*2+1)*(dy*2+1));
	double* sample_weights=(double*)mxMalloc(sizeof(double)*(dx*2+1)*(dy*2+1));

	int iter=0;
	double epsilon=0.001;
	double error=100;
	int wantBreak=0;
	while( error > epsilon && iter < 15)
	{
		int sample_cnt=0;
		for(double y=-dy+*cy;y<=dy+*cy;y++)
		{
			for(double x=-dx+*cx;x<=*cx+dx;x++)
			{
				sample_points[sample_cnt].x=x;
				sample_points[sample_cnt].y=y;
				int nTrust=0;
				sample_weights[sample_cnt++]=TwoDInterpolation(w,x,y,&nTrust);
				if(nTrust==0)
					wantBreak=1;
			}
		}
		if (wantBreak)
			break;
		double sum=0;
		for(int i=0;i<sample_cnt;i++)
		{
			double tw=0;
			for(int k=0;k<sample_cnt;k++)
			{
				double total=0;
				total+=( 1/s*( pow( sample_points[k].x -sample_points[i].x ,2) + pow( sample_points[k].y -sample_points[i].y ,2) ) );				
				double p=exp(-0.5*total);
				tw+=p*sample_weights[k];
			}
			nw[i]=tw;  
			sum+=tw;
		}

		for(int i=0;i<sample_cnt;i++)
		{
			nw[i]/=sum;
		}

		double mx=0;
		double my=0;

		for(int i=0;i<sample_cnt;i++)
		{
			mx+=sample_points[i].x*nw[i];
			my+=sample_points[i].y*nw[i];
		}
		error=pow((*cx-mx),2)+pow((*cy-my),2);
		(*cx)=mx;
		(*cy)=my;		
	}	
	mxFree(sample_points);
	mxFree(sample_weights);
	mxFree(nw);
}



double TwoDMeanShift(const PTR* pts,int nMSGSize,double* w,double kernelsize,double* cx,double* cy,double dx,double dy)
{
	int nCnt=nMSGSize;
	int d=2;
	double s=pow((double)kernelsize,2);

	double* nw=(double*)mxMalloc(sizeof(double)*nCnt);


	PTR* sample_points=(PTR*)mxMalloc(sizeof(PTR)*nCnt);
	PTR* kernelpts=(PTR*)mxMalloc(sizeof(PTR)*nCnt);
	int kernelptcnt=0;
	for(int y=-dy;y<=dy;y++)
	{
		for(int x=-dx;x<=dx;x++)
		{
			kernelpts[kernelptcnt].x=x;
			kernelpts[kernelptcnt++].y=y;
		}
	}



	//double* sample_weights=(double*)mxMalloc(sizeof(double)*(dx*2+1)*(dy*2+1));




	int iter=0;
	double epsilon=0.001;
	double error=100;
	int nBreak=0;
	while(error > epsilon && iter <10)
	{
		int sample_cnt=0;

		double dMinX=0;
		double dMaxX=0;
		double dMinY=0;
		double dMaxY=0;
		for(int i=0;i<kernelptcnt;i++)
		{
			sample_points[sample_cnt].x=kernelpts[i].x+*cx;
			sample_points[sample_cnt].y=kernelpts[i].y+*cy;
			if(dMinX<sample_points[sample_cnt].x)
				dMinX=sample_points[sample_cnt].x;

			if(dMaxX>sample_points[sample_cnt].x)
				dMaxX=sample_points[sample_cnt].x;

			if(dMinY<sample_points[sample_cnt].y)
				dMinY=sample_points[sample_cnt].y;

			if(dMaxY<=sample_points[sample_cnt].y)
				dMaxY=sample_points[sample_cnt].y;
			sample_cnt++;

			if(dMinX<-5 || dMinY<-5 || dMaxX>5 || dMaxY>5)
				nBreak=1;
		}
		if(nBreak==1)
			break;
		double sum=0;
		for(int i=0;i<sample_cnt;i++)
		{
			double tw=0;
			double total=0;
			for(int k=0;k<nCnt;k++)
			{

				total+=( 1/s*( pow( pts[k].x -sample_points[i].x ,2) + pow( pts[k].y -sample_points[i].y ,2) ) );				
				double p=exp(-0.5*total);
				tw+=p*w[k];
			}
			nw[i]=tw;  
			sum+=tw;
		}

		for(int i=0;i<sample_cnt;i++)
		{
			nw[i]/=sum;
		}

		double mx=0;
		double my=0;

		for(int i=0;i<sample_cnt;i++)
		{
			mx+=sample_points[i].x*nw[i];
			my+=sample_points[i].y*nw[i];
		}
		error=pow((*cx-mx),2)+pow((*cy-my),2);
		if( abs(mx)>10 ||abs(my)>10)
		{
			printf("error in mean shift\n");
			break;
		}
		else
		{
			(*cx)=mx;
			(*cy)=my;		
		}
		iter++;
	}	


	double prob=0;
	for(int k=0;k<nCnt;k++)
	{
		double total=0;
		total+=( 1/s*( pow( pts[k].x -(*cx) ,2) + pow( pts[k].y -(*cy) ,2) ) );				
		double p=exp(-0.5*total);
		prob+=p*w[k];
	}
	mxFree(sample_points);
	mxFree(kernelpts);
	mxFree(nw);
	return prob;
}



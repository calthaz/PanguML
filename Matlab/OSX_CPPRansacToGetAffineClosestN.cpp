// CPPRansacToGetAffine.cpp : mex-function interface implentation file


#include "mex.h"
#include <math.h>
#include "MexAndCpp.h"
#include "MatlabToOpenCV.h"
#include "matrix.h"
#include <list>
#include <vector>
#include <iostream>
#include <algorithm>

#define PI (atan(1.0)*4.0)


using namespace std ;
typedef struct
{
	double distance;
	int index;	
}DATA;



typedef vector<DATA> LIST;



typedef struct
{	
	CvPoint2D32f intPt;
	double distance;
	int nOriginalIdx;
}INLIER;

bool UDShortestDistanceFirst ( DATA elem1, DATA elem2 );
void Assign(INLIER* A,INLIER B);
int Connected(CvPoint2D32f pt1,CvPoint2D32f pt2,int nConn);
int Copy(INLIER* A,int* nLenA,INLIER* B,int* nLenB);
int InsertUniqueCloserDistance(INLIER* arrayInliers,int* nLen,int nMaxBuf,INLIER target);
int InsertUnique(INLIER* arrayInliers,int* nLen,int nMaxBuf,INLIER target);
double norm(CvPoint2D32f pt);
CvPoint2D32f ptminus(CvPoint2D32f pt1,CvPoint2D32f pt2);
float innerprod(CvPoint2D32f pt1,CvPoint2D32f pt2);

void Assign(INLIER* A,INLIER B)
{
	A->distance=B.distance;
	A->intPt=B.intPt;
	A->nOriginalIdx=B.nOriginalIdx;
}


int Connected(CvPoint2D32f pt1,CvPoint2D32f pt2,int nConn)
{
	if(nConn==4)
	{
		if(norm(ptminus(pt1,pt2))<=1)
		{
			return 1;
		}
		else
			return 0;
	}
	else
	{
		CvPoint2D32f flag=ptminus(pt1,pt2);
		if(fabs(flag.x)<=1 && fabs(flag.y)<=1)
			return 1;
		else 
			return 0;
	}
}

int Copy(INLIER* A,int* nLenA,INLIER* B,int* nLenB)
{
	for(int k=0;k<(*nLenB);k++)
	{
		Assign(&A[k],B[k]);
	}
	(*nLenA)=(*nLenB);
	return (*nLenA);
}





int InsertUnique(INLIER* arrayInliers,int* nLen,int nMaxBuffer,INLIER target)
{
	if(*nLen>=nMaxBuffer)
	{
		printf("error occurs\n");
		return (*nLen);
	}
	for(int i=0;i<(*nLen);i++)
	{
		if(target.intPt.x==arrayInliers[i].intPt.x && target.intPt.y==arrayInliers[i].intPt.y)
		{			
			return *nLen;			
		}
	}
	//if code gets here it means we need insert...
	Assign(&arrayInliers[*nLen],target);
	(*nLen)++;
	return (*nLen);
}


int InsertUniqueCloserDistance(INLIER* arrayInliers,int* nLen,int nMaxBuffer,INLIER target)
{
	if(*nLen>=nMaxBuffer)
	{
		printf("error occurs\n");
		return (*nLen);
	}
	for(int i=0;i<(*nLen);i++)
	{
		if(target.intPt.x==arrayInliers[i].intPt.x && target.intPt.y==arrayInliers[i].intPt.y)
		{
			if(target.distance<arrayInliers[i].distance)
			{
				Assign(&arrayInliers[i],target);				
				return *nLen;
			}
			else
			{
				return *nLen;
			}
		}
	}
	//if code gets here it means we need insert...
	Assign(&arrayInliers[*nLen],target);
	(*nLen)++;
	return (*nLen);
}



double norm(CvPoint2D32f pt)
{
	return sqrt(pow(pt.x,2)+pow(pt.y,2));
}



float innerprod(CvPoint2D32f pt1,CvPoint2D32f pt2)
{
	return (pt1.x*pt2.x+pt1.y*pt2.y);
}

CvPoint2D32f ptminus(CvPoint2D32f pt1,CvPoint2D32f pt2)
{
	CvPoint2D32f ret;
	ret.x=pt1.x-pt2.x;
	ret.y=pt1.y-pt2.y;
	return ret;
}


void mexFunction(int nargout, mxArray *out[], int nargin, const mxArray	*in[])
{  	
	//function [ret_i,maxUV,maxcnt,cMaxMPT]=RANSAC_TO_GET_PROJ(pts,maxit,error)
	const mxArray* mPTS=in[0];
	int maxit=(int)mxGetScalar(in[1]);
	double error=mxGetScalar(in[2]);
	int ConnTopology=(int)mxGetScalar(in[3]);
	int CN=(int)mxGetScalar(in[4]); //closestN....

	
	int nPTR=mxGetN(mPTS);
	if(nPTR-1<CN)
	{
		CN=nPTR-1;
	}

	CvPoint2D32f* pts=(CvPoint2D32f*)mxMalloc(sizeof(CvPoint2D32f)*nPTR);
	CvPoint2D32f* new_pts=(CvPoint2D32f*)mxMalloc(sizeof(CvPoint2D32f)*nPTR);

	INLIER* inliers=(INLIER*)mxMalloc(sizeof(INLIER)*nPTR);
	INLIER* max_inliers=(INLIER*)mxMalloc(sizeof(INLIER)*nPTR);
	INLIER* connected=(INLIER*)mxMalloc(sizeof(INLIER)*nPTR);
	int nInlier=0;
	int nMaxInlier=0;
	int nConnected=0;
	int maxcnt=0;
	for(int i=0;i<nPTR;i++)
	{
		pts[i].x=CMexAndCpp::GetMA(mPTS,0,i,2,nPTR);
		pts[i].y=CMexAndCpp::GetMA(mPTS,1,i,2,nPTR);
	}


	int nHowMany=nPTR-1;
	int* pClosestList=(int*)mxMalloc(sizeof(int)*nPTR*nHowMany);

	for(int i=0;i<nPTR;i++)
	{
		LIST list;
		for(int k=0;k<nPTR;k++)
		{
			if(k!=i)
			{
				CvPoint2D32f p1=pts[i];
				CvPoint2D32f p2=pts[k];

				DATA dat;
				dat.distance=pow(p1.x-p2.x,2)+pow(p1.y-p2.y,2);
				dat.index=k;
				list.push_back(dat);
			}
		}
		sort(list.begin(),list.end(),UDShortestDistanceFirst);
		for(int k=0;k<nHowMany;k++)
		{
			DATA dat=list.at(k);
			pClosestList[i*nHowMany+k]=dat.index;
		}	
		list.clear();
	}

	
	
	int wid=1;

	CvPoint2D32f to_pts[3];
	to_pts[0]=cvPoint2D32f(0,0);
	to_pts[1]=cvPoint2D32f(wid,0);
	to_pts[2]=cvPoint2D32f(0,wid);

	CvPoint2D32f from_pts[3];
	bool display=false;
	list <int>history;
	list <int>::iterator Iter;
	list <int>::iterator result;
	CvMat* mat_affine=cvCreateMat(2,3,CV_32FC1);								

	int innermax_idx=10;	

	for(int ik=0;ik<maxit && ik <nPTR;ik++)
	{
		//select 1 point......and choose closest 3 points....
		int idx[3];
		int flag=0;
		if(maxit<nPTR)
		{
			idx[0]= ((double) rand() / (double) RAND_MAX) * (nPTR-1) ;
			result = find( history.begin( ), history.end( ), idx[0] );
			if(result ==history.end())
				flag=1;

		}
		else
		{
			idx[0]= ik;
			flag=1;
		}

		if(flag)
		{
			if(maxit<nPTR)
				history.push_back(idx[0]);

			for(int i2=0;i2<innermax_idx && i2< nPTR-1 && i2< CN;i2++)
			{
				idx[1]=pClosestList[idx[0]*nHowMany+i2];
				for(int ii=1;ii<innermax_idx && i2+ii< nPTR-1 && i2+ii<CN ;ii++)
				{

					idx[2]=pClosestList[idx[1]*nHowMany+i2+ii];

					CvPoint2D32f v0=ptminus(pts[idx[2]],pts[idx[0]]);
					CvPoint2D32f v1=ptminus(pts[idx[1]],pts[idx[0]]);
					
					double angle=acos( innerprod(v0,v1) / norm(v0) / norm(v1) )*180/PI;
					if( angle>20 && angle < 170)
					{
						CvPoint2D32f t1;
						CvPoint2D32f t2;
						CvPoint2D32f v2=ptminus(pts[idx[2]],pts[idx[1]]);
						int midx=-1;
						int tidx[3];
						if(norm(v0)>=norm(v1))
						{
							if(norm(v2)>=norm(v0))
							{//v2 is biggest
								midx=2;
							}
							else
							{//v0 is biggest
								midx=0;
							}

						}
						else
						{
							if(norm(v2)>=norm(v1))
							{//v2 is biggest
								midx=2;
							}
							else
							{//v1 is biggest
								midx=1;
							}
						}							
						if( midx==0)
						{
							t1=ptminus(pts[idx[0]],pts[idx[1]]);
							t2=ptminus(pts[idx[2]],pts[idx[1]]);
							tidx[0]=idx[1];
							tidx[1]=idx[0];
							tidx[2]=idx[2];
						}
						else if( midx== 1)
						{
							t1=ptminus(pts[idx[1]],pts[idx[2]]);
							t2=ptminus(pts[idx[0]],pts[idx[2]]);
							tidx[0]=idx[2];
							tidx[1]=idx[1];
							tidx[2]=idx[0];
						}
						else
						{
							t1=ptminus(pts[idx[1]],pts[idx[0]]);
							t2=ptminus(pts[idx[2]],pts[idx[0]]);
							tidx[0]=idx[0];
							tidx[1]=idx[1];
							tidx[2]=idx[2];
						}
						from_pts[0]=pts[tidx[0]];
						from_pts[1]=pts[tidx[1]];
						from_pts[2]=pts[tidx[2]];

						//get projective transform....
						
						cvGetAffineTransform(from_pts,to_pts,mat_affine);
						
						//then transform all the points using the computed transform and measure
						for(int k=0;k< CN;k++)
						{
							new_pts[k].x=mat_affine->data.fl[0]*pts[pClosestList[tidx[0]*nHowMany+k]].x+mat_affine->data.fl[1]*pts[pClosestList[tidx[0]*nHowMany+k]].y+mat_affine->data.fl[2];
							new_pts[k].y=mat_affine->data.fl[3]*pts[pClosestList[tidx[0]*nHowMany+k]].x+mat_affine->data.fl[4]*pts[pClosestList[tidx[0]*nHowMany+k]].y+mat_affine->data.fl[5];
						}
						//cvTransform(pts,new_pts,mat_affine);					
						double sumerror=0;
						
						// we round each point and select unique interger locations.
						//if transform is from t1 t2 then there should be many unique
						//interger locations, however it can't be exactly integer location
						//we can choose within some threshold and the threshold should be
						//working o.k in different image since we normalize in a way because
						//we bring all the points into 0 1 based coordinates....
						nInlier=0;
						for(int k=0;k<CN;k++)
						{
							CvPoint2D32f intPt=cvPoint2D32f(floor(new_pts[k].x+0.5),floor(new_pts[k].y+0.5));
							CvPoint2D32f errorVec=ptminus(intPt,new_pts[k]);
							double distance=norm(errorVec);
							if(distance <error*2)
							{
								INLIER tmp;
								tmp.distance=distance;
								tmp.intPt=intPt;
								tmp.nOriginalIdx=pClosestList[tidx[0]*nHowMany+k];
								InsertUniqueCloserDistance(inliers,&nInlier,nPTR,tmp);
							}
						}
						//Now we want to find connected component to pts[tidx[0]] from inliers.....
						nConnected=0;
						INLIER proposal[3];
						proposal[0].distance=0;
						proposal[0].intPt=cvPoint2D32f(0,0);
						proposal[0].nOriginalIdx=tidx[0];

						proposal[1].distance=0;
						proposal[1].intPt=cvPoint2D32f(1,0);
						proposal[1].nOriginalIdx=tidx[1];

						proposal[2].distance=0;
						proposal[2].intPt=cvPoint2D32f(0,1);
						proposal[2].nOriginalIdx=tidx[2];
						InsertUnique(connected,&nConnected,nPTR,proposal[0]);
						InsertUnique(connected,&nConnected,nPTR,proposal[1]);
						InsertUnique(connected,&nConnected,nPTR,proposal[2]);

						for(int m=0;m<nConnected;m++)
						{
							for(int k=0;k<nInlier;k++)
							{									
								if(Connected(connected[m].intPt,inliers[k].intPt,ConnTopology))
								{
									InsertUnique(connected,&nConnected,nPTR,inliers[k]);
								}
							}
						}						

						if( nConnected>maxcnt)
						{
							//we need to compare the lenght of t1 t2

							//Copy
							Copy(max_inliers,&nMaxInlier,connected,&nConnected);
							maxcnt=nConnected;//number of inliers....
							
						}
						else if( nConnected==maxcnt)
						{
							CvPoint2D32f max_t1=ptminus(pts[max_inliers[2].nOriginalIdx],pts[max_inliers[0].nOriginalIdx]);
							CvPoint2D32f max_t2=ptminus(pts[max_inliers[1].nOriginalIdx],pts[max_inliers[0].nOriginalIdx]);

							if( norm(t1)*norm(t2)<norm(max_t1)*norm(max_t2))
							{
								Copy(max_inliers,&nMaxInlier,connected,&nConnected);
								maxcnt=nConnected;//number of inliers....
							}

						}

					}
					
				}

				
			}


		}

	}
	history.clear();
	cvReleaseMat(&mat_affine);


	//ret_i,maxUV,maxcnt,cMaxMPT

	out[0]=mxCreateDoubleMatrix(1,3,mxREAL);
	CMexAndCpp::SetMA(out[0],0,0,max_inliers[0].nOriginalIdx+1);
	CMexAndCpp::SetMA(out[0],0,1,max_inliers[1].nOriginalIdx+1);
	CMexAndCpp::SetMA(out[0],0,2,max_inliers[2].nOriginalIdx+1);



	out[1]=mxCreateDoubleMatrix(2,maxcnt,mxREAL);
	double minx=0;
	double maxx=0;
	double miny=0;
	double maxy=0;
	for(int i=0;i<maxcnt;i++)
	{
		CMexAndCpp::SetMA(out[1],0,i,pts[max_inliers[i].nOriginalIdx].x);
		CMexAndCpp::SetMA(out[1],1,i,pts[max_inliers[i].nOriginalIdx].y);
		if(i==0)
		{
			minx=max_inliers[i].intPt.x;
			maxx=max_inliers[i].intPt.x;
			miny=max_inliers[i].intPt.y;
			maxy=max_inliers[i].intPt.y;
		}
		else
		{
			if(minx>max_inliers[i].intPt.x)
			{
				minx=max_inliers[i].intPt.x;
			}
			if(maxx<max_inliers[i].intPt.x)
			{
				maxx=max_inliers[i].intPt.x;
			}
			if(miny>max_inliers[i].intPt.y)
			{
				miny=max_inliers[i].intPt.y;
			}
			if(maxy<max_inliers[i].intPt.y)
			{
				maxy=max_inliers[i].intPt.y;
			}
		}
	}
	

	out[2]=mxCreateDoubleMatrix(1,1,mxREAL);
	CMexAndCpp::SetMA(out[2],0,0,maxcnt);

	int mh=maxy-miny+1;
	int mw=maxx-minx+1;
	out[3]=mxCreateDoubleMatrix(mh,mw,mxREAL);
	out[4]=mxCreateDoubleMatrix(mh,mw,mxREAL);

	for(int my=0;my<mh;my++)
	{
		for(int mx=0;mx<mw;mx++)
		{
			CMexAndCpp::SetMA(out[3],my,mx,-1);
			CMexAndCpp::SetMA(out[4],my,mx,-1);
		}
	}
	for(int i=0;i<maxcnt;i++)
	{
		CMexAndCpp::SetMA(out[3],(int)(max_inliers[i].intPt.y)-(int)miny,(int)(max_inliers[i].intPt.x)-(int)minx,pts[max_inliers[i].nOriginalIdx].x);
		CMexAndCpp::SetMA(out[4],(int)(max_inliers[i].intPt.y)-(int)miny,(int)(max_inliers[i].intPt.x)-(int)minx,pts[max_inliers[i].nOriginalIdx].y);
	}

	mxFree(pts);
	mxFree(new_pts);
	mxFree(inliers);
	mxFree(max_inliers);
	mxFree(connected);
	mxFree(pClosestList);
}






bool UDShortestDistanceFirst ( DATA elem1,DATA elem2 )
{
	return elem1.distance < elem2.distance;
}


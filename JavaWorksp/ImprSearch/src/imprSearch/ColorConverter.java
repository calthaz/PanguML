package imprSearch;

import static java.lang.Math.abs;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

/*
 * sample format and recommended separators.
 * DATA FOR COLOR CONVERTER:
 * Recommended separators: SEP_VALUES: 0, 0.01, 0.26, 0.4, 0.65, 0.66, 0.97, 0.99, END. 
 */

/**
 * this class is responsible for converting machine's colors to ours.
 * Currently it is based on different sensitivity on different hue ranges.  
 * Use this to customize search
 * 
 * Note: range like 98 to 5 is not supported! Write 0 to 5, 98 to 99. 
 * @author asus-pc
 *
 */
public class ColorConverter {
	public static final int NUM_SEPS = 14;
	public final double[] HUE_SEPS= new double[NUM_SEPS];
	private double seps[];
	private double stepLength;
	private int validSeps;
	private Scanner sc;

	public ColorConverter(){
		//TODO: replace this with loading data from .txt file	
		loadSeparators();
		
		stepLength=0;
		double last = HUE_SEPS[0];
		for(int i=1;i<HUE_SEPS.length-1; i+=2){
			stepLength+=HUE_SEPS[i]-last;
			last = HUE_SEPS[i+1];
		}
		
		stepLength+=HUE_SEPS[HUE_SEPS.length-1]-last;
		//stepLength-=HUE_SEPS.length/2;
		stepLength = 1/(1-stepLength);
		
		validSeps = 0;
		//if there is no separator at all?
		for(double i: HUE_SEPS){
			if(i!=-1)validSeps++;//i!=0???
		}
		
		if(validSeps==0){
			HUE_SEPS[0]=0;
			HUE_SEPS[1]=0;
			validSeps=2;
		}
		
		seps = new double[validSeps/2];
		//seps[0]=HUE_SEPS[0]+(HUE_SEPS[1]-HUE_SEPS[0])*stepLength;//0*stepLength???
		seps[0]=HUE_SEPS[0]*stepLength;
		for(int i=2;i<validSeps-1; i+=2){//start from 2???
			seps[i/2]= seps[i/2-1]+(HUE_SEPS[i]-HUE_SEPS[i-1])*stepLength;
		}
			
		
		
		
		//for(double i:seps){
			//System.out.println(i);
		//}
	}
	
	public double toMyHue(float hue){
		
		double myHue=0;
		
		if(hue<HUE_SEPS[0]){
			myHue=hue*stepLength;
			return myHue;
		}else if(hue>=HUE_SEPS[0]&&hue<=HUE_SEPS[1]){//add a "=" in case these two are equal
			myHue = seps[0];
			return myHue;
		}
		/*else 
			if(hue>=HUE_SEPS[1]&&hue<HUE_SEPS[2]){
			myHue = seps[0]+(hue-HUE_SEPS[1])*stepLength;
		}else if(hue>=HUE_SEPS[2]&&hue<HUE_SEPS[3]){
			myHue=seps[1];
		}else 
			
			if(hue>=HUE_SEPS[3]&&hue<HUE_SEPS[4]){
			myHue = seps[1]+(hue-HUE_SEPS[3])*stepLength;
		}else if(hue>=HUE_SEPS[4]&&hue<HUE_SEPS[5]){
			myHue=seps[2];
		}else 
			
			if(hue>=HUE_SEPS[5]&&hue<HUE_SEPS[6]){
			myHue = seps[2]+(hue-HUE_SEPS[5])*stepLength;
		}else if(hue>=HUE_SEPS[6]&&hue<HUE_SEPS[7]){
			myHue = seps[3];
		}else if(hue>=HUE_SEPS[7]&&hue<HUE_SEPS[8]){
			myHue = seps[3]+(hue-HUE_SEPS[7])*stepLength;
		}*/
		
		for(int i = 1; i<validSeps-1;i+=2){
			if(hue>=HUE_SEPS[i]&&hue<HUE_SEPS[i+1]){//i=1
				myHue = seps[(i-1)/2]+(hue-HUE_SEPS[i])*stepLength;
				return myHue;
			}else if(hue>=HUE_SEPS[i+1]&&hue<HUE_SEPS[i+2]){
				myHue=seps[(i+1)/2];
				return myHue;
			}
		}
		
		if(hue>HUE_SEPS[validSeps-1]){
			myHue = seps[seps.length-1]+(hue-HUE_SEPS[validSeps-1])*stepLength;
			return myHue;
		}
		
		//not a valid hue input
		return -1;
		
	}
	
	public float toStandardHue(double hue){
		double stdH=-1;
		
		if(hue<seps[0]){
			stdH = (float) (hue/stepLength);//stdH???
			return (float)stdH;
		}else if(abs(hue-seps[0])<0.000001){
			stdH = (HUE_SEPS[0]+HUE_SEPS[1])/2;
			return (float) stdH;
		}
		
		for(int i = 0; i<seps.length-1;i++){
			if(hue>seps[i]&&hue<seps[i+1]){
				stdH = HUE_SEPS[2*i+1]+(hue-seps[i])/stepLength;
				return (float) stdH;
			}else if(abs(hue-seps[i])<0.000001){
				stdH = (HUE_SEPS[i*2]+HUE_SEPS[i*2+1])/2;
				return (float) stdH;
			}
		}
		
		if(hue>seps[seps.length-1]){
			stdH=HUE_SEPS[validSeps-1]+(hue-seps[validSeps/2-1])/stepLength;
		}
		
		return (float)stdH;
	}
	
	public void loadSeparators(){
		/*
		 * The format for separators should be:
		 * SEP_VALUES: 0, 0.01, 0.26, 0.4,0.65, 0.66, 0.97, 0.99, END.
		 */
		try {
			sc=new Scanner(new File("D:\\workspace\\ImprSearch\\data.txt"));
			//BufferedReader rd = new BufferedReader(new FileReader("data.txt"));
			
			ArrayList<Double> temp = new ArrayList<Double>();
			System.out.println("i am here to load hue seperators");
			while(sc.hasNextLine()){
				String line=sc.nextLine();
				if (!line.equals("")){
					temp.clear();
					//System.out.println(line);
					
					if(line.indexOf("SEP_VALUES:")==0){

						line=line.substring(line.indexOf(":")+1);
						String str=line.substring(0);
						
						while(str.indexOf(" ")==0){
							int index=str.indexOf(",");
							if(index!=-1){
								str=str.substring(1,index);
								try{
									Double d = Double.parseDouble(str);
									temp.add(d);
									//System.out.print(" \""+d);
								}catch(NumberFormatException e){
									e.printStackTrace();
								}
								if(temp.size()>NUM_SEPS)break;
								//System.out.println("");
								line=line.substring(index+1);
								str=line.substring(0);
							}else break;
						}
					}
					if(temp.size()%2!=0)temp.clear();
				}
				
			}
			
			Collections.sort(temp);
			for(int i=0; i<NUM_SEPS;i++){
				if(i<temp.size())HUE_SEPS[i]=temp.get(i);
				else HUE_SEPS[i]=-1;
			}
			sc.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		
		
	}
}

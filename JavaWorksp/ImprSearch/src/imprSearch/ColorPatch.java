package imprSearch;


import java.util.ArrayList;
import java.util.Collections;

public class ColorPatch {
	private int[] pixels;
	private int color;
	private int red;
	private int green;
	private int blue;
	private int parentWidth;
	private int parentHeight;
	
	/**
	 * Creates an object simulating a patch of color in a larger picture as a human perceives it
	 * @param width the width of the picture to which this patch belongs
	 * @param height the height of the picture to which this patch belongs
	 * @param pixels the array containing pixel coordinates of the patch
	 * @param color the average color of this patch in the parent picture
	 */
	public ColorPatch(int width, int height, int[] pixels, int color){
		parentWidth=width;
		parentHeight=height;
		this.pixels=pixels;
		sortPixels();
		this.color=color;
		red=(color>>16)&0xFF;
		green=(color>>8)&0xFF;
		blue=color&0xFF;
	}
	
	public int[] getPatchPixels(){
		return pixels;
	}
	
	public int getColor(){
		return color;
	}
	
	public int getRed(){
		return red;
	}
	
	public int getGreen(){
		return green;
	}
	
	public int getBlue(){
		return blue;
	}
	
	public int getParentWidth(){
		return parentWidth;
	}
	
	public int getParentHeight(){
		return parentHeight;
	}
	
	private void sortPixels(){
		ArrayList<Integer> temp = new ArrayList<Integer>();
		// :( can't think of anything better
		for(int i: pixels){
			temp.add(i);
		}
		Collections.sort(temp);
		
		for(int i=0; i<temp.size();i++){
			pixels[i]=temp.get(i);
		}
	}
	
	/**
	 * 
	 * @return dimensions[] = {xMin,xMax,yMin,yMax}
	 */
	public int[] getOccupiedDimensions(){
		int xMin=pixels[0]%parentWidth;
		int xMax=xMin;
		int yMin=pixels[0]/parentHeight;
		int yMax=yMin;
		
		for(int i:pixels){
			if(i%parentWidth<xMin) xMin=i%parentWidth;
			if(i%parentWidth>xMax) xMax=i%parentWidth;
			if(i/parentWidth<yMin) yMin=i/parentWidth;
			if(i/parentWidth>yMax) yMax=i/parentWidth;
		}
		
			int[] dimensions = {xMin,xMax,yMin,yMax};
			return dimensions;
	}
	
	/**
	 * Wait. not finished
	 * Bad play. This method only cuts patches into horizontal slices
	 * @param maxSize
	 * @return
	 */
	public ColorPatch[] getSubPatches(int maxSize){
		ArrayList<int[]> temp = new ArrayList<int[]>();
		int w=0;
		int l=0;
		int[] dimensions = getOccupiedDimensions();
		//if (dimensions[1]-dimensions[0]>dimensions[4]-dimensions[3]){
			l=dimensions[1]-dimensions[0];
			w=dimensions[3]-dimensions[2];
			int newPixels[] = new int[maxSize];
			int index=0;
			int i=0;
			while(index<pixels.length){
				
				newPixels[i]=pixels[index];
				index++;
				i++;
				if(i>=maxSize-1){
					temp.add(newPixels);
					newPixels = new int[maxSize];
					i=0;
				}
				
			}
			temp.add(newPixels);
			
		//}else{
			//w=dimensions[1]-dimensions[0];
			//l=dimensions[3]-dimensions[2];
		//}
		ColorPatch[] subs= new ColorPatch[temp.size()];
		for(int j=0;j<subs.length;j++){
			subs[j]= new ColorPatch(parentWidth,parentHeight,temp.get(j),color);
		}
		return subs;
		
	}
	
	public String toString(){
		return "Patch size: "+pixels.length+", patch color: "+red+", "+green+", "+blue+".";
	}
}

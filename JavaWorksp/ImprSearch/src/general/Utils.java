package general;
import static java.lang.Math.sqrt;
//import static java.lang.Math.abs;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * 
 */

/**
 * @author asus-pc
 *
 */
public abstract class Utils {
	//Note: Change here on May 13 and back to rgb on may 22 and back and forth...
	//and to argb again on August 20, 2016
	public static final int IMAGE_TYPE=BufferedImage.TYPE_INT_ARGB;
	
	public final static String DEPOSIT_POSITION = "D:\\zym\\Java Programming\\Imprs\\sketches";//"g:\\Projects\\Impression Search\\sketches";//
	public final static String DEFAULT_CANDI_FOLDER = "D:\\zym\\Java Programming";//"g:\\Projects\\Impression Search";//
	
	//draft input size
	public static final int DRAFT_HEIGHT = 100;//800//400
	public static final int DRAFT_WIDTH = 100;//1000//500
			
	/*preferred size for comparing NO SCALLING AFTERWARDS¡¡
	public static final int IMAGE_WIDTH = 500;
	public static final int IMAGE_HEIGHT = 400;
	*/
			
		
	private static final int MAX_LUMINANCE =1000;
	
	public static final int DISPLAY_WIDTH = 1000;//900;
	public static final int DISPLAY_HEIGHT = 800;//700;
	
	//public static final int SLIDING_STEP = 4;
	//public static final int MAX_AM_RATE = 2;
	//public static final double AM_RATE_STEP = 1.2;
	//public static final int MAX_FOLDS = 2;
	
	public static double computeStandardDeviation(int[] samples) {
		int sum = 0;
		for(int i: samples){
			sum += i;
		}
		double average = (double)sum/samples.length;
		double s = 0;
		for(int i: samples){
			s+=(i-average)*(i-average);
		}
		s = sqrt(s)/samples.length;
		return s;
	}
	
	/**
	 * make at least one side equal to destination size, 
	 * and ensure the whole returned dimension can be contained in the destination area,
	 * leaving empty space if necessary. 
	 * @param origW
	 * @param origH
	 * @param destiW
	 * @param destiH
	 * @return Dimension (width,height)
	 */
	public static Dimension scaleUniformFit(int origW, int origH, int destiW, int destiH){
		double ratio = (double)origW/origH;
		Dimension d = new Dimension();
		if ((double)destiW/destiH<ratio){
			d.width=destiW;
			d.height=(int)(destiW/ratio);
		}else{
			d.height=destiH;
			d.width=(int) (destiH*ratio);
		}
		return d;
	}
	
	/**
	 * make at least one side equal to destination size, 
	 * fill the destination region and leave out some extra area of the origin object  
	 * @param origW
	 * @param origH
	 * @param destiW
	 * @param destiH
	 * @return Dimension (width,height)
	 */
	public static Dimension scaleUniformFill(int origW, int origH, int destiW, int destiH){
		double ratio = (double)origW/origH;
		Dimension d = new Dimension();
		if ((double)destiW/destiH<ratio){
			d.height=destiH; //TODO: it seems that I've got it wrong here? 
			d.width=(int) (destiH*ratio);
		}else{
			d.width=destiW;
			d.height=(int)(destiW/ratio);
		}
		return d;
	}
	
	/**
	 * Applies the histogram equalization algorithm to the given image, represented by an array of color values
	 * 
	 * @param pixels The pixels of the input image.
	 * @return The equalized version of pixels of the image formed by applying histogram equalization.
	 */
	public static int[] applyHistogramEqualization(int[] pixels){
		int total = pixels.length;
		int[] luminances = new int[total];
		for(int i=0; i<total; i++){
			 int color = pixels[i];
			//luminances[i] = Math.round((float)(0.299*((color>>16)&0xFF)+0.587*((color>>8)&0xFF)+0.114*(color&0xFF)));
			 float[] hsbvals = new float[3];
			 hsbvals = Color.RGBtoHSB((color>>16)&0xFF, (color>>8)&0xFF, color&0xFF, hsbvals);
			luminances[i]=(int) (hsbvals[2]*MAX_LUMINANCE);
		}
		int[] equalized = new int[total];
		int[] histogram = histogramFor(luminances);
		int[] cumulativeArr = cumulativeSumFor(histogram);
		//for(int i:cumulativeArr){
			//System.out.println(i);
		//}
		
		for(int i=0; i<total;i++){
			
			equalized[i]=(cumulativeArr[luminances[i]]*MAX_LUMINANCE)/total;
			//TODO translate equalized array to color values
			Color c = new Color(pixels[i]);
			float[] hsbvals = new float[3];;
			hsbvals = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsbvals);
			equalized[i] = Color.HSBtoRGB(hsbvals[0], hsbvals[1], (float)equalized[i]/MAX_LUMINANCE);
		}
		
		
		return equalized;
	}
	

	
	/**
	 * Given the luminances of the pixels in an image, returns a histogram of the frequencies of
	 * those luminances.
	 * <p>
	 * You can assume that pixel luminances range from 0 to MAX_LUMINANCE, inclusive.
	 * 
	 * @param luminances The luminances in the picture.
	 * @return A histogram of those luminances.
	 */
	private static int[] histogramFor(int[] luminances) {
		/* TODO: Implement this method! */
		int[] histogram = new int[MAX_LUMINANCE+1];
		for (int j=0; j<luminances.length ; j++){
				histogram[luminances[j]]++;
		}
		return histogram;
	}
	
	public static int[] histogramFor(int[] samples, int max) {
		/* TODO: Implement this method! */
		int[] histogram = new int[max+1];
		for (int j=0; j<samples.length ; j++){
				histogram[samples[j]]++;
		}
		return histogram;
	}
	
	public static int[] histogramFor(float[] samples, int max) {
		/* TODO: Implement this method! */
		int[] histogram = new int[max+1];
		for (int j=0; j<samples.length ; j++){
				histogram[(int)samples[j]]++;
		}
		return histogram;
	}
	
	public static int[] histogramFor(ArrayList<Integer> samples, int max) {
		/* TODO: Implement this method! */
		int[] histogram = new int[max+1];
		for (int j=0; j<samples.size() ; j++){
				histogram[(int)samples.get(j)]++;
		}
		return histogram;
	}
	
	/**
	 * Given a histogram of the luminances in an image, returns an array of the cumulative
	 * frequencies of that image.  Each entry of this array should be equal to the sum of all
	 * the array entries up to and including its index in the input histogram array.
	 * <p>
	 * For example, given the array [1, 2, 3, 4, 5], the result should be [1, 3, 6, 10, 15].
	 * 
	 * @param histogram The input histogram.
	 * @return The cumulative frequency array.
	 */
	public static int[] cumulativeSumFor(int[] histogram) {
		/* TODO: Implement this method! */
		int[] cumulativeArr = new int[histogram.length];
		for (int i=0; i<histogram.length; i++){
			int sum=0;
			for (int j=0; j<=i; j++){
				sum+=histogram[j];
			}
			cumulativeArr[i]=sum;
		}
		return cumulativeArr;
	}
	
	/**
	 * findLocalMaximums including possible end points
	 * @param histogram
	 * @param threshold to specify the minimum value of local maximums to include
	 * @param step the degree to which the original histogram is compressed
	 * @return
	 */
	public static int[] findLocalMaximums(int[] histogram, int threshold, int step){ //threshold find Local Maximums
		int[] smaller = new int[histogram.length/step+1];
		int[] temp = new int[smaller.length];//-1?
		for(int i = 0; i<smaller.length; i++){
			int sum = 0;
			for(int j= i*step; j<i*step+step&&j<histogram.length; j++){
				sum += histogram[j];
			}
			smaller[i] = sum;
		}
		
		int last = 1; 
		int count = 0;
		for(int i = 1; i<smaller.length; i++){
			int cur = smaller[i]-smaller[i-1];
			temp[i-1]=-1;
			if(last > 0 && cur < 0 && smaller[i-1]>= threshold) {
				temp[i-1]=i*step-step/2;
				count++;
			}
			last = cur;
		}
		
		if(last>0 && smaller[smaller.length-1]>= threshold) {
			temp[smaller.length-1]= smaller.length*step-step/2; //to include the end points.
			count++;
		}else{
			temp[smaller.length-1]=-1;
		}
		
		int[] result = new int[count];
		int i = 0;
		for(int value:temp){
			if(value!=-1) {
				result[i] = value;
				i++;
			}
		}
		
		
		return result;
	}
	
	public static float[][] normalize1(float[][] matrix){
		int w = matrix[0].length;
		
		float[][] nmat = new float[matrix.length][w];
		float min = matrix[0][0];
		float max = min;
		
		for(int i=0; i<matrix.length*w; i++){
			if(matrix[i/w][i%w]>max) max=matrix[i/w][i%w];
			if(matrix[i/w][i%w]<min) min=matrix[i/w][i%w];
		}
		float range = max-min;
		for(int i=0; i<matrix.length*w; i++){
			nmat[i/w][i%w]=(matrix[i/w][i%w]-min)/range;
		}
		return nmat;
	}
	
	/**
	 * 
	 * @param r (a'-b')/(a-b)
	 * @param x
	 * @param offset O'- O
	 * @return
	 */
	public static int mapCoordinate(double r, int x, int offset){
		return (int)(r*x+offset);
	}
	

	
}

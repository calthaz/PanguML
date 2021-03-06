package zym.cv.findsimilarimg;


import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import javax.imageio.ImageIO;

import static java.lang.Math.sqrt;
import static java.lang.Math.abs;

/**
 * <span class="en">This class encapsulates an image instance in the algorithm described in
 * <a href="http://grail.cs.washington.edu/projects/query/mrquery.pdf">this paper</a><br>
 *  Now the color space is actually YIQ,
 * but I do bother to change the variable names</span>
 * <span class="zh">这个类封装了
 * <a href="http://grail.cs.washington.edu/projects/query/mrquery.pdf">这个论文</a>
 * 中的单个图片对象<br>
 * 现在用的颜色空间其实是YIQ，
 * 因为改变量名很麻烦所以没改
 * </span>
 */
public class ImageEntry implements Comparable<ImageEntry>,Serializable{

	private static final long serialVersionUID = 1L;

	private String path;
	private double score;
	private String pairedImprID;
	private BufferedImage compressed;
	
	/** <span class="zh">0,1,2 对应 h,s,b</span>
	 * <span class="en">0,1,2 correspond to h,s,b</span>*/
	public ArrayList<Float> average;
	
	private float[][] matrixH;
	private float[][] matrixS;
	private float[][] matrixB;
	
	/** <span class="zh">宽</span><span class="en">width</span>
	 */
	public static final int W = 128;//128
	/** <span class="zh">高</span><span class="en">height</span>*/
	public static final int H = 128;//128
	private static int TOP_M=60;//60
	
	/**
	 * <pre>
	 * dimension: value<br>
	 * 1st:       channel and sign [H+, H-, S+, S-, B+, B-] 通道和符号
	 * 2rd:       coordinates 坐标
	 * int[]      contains coefficients at [i,j]位置的系数
	 * </pre>	
	 */
	public ArrayList<ArrayList<int[]>> coefficients;
	private String myID;
	
	/**
	 * <span class="en">Use an image to initialize and process, so this.path is null.</span>
	 * <span class="zh">使用图片进行初始化和处理，因此this.path为null。</span>
	 * @param src
	 * @param ID
	 */
	public ImageEntry(BufferedImage src, String ID){
		path=null;
		myID=ID;
		process(src);
	}
	/**
	 * <span class="en">Use an image to initialize and process</span>
	 * <span class="zh">使用文件进行初始化和处理</span>
	 * @param path
	 * @param ID
	 * @throws IOException <span class="zh">如果文件读取失败</span><span class="en">If fails to read image </span>
	 */
	public ImageEntry(String path, String ID) throws IOException{
		myID=ID;
		this.path=path;
		BufferedImage src = ImageIO.read(new File(path));
		process(src);
	}
	
	/**
	 * <span class="zh">创建一个光秃秃，未初始化的图像实体，有score，ID和path</span>
	 * <span class="en">creat a bare, uninitialized ImageEntry with a score field, ID and a path</span>
	 * @param path 
	 * <span class="zh">图像的路径，没有尝试读取它</span>
	 * <span class="en">path to the image, no attempt to read it</span>
	 */
	public ImageEntry(String path) {
		myID="raw"+(int)Math.random()*100000;
		this.path=path;
	}

	private void process(BufferedImage src){
		compressed = new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);
		Graphics2D g = compressed.createGraphics();
		g.drawImage(src, 0, 0, W, H, null);
		g.dispose();
		int[] temp=new int[W*H];
		temp=compressed.getRGB(0, 0, W, H, temp, 0, W);
		toMatrix(temp);
		decomposeImage(matrixH);
		decomposeImage(matrixS);
		decomposeImage(matrixB);
		average=new ArrayList<Float>();
		
		coefficients = new ArrayList<ArrayList<int[]>>();
		
		ArrayList<ArrayList<int[]>> h = group(matrixH);
		ArrayList<ArrayList<int[]>> s = group(matrixS);
		ArrayList<ArrayList<int[]>> b = group(matrixB);
				
		coefficients.add(h.get(0));
		coefficients.add(h.get(1));
		coefficients.add(s.get(0));
		coefficients.add(s.get(1));
		coefficients.add(b.get(0));
		coefficients.add(b.get(1));
		
		/*
		System.out.println(coefficients.size());
		System.out.println(coefficients.get(0).size());
		System.out.println(coefficients.get(0).get(0).size()+" + "+coefficients.get(0).get(1).size()+" = 60");
		System.out.println(coefficients.get(1).get(0).size()+" + "+coefficients.get(1).get(1).size()+" = 60");
		System.out.println(coefficients.get(2).get(0).size()+" + "+coefficients.get(2).get(1).size()+" = 60");
		System.out.println(coefficients.get(0).get(0).get(0)[0]+","+coefficients.get(0).get(0).get(0)[1]);
		*/
	}
	
	private void toMatrix(int[] temp) {

		//pixels=new int[h][w];//row major??
		matrixH=new float[H][W];
		matrixS=new float[H][W];
		matrixB=new float[H][W];
		for(int i=0; i<temp.length;i++){
			int c=temp[i];
			//pixels[i/w][i%w]=temp[i];
			//float[] hsbvals = new float[3];
			//hsbvals=Color.RGBtoHSB((c>>16)&0xff, (c>>8)&0xff, c&0xff, hsbvals);
			//YIQ color space now. !!!
			int r = (c>>16)&0xff;
			int g = (c>>8)&0xff;
			int b = c&0xff;
			matrixH[i/W][i%W]= (float) (0.299*r+0.587*g+0.144*b);
			matrixS[i/W][i%W]= (float) (0.596*r-0.274*g-0.322*b);
			matrixB[i/W][i%W]= (float) (0.211*r-0.523*g+0.312*b);
		}
	}

	private static void decomposeImage(float[][] matrix) {
		
		for(float[] row:matrix){
			decomposeArray(row);
		}
		//printMatrix(matrix);
		transpose(matrix);
		//printMatrix(matrix);
		for(float[] col:matrix){
			decomposeArray(col);
		}
		//printMatrix(matrix);
		transpose(matrix);
		//printMatrix(matrix);
	}
	
	/**
	 * Still I don't know what this method does.<br>
	 * It says it's Haar wavelet in <a href="http://grail.cs.washington.edu/projects/query/mrquery.pdf">that paper</a>
	 * <a href="http://grail.cs.washington.edu/projects/query/mrquery.pdf">这个论文</a>说是Haar小波分析
	 * @param arr
	 */
	private static void decomposeArray(float[] arr) {

		int h=arr.length;
		float[] arr2 = new float[h];
		for(int i=0;i<h;i++){
			arr[i]=(float) (arr[i]/sqrt(h));
		}
		while(h>1){
			h/=2;
			for(int j=0; j<h; j++){
				arr2[j]=(float) ((arr[2*j]+arr[2*j+1])/sqrt(2));
				arr2[h+j]=(float) ((arr[2*j]-arr[2*j+1])/sqrt(2));
			}
			//arr=arr2;//note: address modification is not passed back.
			for(int i=0;i<arr.length;i++){
				arr[i]=arr2[i];
			}
		}
	}
	
	/**
	 * <pre>
	 * |1 2 3|    |1 4 7|
	 * |4 5 6| -> |2 5 8|
	 * |7 8 9|    |3 6 9|
	 * </pre>
	 * 
	 * @param matrix must be a square. No checking here
	 * @throws IndexOutOfBoundsException
	 */
	private static void transpose(float[][] matrix) throws IndexOutOfBoundsException{
		for(int row=0;row<matrix.length;row++){
			for(int col=0; col<row;col++){
				float temp=matrix[row][col];
				matrix[row][col]=matrix[col][row];
				matrix[col][row]=temp;
			}
		}
	}
	

	
	/**
	 * 
	 * @param matrix matrices of the result of Haar wavelet decomposition
	 * @return 2 arrayList, each contains a list of coordinates with the same sign
	 * 0, pos, 1, neg
	 */
	private ArrayList<ArrayList<int[]>> group(float[][] matrix){
		ArrayList<ArrayList<int[]>> groups = new ArrayList<ArrayList<int[]>>(); 
		//Cannot create a generic array of ArrayList<int[]> Simply can't
		ArrayList<int[]> pos = new ArrayList<int[]>();
		ArrayList<int[]> neg = new ArrayList<int[]>();
		groups.add(pos);
		groups.add(neg);
		//how to get the top mth values and its corresponding coordinate? I will use a fancy method here.
		ArrayList<Float> original = new ArrayList<Float>();
		ArrayList<Float> ranking = new ArrayList<Float>();
		for(int row=0;row<matrix.length;row++){
			for(int col=0;col<matrix[row].length;col++){
				original.add(matrix[row][col]);
				ranking.add(abs(matrix[row][col]));
			}
		}
		
		average.add(ranking.remove(0));//TODO: several averages. ??? 忘记了
		
		Collections.sort(ranking);
		//System.out.println(ranking);
		float min = ranking.get(ranking.size()-1);
		
		for(int i=ranking.size()-1; i>ranking.size()-TOP_M-1; i--){
			float val = ranking.get(i);
			int posi = original.indexOf(val);
			int negi = original.indexOf(-val);
			if(posi!=-1){
				int[] e = {posi/W,posi%W};
				original.set(posi, min);
				pos.add(e);
				//System.out.println("pos: "+e[0]+","+e[1]);
			}
			if(negi!=-1){
				int[] e = {negi/W,negi%W};
				original.set(negi,min);
				neg.add(e);
				//System.out.println("neg: "+e[0]+","+e[1]);
			}
		}
		return groups;
	}
	
	public String getPath(){
		return path;
	}
	/**
	 * <span class="en">get the matrix of the 1st color channel</span>
	 * <span class="zh">得到第一个颜色通道的矩阵</span>
	 * @return
	 */
	public float[][] getMatrixH() {
		return matrixH;
	}
	/**
	 * <span class="en">get the matrix of the 2nd color channel</span>
	 * <span class="zh">得到第二个颜色通道的矩阵</span>
	 * @return
	 */
	public float[][] getMatrixS() {
		return matrixS;
	}
	/**
	 * <span class="en">get the matrix of the 3rd color channel</span>
	 * <span class="zh">得到第三个颜色通道的矩阵</span>
	 * @return
	 */
	public float[][] getMatrixB() {
		return matrixB;
	}

	private static void printMatrix(float[][] matrix){
		System.out.println("");
		for(float[] row : matrix){
			System.out.print("|");
			for(float v: row){
				System.out.print(v+",");
			}
			System.out.println("|");
		}
	}
	
	public void setScore(float score) {
		this.score = score;
	}
	
	/**
	 * <span class="en">get id of another ImageEntry used to compare with this object.</span>
	 * <span class="zh">获取用于与此对象进行比较的另一个ImageEntry的id</span>
	 * @return id
	 */
	public String getPairedImprID() {
		return pairedImprID;
	}
	
	/**
	 * <span class="en">set id of another ImageEntry used to compare with this object.</span>
	 * <span class="zh">设置用于与此对象进行比较的另一个ImageEntry的id</span>
	 * @param pairedImprID
	 */
	public void setPairedImprID(String pairedImprID) {
		this.pairedImprID = pairedImprID;
	}
	
	
	public String toString(){
		String str=path+" compared with "+pairedImprID;
		/* No use to print them now+"|Image Entry info: |"
		 * for(ArrayList<ArrayList<int[]>> sign: coefficients){
			//str+="new component:\n";
			for(ArrayList<int[]> group: sign){
				//str+="new group:\n";
				for(int[] co: group){
					str+=String.format("{%s*%s}", co[0],co[1]);
				}
			}
			//str+="\n";
		}*/
		if(this.pairedImprID != null){
			str+=". Score:"+score;
		}
		return str;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score=score;
		
	}

	@Override
	public int compareTo(ImageEntry rhs) {
		if (score<rhs.getScore())return -1;
		if (score>rhs.getScore())return 1;
		else return path.compareTo(rhs.getPath()); 
	}
	
	/**
	 * 
	 * @return <span class="en">Original image that is compressed to {@code this.W} by {@code this.H}</span>
	 * <span class="zh">压缩成{@code this.W} x {@code this.H}的原始图片</span>
	 */
	public BufferedImage getDraft() {
		
		return compressed;
	}
	
	/**
	 * 
	 * @return <span class="en">Original image that is compressed to {@code this.W} by {@code this.H}</span>
	 * <span class="zh">压缩成{@code this.W} x {@code this.H}的原始图片</span>
	 */
	public BufferedImage display() {
		return getDraft();
	}

	public int getWidth() {
		return W;
	}

	public int getHeight() {
		return H;
	}

	public String getID() {
		return myID;
	}

	/**
	 * <span class="en">Test</span>
	 * <span class="zh">测试</span>
	 * @param args 
	 */
	public static void main(String[] args){
		float[][] matrix = {{(float) 0.7,(float) 0.2,(float) 0.3,(float) 0.5},
				{(float) 0.4,(float) 0.5,(float) 0.6,(float) 0.9},
				{(float) 0.7,(float) 0.8,(float) 0.9,(float) 0.3},
				{(float) 0.7,(float) 0.8,(float) 0.9,(float) 0.5}};
		printMatrix(matrix);
		//decomposeArray(matrix[0]);
		decomposeImage(matrix);
		printMatrix(matrix);
		//group(matrix);
		//transpose(matrix);
		//printMatrix(matrix);
		int[] a={1,2,3};
		int[] b={1,2,3};
		if(a.equals(b))System.out.println("a=b");
		else System.out.println("not equal");//why???
		
	}
	
	/**
	 * 
	 * <span class="en">delete coefficients and other unnecessary data so that this entry can be saved in a map</span>
	 * <span class="zh">删除coefficients和其他不必要的数据，以便该条目可以保存在map中</span>
	 * @return 
	 * <span class="en">an new bare ImageEntry with the average 
	 * and the path that are the same as this ImageEntry's</span>
	 * <span class="zh">另一个裸的ImageEntry，他有和这个ImageEntry一样的path和average.</span>
	 */
	public ImageEntry strip() {
		ImageEntry ret = new ImageEntry(this.path);
		ret.average = this.average;
		return ret;
	}

}

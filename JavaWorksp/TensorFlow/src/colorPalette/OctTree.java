package colorPalette;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import tools.ColorPaletteReader;
import tools.TFUtils;
/**
 * https://xcoder.in/2014/09/17/theme-color-extract/
 * https://www.microsoft.com/msj/archive/S3F1.aspx
 * https://github.com/XadillaX/theme-color-test
 *
 */
public class OctTree implements ColorExtractor{
	private static final int MIN_COLOR_COUNT = 12;
	private static final int MAX_PIC_SIZE = 150;
	//private static final int MAX_PER_CHANNEL = 255;
	//private static final int MAX_COLOR_VALUE = 16777215;
	private OctreeNode[] reducible = new OctreeNode[8];
	private int leafNum = 0; 
	private OctreeNode root = new OctreeNode();
	public OctTree(){
		for(int i = 0; i < 7; i++) {
			//reducible.add(null);	
			reducible[i] = null;
		}
	}
	/**
	 * if maxColors is less than the number of nodes in the first layer, 
	 * IndexOutOfBound will be thrown. so we force it to be more than 12, 
	 * than we will truncate or fill the return array so it has the same 
	 * number of colors. 
	 * @param img
	 * @param maxColors the number of output colors. 
	 * 	if not enough colors are extracted, the rgb array will be filled with {-1, -1, -1}
	 * @return {@code int[maxColors][3]} 
	 */
	public int[][] getRGBPalette(BufferedImage img, int maxColors){
		int outputLength = maxColors;
		if(maxColors<MIN_COLOR_COUNT){
			maxColors = MIN_COLOR_COUNT;
		}
		int w = img.getWidth();
		int h = img.getHeight();
		
		if(w>MAX_PIC_SIZE||h>MAX_PIC_SIZE){
			Dimension d = TFUtils.scaleUniformFit(w, h, MAX_PIC_SIZE, MAX_PIC_SIZE);
			img = TFUtils.getScaledImage(img, d.width, d.height);
			w = d.width;
			h = d.height;
		}
		int[] data = img.getRGB(0, 0, w, h, null, 0, w);

	    buildOctree(data, maxColors);

	    Map<Integer, Integer> colors = new HashMap<Integer, Integer>();
	    colorsStats(root, colors);

	    int[] counts = new int[maxColors];
	    int j = 0;
	    for(Map.Entry<Integer, Integer> en: colors.entrySet()){
	    	counts[j] = en.getValue();
	    	j++;
	    }
	    while(j<maxColors){
	    	counts[j] = -1;
	    	j++;
	    }
	    // ascending numerical order
	    Arrays.sort(counts);
	    //Arrays.sort
	    
	    int[][] ret = new int[outputLength][3];
	    for(int i=maxColors-1; i>=maxColors-outputLength; i--){
	    	if(counts[i]>=0){
	    		int c = getFirstKeyByValue(colors, counts[i]);
	    		ret[maxColors-1-i][0] = (c>>16)&0xFF;
	    		ret[maxColors-1-i][1] = (c>>8)&0xFF;
	    		ret[maxColors-1-i][2] = c&0xFF;
	    	}else{
	    		ret[maxColors-1-i][0] = -1;
	    		ret[maxColors-1-i][1] = -1;
	    		ret[maxColors-1-i][2] = -1;
	    	}
	    	
	    }
	    return ret;
	}
	
	/**
	 * perform the oct-tree algorithm and compared it with a palette
	 * @param palette path to the palette file, a reader will be constructed to read it
	 * @param img
	 * @param maxColors the number of output colors. 
	 * 	if not enough colors are extracted, the rgb array will be filled with {-1, -1, -1}
	 * @return
	 */
	public int[][] getPaletteAccordingTo(String palette, BufferedImage img, int maxColors){
		ColorPaletteReader rd = new ColorPaletteReader(palette);
		ArrayList<int[]> colors = rd.getRGBPalette();
		return getPaletteAccordingTo(colors, img, maxColors);
	}
	
	/**
	 * perform the oct-tree algorithm and compared it with a palette
	 * @param colors
	 * @param img
	 * @param maxColors the number of output colors. 
	 * 	if not enough colors are extracted, the rgb array will be filled with {-1, -1, -1}
	 * @return array of colors, in rgb arrays
	 */
	public int[][] getPaletteAccordingTo(ArrayList<int[]> colors, BufferedImage img, int maxColors){
		int[][] raw = getRGBPalette(img, maxColors);
		//int[][] ret = new int[raw.length][3];
		for(int i=0; i<raw.length; i++){
			int[] rgb = raw[i];
			
			int minDist = Integer.MAX_VALUE; 
			if(rgb[0]<0){
				continue;	
			}
			for(int[] pal : colors){
				int dist =  Math.abs(pal[0]-rgb[0])+Math.abs(pal[1]-rgb[1])+Math.abs(pal[2]-rgb[2]);
				if(dist<minDist){
					raw[i] = pal;
					minDist = dist;
				}
			}
			
		}
		return raw;
	}
	
	/**
	 * Find the first key in a map with the given value 
	 * and remove the entry
	 * @param map
	 * @param value
	 * @return the first key in the map that has the given value 
	 */
	private static <T, E> T getFirstKeyByValue(Map<T, E> map, E value) {
		T key = null;
	    for (Entry<T, E> entry : map.entrySet()) {
	        if (Objects.equals(value, entry.getValue())) {
	            key = entry.getKey();
	            map.remove(key, value);
	            break;
	        }
	    }
	    return key;
	}

	
	private class OctreeNode{
		public boolean isLeaf;
		public int pixelCount;
		public int red;
		public int green;
		public int blue;
		public OctreeNode[] children;
		private OctreeNode next;

		public OctreeNode(){
			this.isLeaf = false;
		    this.pixelCount = 0;
		    this.red = 0;
		    this.green = 0;
		    this.blue = 0;

		    this.children = new OctreeNode[8];
		    for(int i = 0; i < this.children.length; i++) this.children[i] = null;

		    // 这里的 next 不是指兄弟链中的 next 指针
		    // 而是在 reducible 链表中的下一个节点
		    this.next = null;
		}
	}
	/**
	 * createNode
	 * if level<7 put it in the reducible[level] 
	 * and link node.next to what was there previously 
	 * @param parent {OctreeNode} parent the parent node of the new node
	 * @param idx {Number} idx child index in parent of this node
	 * @param level {Number} level node level
	 * @return {OctreeNode} the new node
	 */
	private OctreeNode createNode(OctreeNode parent, int idx, int level) {
	    OctreeNode node = new OctreeNode();
	    if(level == 7) {
	        node.isLeaf = true;
	        leafNum++;
	    } else {
	        //node.next = reducible.get(level);
	    	node.next = reducible[level];
	        //reducible.set(level, node);
	        reducible[level] = node;
	        
	    }

	    return node;
	}
	/**
	 * addColor <b>Note:recursive</b><br>
	 * add color to the tree. For example:<br>
	 * <pre>
	 * R: 0110 1101
	 * G: 1100 1100
	 * B: 1010 1010
	 * </pre>
	 * the path to its leaf (if not trimmed) is
	 * <pre>
	 * index:   011=>110=>101=>000=>111=>110=>001=>100
	 * children: 3 => 6 => 5 => 0 => 7 => 6 => 1 => 4
	 * </pre>
	 * and this leaf will have 
	 * <pre>
	 * pixelCount++;
	 * red+=R;
	 * green+=G;
	 * blue+=B;
	 * </pre>
	 * @param node {OctreeNode} node the octree node
	 * @param color {int} color color object
	 * @param level {int} level node level
	 */
	private void addColor(OctreeNode node, int color, int level) {
		int red = (color>>16)&0xFF;
		int green = (color>>8)&0xFF;
		int blue = color&0xFF;
	    if(node.isLeaf) {
	        node.pixelCount++;
	        node.red += red;
	        node.green += green;
	        node.blue += blue;
	    } else {
	    	int r = (red >> (7 - level)) & 1;
	        int g = (green >> (7 - level)) & 1;
	        int b = (blue >> (7 - level)) & 1;

	        int idx = (r << 2) + (g << 1) + b;

	        if(null == node.children[idx]) {
	            node.children[idx] = createNode(node, idx, level + 1);
	        }

	        //if(undefined == node.children[idx]) {
	            //console.log(color.r.toString(2));
	        //}

	        addColor(node.children[idx], color, level + 1);
	    }
	}
	
	/**
	 * trim the leaves<br>
	 * adds leaves together and change their parent into a leaf node
	 */
	private void reduceTree() {
	    // find the deepest level of node
	    int lv = 6;
	    //while(null == reducible.get(lv)) {
	    while(null == reducible[lv]){
	    	lv--;
	    	if(lv<0)break;//TODO I add this to avoid some overflow problems, but it may cause infinite loops as well. 
	    }

	    // get the node and remove it from reducible link
	    //OctreeNode node = reducible.get(lv);
	    OctreeNode node = reducible[lv];
	    //reducible.set(lv, node.next);
	    reducible[lv] = node.next;
	    
	    // merge children
	    int r = 0;
	    int g = 0;
	    int b = 0;
	    int count = 0;
	    for(int i = 0; i < 8; i++) {
	        if(null == node.children[i]) continue;
	        r += node.children[i].red;
	        g += node.children[i].green;
	        b += node.children[i].blue;
	        count += node.children[i].pixelCount;
	        leafNum--;
	    }

	    node.isLeaf = true;
	    node.red = r;
	    node.green = g;
	    node.blue = b;
	    node.pixelCount = count;
	    leafNum++;
	}
	
	/**
	 * buildOctree
	 * 
	 * @param pixels pixels The pixels array
	 * @param maxColors maxColors The max count for colors
	 * @return 
	 */
	private void buildOctree(int[] pixels, int maxColors) {
	    for(int i = 0; i < pixels.length; i++) {
	        // 添加颜色
	        addColor(root, pixels[i], 0);

	        // 合并叶子节点
	        while(leafNum > maxColors) {
	        	reduceTree();
	        }
	    }
	}
	
	/**
	 * colorsStats
	 * reads the colors in the leaves by {@code leaf.color/leaf.pixelCount}<br>
	 * <b>Note:recursive</b>
	 * @param {OctreeNode} node the node will be stats
	 * @param {Object} object color stats
	 * @return {undefined}
	 */
	private void colorsStats(OctreeNode node, Map<Integer, Integer> object) {
	    if(node.isLeaf) {
	        int r = node.red / node.pixelCount;
	        int g = node.green / node.pixelCount;
	        int b = node.blue / node.pixelCount;
	        int color = (r << 16) | (g << 8) | b;
	        if(object.containsKey(color)) {
	        	//object[color] += node.pixelCount;
	        	object.put(color, object.get(color)+node.pixelCount);
	        }
	        else object.put(color, node.pixelCount);
	        
	        return;
	    }

	    for(int i = 0; i < 8; i++) {
	        if(null != node.children[i]) {
	            colorsStats(node.children[i], object);
	        }
	    }
	}
	/**
	 * makes a tree ready for another image
	 */
	public void clear() {	
		leafNum = 0; 
		root = new OctreeNode();
		for(int i = 0; i < 7; i++) {
			reducible[i] = null;
			//reducible.set(i, null);	
		}
	}
	

}

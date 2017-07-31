package colorPalette;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class OctTree {
	private ArrayList<OctreeNode> reducible = new ArrayList<OctreeNode>();
	private int leafNum = 0; 
	private OctreeNode root = new OctreeNode();
	public OctTree(){
		for(int i = 0; i < 7; i++) reducible.add(null);	
	}
	
	public int[][] getPalatte(BufferedImage img, int maxColors){
		int w = img.getWidth();
		int h = img.getHeight();
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
	    Arrays.sort(counts);
	    
	    int[][] ret = new int[maxColors][3];
	    for(int i=maxColors-1; i>=0; i--){
	    	if(counts[i]>0){
	    		int c = getFirstKeyByValue(colors, counts[i]);
	    		ret[maxColors-1-i][0] = (c>>16)&0xFF;
	    		ret[maxColors-1-i][1] = (c>>8)&0xFF;
	    		ret[maxColors-1-i][2] = c&0xFF;
	    	}
	    	
	    }
	    return ret;
	}
	
	private static <T, E> T getFirstKeyByValue(Map<T, E> map, E value) {
		T key = null;
	    for (Entry<T, E> entry : map.entrySet()) {
	        if (Objects.equals(value, entry.getValue())) {
	            key = entry.getKey();
	            break;
	        }
	    }
	    map.remove(key);
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
	 *
	 * @param {OctreeNode} parent the parent node of the new node
	 * @param {Number} idx child index in parent of this node
	 * @param {Number} level node level
	 * @return {OctreeNode} the new node
	 */
	private OctreeNode createNode(OctreeNode parent, int idx, int level) {
	    OctreeNode node = new OctreeNode();
	    if(level == 7) {
	        node.isLeaf = true;
	        leafNum++;
	    } else {
	        node.next = reducible.get(level);
	        //reducible[level] = node;
	        reducible.set(level, node);
	    }

	    return node;
	}
	/**
	 * addColor
	 *
	 * @param {OctreeNode} node the octree node
	 * @param {Object} color color object
	 * @param {Number} level node level
	 * @return {undefined}
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
	 * reduceTree
	 *
	 * @return {undefined}
	 */
	private void reduceTree() {
	    // find the deepest level of node
	    int lv = 6;
	    while(null == reducible.get(lv)) {
	    	lv--;
	    	if(lv<0)break;//TODO I add this to avoid some overflow problems, but it may cause infinite loops as well. 
	    }

	    // get the node and remove it from reducible link
	    OctreeNode node = reducible.get(lv);
	    //reducible[lv] = node.next;
	    reducible.set(lv, node.next);

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
	 *
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

	public void clear() {
		// TODO Auto-generated method stub
		reducible = new ArrayList<OctreeNode>();
		leafNum = 0; 
		root = new OctreeNode();
		for(int i = 0; i < 7; i++) reducible.add(null);	
	}
	

}

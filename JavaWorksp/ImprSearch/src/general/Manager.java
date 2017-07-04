package general;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.table.AbstractTableModel;

/*
 * http://stackoverflow.com/questions/15311757/java-abstract-class-fields-override
 * In Java, data members/attributes are not polymorphic. 
 * Overloading means that a field will have a different value depending from which class it's accessed. 
 * The field in the subclass will hide the field in the super-class, but both exists. 
 * The fields are invoked based on reference types, while methods are used of actual object. 
 * It's called, variable hiding/shadowing, for more details look on ...
 */

/**
 * This guy sits behind the GUI desk and keep all the information about the candidates and the drafts.
 * when comparing is requested by the customer, this guy goes to work on himself,  
 * mean while the assistant at the desk should inform other customers that their manager is busy...
 */
public abstract class Manager implements Runnable {
	protected ArrayList<Candidate> candiList; //= new ArrayList<Candidate>();
	public  MyTableModel tableModel; //= new MyTableModel(); //AbstractTableModel
	
	protected ArrayList<String> imprList; //= new ArrayList<String>();
	public MyComboBoxModel cbModel; //= new MyComboBoxModel();
	
	protected HashMap<String, Query> imprStock; //= new HashMap<String, Query>();
	protected String imprID;
	protected BufferedImage imprDraft;
	
	protected String mode;
	
	public double AM_RATE_STEP = 1.2;
	public double MAX_AM_RATE=2;
	public int MAX_FOLDS=2;
	public double CENTER_X=0.5;
	public double CENTER_Y=0.5;
	public double SEARCH_W=0.2;
	public double SEARCH_H=0.2;
	public int SLIDING_STEP = 4;
	public int probFunc = 1;
	
	public String progressFile = "progress.txt";
	public String resultFile = "result.txt";
	
	public int searchMethod;
	public int preprocessing;
	
	//should these models be public???
	@SuppressWarnings("serial")
	public class MyTableModel extends AbstractTableModel{
		
		private ArrayList<Candidate> data = candiList;
		private String[] columnNames = {"File path","Compared Impression","Score"};
		
		public void setData(ArrayList<Candidate> list){
			data=list;
		}
		
		@Override
		public int getColumnCount() {
		
			return columnNames.length;
		}

		@Override
		public int getRowCount() {
	
			return data.size();
		}

		 public String getColumnName(int col) {
	            return columnNames[col];
	     }
		 
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
		
			switch (columnIndex){
				case 0: return data.get(rowIndex).getPath();
				case 1: return data.get(rowIndex).getPairedImprID();
				case 2: return data.get(rowIndex).getScore();
				default: return null;
			}
		}
		
		public boolean isCellEditable(int row, int col) {
			 return false;
		}
	}
	//------------------------------------------------------------------------
	@SuppressWarnings("serial")
	public class MyComboBoxModel extends DefaultComboBoxModel<String>{
		private ArrayList<String> items = imprList;
		private String selectedObject;
		// implements javax.swing.ListModel
	    @Override
	    public int getSize() {
	    	
	        return items.size();
	    }
	    
	    // implements javax.swing.ListModel
	    @Override
	    public String getElementAt(int index) {
	        if (index >= 0 && index < items.size()) {
	            return items.get(index);
	        } else {
	            return null;
	        }
	    }
	    
	    /**
	     * Returns the index-position of the specified object in the list.
	     *
	     * @param anObject
	     * @return an int representing the index position, where 0 is
	     *         the first position
	     */
	    public int getIndexOf(String anObject) {
	        return items.indexOf(anObject);
	    }
	    
	 // implements javax.swing.MutableComboBoxModel
	    public void addElement(String anObject) {
	        items.add(anObject);
	        fireIntervalAdded(this, items.size() - 1, items.size() - 1);
	        if (items.size() == 1 && selectedObject == null && anObject != null) {
	            setSelectedItem(anObject);
	        }
	    }

	 // implements javax.swing.MutableComboBoxModel
	    public void insertElementAt(String anObject, int index) {
	        items.add(index, anObject);
	        fireIntervalAdded(this, index, index);
	    }
	    
	 // implements javax.swing.MutableComboBoxModel
	    public void removeElementAt(int index) {
	        if (getElementAt(index) == selectedObject) {
	            if (index == 0) {
	                setSelectedItem(getSize() == 1 ? null : getElementAt(index + 1));
	            } else {
	                setSelectedItem(getElementAt(index - 1));
	            }
	        }

	        items.remove(index);

	        fireIntervalRemoved(this, index, index);
	    }

	 // implements javax.swing.MutableComboBoxModel
	    public void removeElement(String anObject) {
	        int index = items.indexOf(anObject);
	        if (index != -1) {
	            removeElementAt(index);
	        }
	    }
	    
	    /**
	     * Empties the list.
	     */
	    public void removeAllElements() {
	        if (items.size() > 0) {
	            int firstIndex = 0;
	            int lastIndex = items.size() - 1;
	            items.clear();
	            selectedObject = null;
	            fireIntervalRemoved(this, firstIndex, lastIndex);
	        } else {
	            selectedObject = null;
	        }
	    }
	    /*
	    @Override
	    public void add(List elementsToAdd) {
	        items.addAll((Collection<? extends String>) elementsToAdd);
	        fireContentsChanged(this, -1, -1);

	    }

	    @Override
	    public List getElements() {
	        return items;
	    }
		*/

	}
	
	public abstract void loadFiles(File inFile);
	public abstract boolean loadFiles(String path);
	
	public void clearCandiList() {
		candiList.clear();
		if(mode.equals("gui")){
			tableModel.fireTableDataChanged();
		}
	}
	
	/**
	 * For gui mode
	 * @param draft snatched from the sketch pad
	 * @param reliability not used
	 * @param name
	 */
	public void receiveDraft(BufferedImage draft, int reliability, String name) {
		this.imprDraft = draft;
		//this.rly = reliability;
		imprID=name;
	}
	
	/**
	 * for text mode
	 * imprID is just the file name. 
	 * @param path
	 */
	public boolean receiveDraft(String path){
		File f=new File(path);
		if(f.isFile()){
			try {			
				this.imprDraft=ImageIO.read(f);
				imprID=f.getName();
				return true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			System.out.println("It is not a normal file. "+path);
			return false;
		}
		return false;
		
	}
	
	/**
	 * 
	 * @return null if in text mode! This method should not be called under text mode
	 */
	public BufferedImage getImpr() {
		if(mode.equals("gui")){
			String imprName = (String)cbModel.getSelectedItem();
			System.out.println("Opening: " + imprName + " from impression list." );
			return imprStock.get(imprName).display();//getDraft();
		}else{
			return null;
		}
	}
	
	public String getImpr(String id){
		return imprStock.get(id).toString(); //too bad. only a string? 
	}
	
	/**
	 * Considering the large space of a single picture, I don't want to initialize the clip field in windows here
	 * @param candi
	 * @param draft
	 * @return array of empty windows! no data, just some dimensions
	 */
	public ArrayList<CandiWindow> getEmptyWindows(BufferedImage candi, BufferedImage draft){
		int candiW = candi.getWidth();
		int candiH=candi.getHeight();
		int draftW=draft.getWidth();
		int draftH=draft.getHeight();
		ArrayList<CandiWindow> winds = new ArrayList<CandiWindow>();
		Dimension smallest = Utils.scaleUniformFill(candiW, candiH, draftW, draftH);
		int w=smallest.width;
		int h =smallest.height;
		double amRate=(double)smallest.width/candiW;
		int step = SLIDING_STEP;
		int x1,y1;
		x1=y1=0;
		//x2=draftW-1;
		//y2=draftH-1;
		//Just add one as a backup incase there can't be any window at all
		CandiWindow cw = new CandiWindow(draftW,draftH,(int)(x1/amRate),(int)(y1/amRate),(int)((x1+draftW-1)/amRate),(int)((y1+draftH-1)/amRate));
		winds.add(cw);
		do{
			//horizontal step
			for(x1=0; x1+draftW<=w;x1+=step){ //TODO border check
				//vertical step
				for(y1=0;y1+draftH<=h;y1+=step){
					if(isInRect(x1,y1,draftW,draftH,w,h)){
						cw = new CandiWindow(draftW,draftH,(int)(x1/amRate),(int)(y1/amRate),(int)((x1+draftW-1)/amRate),(int)((y1+draftH-1)/amRate));
						winds.add(cw);
					}
				}
				
			}
			
			amRate*=AM_RATE_STEP;
			w*=AM_RATE_STEP;
			h*=AM_RATE_STEP;
		}while(amRate<MAX_AM_RATE&&((w/draftW<MAX_FOLDS)||(h/draftH<MAX_FOLDS)));
		System.out.println("Windows count: "+winds.size()+"!!!!!!!!!!!!!!!!!!!!!");
		return winds;
	}
	
	private boolean isInRect(int x1, int y1, int draftW, int draftH, int w, int h) {
		double contract = SEARCH_W*w/SEARCH_H/h;
		int cx=x1+(draftW)/2;
		int cy = y1+(draftH)/2;
		double r =  Math.sqrt(Math.abs(cx-CENTER_X*w)*Math.abs(cx-CENTER_X*w) + Math.abs(cy-CENTER_Y*h)*Math.abs(cy-CENTER_Y*h)*contract*contract);//TODO times or divided by???
		double likelihood = func(this.probFunc,2*r/(SEARCH_W*w));//TODO 
		//if(Math.abs(cx-CENTER_X*w)<=(w*SEARCH_W)/2 && Math.abs(cy-CENTER_Y*h)<=(h*SEARCH_H)/2)
		//System.out.println((Math.abs(cy-CENTER_Y*h))+", "+ (Math.abs(cx-CENTER_X*w))+") "+ 2*r+"; \n "+(SEARCH_H*h)+"; "+(SEARCH_W*w)+"; "+likelihood);
		
		return  Math.random()<=likelihood && Math.abs(cx-CENTER_X*w)<=(w*SEARCH_W)/2 && Math.abs(cy-CENTER_Y*h)<=(h*SEARCH_H)/2;//
		
	}//Total file count: 9. Total window count: 445. Comparing has taken 2801ms.
	//Total file count: 9. Total window count: 248. Comparing has taken 1864ms.
	
	private double func(int type, double x){
		switch(type){
		case 0:
			return -x*x+1; //0.5235 real: 12/30
		case 1:
			return -x*x*x+1;//0.5890 real 14/30
		case 2:
			return 1/(x-2)/2+(double)5/4; //0.9034 Hey, remember to cast number to double, too! 
		case 3:
			return 1/(x+1); 
		default:
			return 1;
		case -1:
			return -1;
		case -2:
			return 1;
		}
		
	}
	
	
	public abstract BufferedImage showComparision(int candiIndex);
	public abstract int[] showPreference();
	public abstract String printSettings();
	public abstract void modifyPreference(int[] pref);
	
	public double getMaxAmRate() {
		return MAX_AM_RATE;
	}
	public void setMaxAmRate(double max_AM_RATE) {
		MAX_AM_RATE = max_AM_RATE;
	}
	public int getMaxFolds() {
		return MAX_FOLDS;
	}
	public void setMaxFolds(int max_FOLDS) {
		if(max_FOLDS>0){
			MAX_FOLDS = max_FOLDS;
		}
		
	}
	public double getCenterX() {
		return CENTER_X;
	}
	public boolean setCenterX(double center_X) {
		if(center_X>=0&&center_X<=1){
			CENTER_X = center_X;
			return true;
		}
		return false;
		
	}
	public double getCenterY() {
		return CENTER_Y;
	}
	public boolean setCenterY(double center_Y) {
		if(center_Y>=0&&center_Y<=1){
			CENTER_Y = center_Y;
			return true;
		}
		return false;
	}
	public void setProgressFile(String string) {
		// TODO Auto-generated method stub
		this.progressFile = string;
	}

	public void setOutputFile(String string) {
		// TODO Auto-generated method stub
		this.resultFile = string;
	}
}


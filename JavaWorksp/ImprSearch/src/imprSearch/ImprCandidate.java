package imprSearch;

import general.CandiWindow;
import general.Candidate;

/**
 * This class stores the information of a candidate in the search
 * @author asus-pc
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
public class ImprCandidate implements Candidate{
	private double score;
	private final String origPath;
	private String pairedImprID;
	private int[] patchesWined;
	private CandiWindow myCw = new CandiWindow(0,0,0,0,0,0);
	
	public ImprCandidate(String pathName){
		 origPath = pathName;
	}

	public int[] getPatchesWined() {
		return patchesWined;
	}

	public void setPatchesWined(int[] patchesWined) {
		this.patchesWined = patchesWined;
	}

	public String getPairedImprID() {
		return pairedImprID;
	}

	public void setPairedImprID(String pairedImprID) {
		this.pairedImprID = pairedImprID;
	}

	public String getPath() {
		return origPath;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	/*
	@Override
	public int compareTo(ImprCandidate rhs) {
		if (score<rhs.score)return 1;
		if (score>rhs.score)return -1;
		else return origPath.compareTo(rhs.origPath); 
		//TODO why can this candidate access the private field of the other?
		
	}*/
	
	public String toString(){
		String str = origPath;
		if(pairedImprID==null){
			return str+"Not yet compared with anything. ";
		}
		str+=" compared with "+pairedImprID+" wins "+patchesWined.length+" color patches. Score:"+score;
		return str;
	}

	/**
	 * why there are two compareTo methods?
	 * That's wait and see.
	 */
	@Override
	public int compareTo(Candidate rhs) {
		// TODO Auto-generated method stub
		if (score<rhs.getScore())return 1;
		if (score>rhs.getScore())return -1;
		else return origPath.compareTo(rhs.getPath()); 
	}

	@Override
	public void SetWinner(CandiWindow cw) {
		// TODO Auto-generated method stub
		myCw = cw;
	}

	@Override
	public CandiWindow getWinner() {
		// TODO Auto-generated method stub
		return myCw;
	}
	
	
}

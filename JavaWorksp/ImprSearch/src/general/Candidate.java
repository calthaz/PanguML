package general;



public interface Candidate extends Comparable<Candidate>{//implements Comparable
	String getPairedImprID();
	void setPairedImprID(String pairedImprID);
	String getPath();
	double getScore();
	void setScore(double score);
	//int compareTo(Candidate rhs);
	String toString();
	void SetWinner(CandiWindow cw);
	CandiWindow getWinner();
}

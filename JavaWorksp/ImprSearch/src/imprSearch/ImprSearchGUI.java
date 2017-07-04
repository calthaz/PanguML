package imprSearch;
/* Version-2
 * This program is intended for searching for a image based on a user's drawing from his or her memory. 
 */
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;

import general.Utils;
import general.SketchPad;

/**
 * The GUI of the program.
 * It contains the main method.
 * Handles searching requests from the user 
 * and exchanges data with both the SketchPad and the SearchManager
 * @author asus-pc
 *
 */
@SuppressWarnings("serial")
public class ImprSearchGUI extends JFrame implements ActionListener,ListSelectionListener {
	private static SketchPad pad = new SketchPad();
	
	//command panel
	public static final int COMMAND_HEIGHT = 180;
	private JButton start,chooseFile,deleteFile,preference;
	private JTable resultT;
	private ListSelectionModel listSM;
	private JComboBox<String> imprList;
	
	private ImprManager manager = new ImprManager();
	
	public ImprSearchGUI(){
		super("Impression Search");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
	    this.setExtendedState(JFrame.MAXIMIZED_VERT);//what's this?Extends it to fill the screen
		
		Container con = this.getContentPane();
	    con.setLayout(new BorderLayout());    
	    con.add(pad);
	    
	    /*filePane is the one that deals with saving, opening, choosing files.*/
	    JPanel filePane = new JPanel();
	    filePane.setLayout(new GridBagLayout());
	    start = new JButton("Start Searching");
	 
	    imprList = new JComboBox<String>(manager.cbModel);
	    imprList.addActionListener(this);
	    
	    resultT = new JTable(manager.tableModel);
	    resultT.setFillsViewportHeight(true);
	    //Regard this as an action event
	    listSM = resultT.getSelectionModel();
        listSM.addListSelectionListener(this);
      
        resultT.getColumnModel().getColumn(1).setPreferredWidth(50);
        resultT.getColumnModel().getColumn(2).setPreferredWidth(10);//TODO no use?
        //TODO hey, the rows are sorted, but the files are not. 
        //Demonstrates sorting and filtering, and how this can cause the view coordinates to diverge from the model coordinates.
        //the scroller that holds the result table
	    JScrollPane sp=new JScrollPane(resultT);
	    sp.setPreferredSize(new Dimension(SketchPad.CANVAS_WIDTH,COMMAND_HEIGHT));
	    
	    chooseFile = new JButton ("Choose a file");
	    deleteFile = new JButton("Delete All from List");
	    preference = new JButton("Set searching preference");
	    //JFileChooser fc = new JFileChooser("...");
	    
	    organizeFilePane(filePane,sp);
	    
	    this.add(filePane,BorderLayout.SOUTH);
	
	    start.addActionListener(this);
	    chooseFile.addActionListener(this);
	    deleteFile.addActionListener(this);
	    preference.addActionListener(this);
	    
	    this.pack();
	    this.setVisible(true);
	
	}
	
	private void organizeFilePane(JPanel filePane, JScrollPane sp) {
		
		GridBagConstraints c = new GridBagConstraints();
		c.weightx=0.9;
		c.weighty=0.5;
		c.fill=GridBagConstraints.HORIZONTAL;
		
		c.gridx=0;
		c.gridy=0;
		filePane.add(imprList, c);
		
		c.gridx=0;
		c.gridy=1;
		c.gridheight=3;
		filePane.add(sp, c);
		
		c.weightx=0.1;
		c.gridx=1;
		c.gridy=0;
		c.gridheight=1;
		filePane.add(chooseFile, c);
		
		c.gridx=1;
		c.gridy=1;
		c.gridheight=1;
		filePane.add(deleteFile, c);
		
		c.gridx=1;
		c.gridy=2;
		c.gridheight=1;
		filePane.add(start, c);
		
		c.gridx=1;
		c.gridy=3;
		filePane.add(preference, c);
		
	}
	
	public static void main(String[] args) {
		//Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	
                new ImprSearchGUI();
               
            }
        });
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		//imprList.setSelectedIndex(-1);
		if(!e.getValueIsAdjusting() && resultT.getSelectedColumn()!= -1){//
			
			int r = resultT.getSelectedRow();
			int c = resultT.getSelectedColumn();
			//File f = new File((String) fileData.elementAt(r).elementAt(0));
			File f = new File((String) resultT.getModel().getValueAt(r, 0));
			pad.displayImage(f);
			if (c==0||c==1) {
				
				System.out.println("Opening: " + f.getName() + " from file list." );
				
			}else{
				
				pad.displayMask(manager.showComparision(r));
			}
			
			
			
		}
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource()==start){
			
    	    System.out.println("Manager starts to work. Don't disturb him... ");
    	    
    	    manager.receiveDraft(pad.getDraft(),pad.getRly(),pad.getDraftName());
    	    Thread comparing = new Thread(manager,"Search-Manager");
    	    comparing.start();

    	    
		}else if(e.getSource()==chooseFile){
			
			JFileChooser chooser = new JFileChooser(Utils.DEFAULT_CANDI_FOLDER);
			
			FileNameExtensionFilter filter = new FileNameExtensionFilter("JPG & GIF & PNG Images", "jpg", "gif","png");//TODO really?
			chooser.setFileFilter(filter);
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);//.DIRECTORIES_ONLY
			int returnVal = chooser.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
	            File file = chooser.getSelectedFile();
	            //File folder = chooser.getCurrentDirectory();
	            //fileNames.addElement(file.getPath());
	            manager.loadFiles(file);
	            
	        } else {
	        	System.out.println("Open command cancelled by user." );
	        }
			
		}else if(e.getSource()== imprList){
			//resultT.clearSelection();
			pad.displayImage(manager.getImpr());
			
		}else if(e.getSource()== deleteFile){
			
			manager.clearCandiList();
		}else if(e.getSource()== preference){
			int[] pref = manager.showPreference();
			JFrame prefDialog = new JFrame("Set Search Preferences");
			prefDialog.setSize(400, 400); 
			Container con = prefDialog.getContentPane();
			con.setLayout(new FlowLayout());
			con.add(new JLabel("Searching method"));
			
			ButtonGroup search = new ButtonGroup();
			JRadioButton rgbS = new JRadioButton("RGB Search");
			JRadioButton hsbS = new JRadioButton("HSB Search");
			search.add(rgbS);
			search.add(hsbS);
			con.add(rgbS);
			con.add(hsbS);
			con.add(new JLabel("Preprocessing"));
			//JCheckBox none = new JCheckBox("None");
			JCheckBox equal = new JCheckBox("Histogram Equalization");
			//con.add(none);
			con.add(equal);
			con.add(new JButton("Apply change"));
			con.add(new JButton("Cancel"));
			prefDialog.setVisible(true);
			//TODO implement later
		}
	}

}

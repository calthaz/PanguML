package testers;

import java.awt.Dimension;

import javax.swing.JFrame;

import general.SketchPad;

@SuppressWarnings("serial")
public class SketchPadDriver extends JFrame{
	
	public SketchPadDriver(){
		super("Sketch Pad Tester");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setPreferredSize(new Dimension(1000, 800));
		this.add(new SketchPad());
		this.pack();
		this.setVisible(true);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	
                new SketchPadDriver();
               
            }
        });
	}

}

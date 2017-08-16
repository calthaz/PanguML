package general;

import fastQuerying.QueryManager;
import imprSearch.ImprManager;


public class SearchGUIDriver {

	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
            	
                //new SearchGUI(new ImprManager());
                new SearchGUI(new QueryManager());
            }
        });
	}

}

package general;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import fastQuerying.QueryManager;
import general.Manager;
import imprSearch.ImprManager;
import imprSearch.Impression;
import general.TextSettingsReader;

public class QueryAgent {
	
	//public QueryAgent(){
		
	//}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if(args.length > 0){
			System.out.println("Query Accepted");
			String settingsPath = args[0];
			TextSettingsReader rd;
			//System.out.println("In query agent");
			try {
				//System.out.println("In query agent try");
				rd = new TextSettingsReader(settingsPath);
				String prg = rd.settings.get("progressPlace");
				PrintWriter wr=new PrintWriter(new FileWriter(prg),true);
				wr.println("Start: Thread start");
				Manager manager;
				if(rd.settings.get("MANAGER_TYPE").equals("ImprSearch")){
					manager = new ImprManager("text");
				}else{
					manager = new QueryManager("text");
				}
				
				//System.out.println("In query agent try1");
				boolean msg = manager.receiveDraft(rd.settings.get("draftPath"));
				if(!msg){
					wr.println("Fatal Error: failed to load draft: "+rd.settings.get("draftPath"));//TODO handle msg: false --- exit
					wr.flush();
					wr.close();
					System.exit(-1);
				}
				
				//System.out.println("In query agent try3");
				manager.setProgressFile(rd.settings.get("progressPlace"));
				//System.out.println("In query agent" + rd.settings.get("progressPlace"));
				manager.setOutputFile(rd.settings.get("outputPlace"));
				
				//System.out.println("In query agent try2");
				String str = rd.settings.get("candiFolder");
				String folders[] = str.split(";");
				System.out.println("In query agent try2.1"+folders);
				for(String path: folders){
					System.out.println("In query agent try2.1n: "+path.trim());
					msg = manager.loadFiles(path.trim());
					System.out.println("In query agent try2.n");
					if(!msg){
						wr.println("Error: failed to load Candi Folder: "+rd.settings.get("candiFolder"));
						wr.flush();
					}
				}
				
			
				try{
					//System.out.println("In query agent try try");
					//if(manager.getClass().getName().equals("imprSearch.ImprManager"))
					manager.searchMethod = Integer.parseInt(rd.settings.get("searchMethod"));
					manager.preprocessing = Integer.parseInt(rd.settings.get("preprocessing"));
					manager.MAX_AM_RATE = Double.parseDouble(rd.settings.get("maxAmRate"));
					manager.MAX_FOLDS = Integer.parseInt(rd.settings.get("maxFolds"));
					manager.CENTER_X = Double.parseDouble(rd.settings.get("centerX"));
					manager.CENTER_Y = Double.parseDouble(rd.settings.get("centerY"));
					manager.SEARCH_W = Double.parseDouble(rd.settings.get("searchW"));
					manager.SEARCH_H = Double.parseDouble(rd.settings.get("searchH"));
					manager.SLIDING_STEP = Integer.parseInt(rd.settings.get("slidingStep"));
					Impression.MAX_PATCH_SIZE = Integer.parseInt(rd.settings.get("maxPatchSize"));
					
					Thread comparing = new Thread(manager,"Search-Manager");
		    	    comparing.start();
		    	    
		    	    
		    	    wr.flush();
		    	    wr.close();
		    	    
				}catch(NumberFormatException e){
					e.printStackTrace();
					wr.println("Error: Illegal args for searching parameters");
					//close???
				}
			
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			/*
			String draftPath = args[0];
			String CandiFolder = args[1].trim();
			Manager manager = new ImprManager("text");
			boolean msg = manager.receiveDraft(draftPath);
			if(msg){
				System.out.println("-- draft loaded");
				msg = manager.loadFiles(CandiFolder);
				if(msg){
					System.out.println("-- candidates loaded");
					Thread comparing = new Thread(manager,"Search-Manager");
		    	    comparing.start();
				}else{
					System.out.println("-- failed to load candi folder");
					System.out.println("-- Search Aborted.");
				}
			}else{
				System.out.println("-- failed to load the draft");
				draftPath="";
				System.out.println("-- Search Aborted.");
			}*/
		}else{
			System.out.println(args.length + ": Invalid query.");
		}
	}

}

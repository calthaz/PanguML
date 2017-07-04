package general;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Scanner;

import imprSearch.ImprManager;

public class SearchByTextDriver {
	private static final String[] commands = {
			"NEW QUERY",
			"DRAFT PATH",//path expected after it always replace the original one
			"ADD CANDI FOLDER",//path expected after it
			"ADD CANDI FILE",//path expected after it
			"DELETE ALL CANDI",
			"DELETE CANDI",//path expected after it
			"DELETE DRAFT",
			"PRINT CANDI LIST",
			"PRINT DRAFT PATH",
			"PRINT PREFERENCE",
			"PRINT CANDI",//path expected after it
			"PRINT DRAFT",
			"BEGIN SEARCH",//invokes data check and conversation
			"CLEAR ALL",
	};
	
	private String draftPath;
	private static final ArrayList<String> CMD = new ArrayList<String>();
	
	private Scanner sc = new Scanner(System.in);
	private Manager manager;
	
	public SearchByTextDriver(){
		manager = new ImprManager("text");
		System.out.println("Welcome to search by text!");
		System.out.println("Type commands in capital letters and end command with a \">\"");
		CMD.add("NEW QUERY");//0
		CMD.add("DRAFT PATH");//1
		CMD.add("ADD CANDI FOLDER");//2
		CMD.add("ADD CANDI FILE");//3
		
		CMD.add("DELETE ALL CANDI");//4
		CMD.add("DELETE CANDI");//5
		CMD.add("DELETE DRAFT");//6
		
		CMD.add("PRINT CANDI LIST");//7
		CMD.add("PRINT DRAFT PATH");//8
		CMD.add("PRINT PREFERENCE");//9
		CMD.add("PRINT CANDI");//10
		CMD.add("PRINT DRAFT");//11
		
		CMD.add("BEGIN SEARCH");//12
		CMD.add("CLEAR ALL");//13
		
		CMD.add("SET CENTER X");//14
		CMD.add("SET CENTER Y");//15
		CMD.add("SET PREPROCESSING");//16
		/*
		 * DRAFT PATH> G:\Projects\Impression Search\sketches\berlin-dom.jpg
-- path is set
ADD CANDI FOLDER> G:\Projects\Impression Search\Testing Images\berlin dom.jpg
-- candidates loaded
[G:\Projects\Impression Search\Testing Images\berlin dom.jpg compared with 1berlin-dom.jpg-imp wins 12 color patches. Score:0.5493486973947895]
Total file count: 1. Total window count: 49. Comparing has taken 232ms.

DRAFT PATH> D:\zym\Java Programming\Imprs\sketches\berlin-dom.jpg
-- path is set
ADD CANDI FOLDER> D:\zym\Java Programming\Testing Images\berlin dom.jpg
-- candidates loaded
		 */
		
		while(true){
			String input = sc.nextLine().trim();
			if(input.indexOf(">")>0){
				int seperator = input.indexOf(">");
				String command = input.substring(0, seperator);
				if(CMD.contains(command)){
					parseCommand(CMD.indexOf(command), input.substring(seperator+1).trim());
				}else{
					System.out.println("-- invalid command.");
				}
			}else{
				System.out.println("-- syntax error. Commands must contain \'>\'");
			}
		}
	}

	private void parseCommand(int index, String para) {
		boolean msg=false;
		switch(index){
		case 0:
			draftPath="";
			System.out.println("-- at your service");
			break;
		case 1:
			draftPath=para;
			System.out.println("-- path is set");
			break;
		case 2:
		case 3:
			msg = manager.loadFiles(para);
			if(msg){
				System.out.println("-- candidates loaded");
			}else{
				System.out.println("-- failed to load");
			}
			break;
			 
		case 4:
			manager.clearCandiList();
			System.out.println("-- candi list cleared");
			break;
			
		case 5:
			System.out.println("-- not implemented");
			break;
		
		case 6:
			draftPath="";
			System.out.println("-- draftPath deleted");
			break;
			
		case 7:
			System.out.println("-- ");
			System.out.println(manager.candiList);
			break;
			
		case 8:
			System.out.println("-- "+draftPath);
			break;
			
		case 9:
			System.out.println("------"+Calendar.getInstance().getTime()+"------");
			System.out.println("-- "+manager.printSettings());
			break;
			
		case 10:
			System.out.println("-- not implemented");
			break;
			
		case 11:
			System.out.println("-- not implemented");
			break;
		
		
		case 12:
			msg = manager.receiveDraft(draftPath);
			if(msg){
				System.out.println("-- draft loaded");
			}else{
				System.out.println("-- failed to load the draft");
				draftPath="";
				System.out.println("-- still waiting");
				break;
			}
			if(manager.candiList.size()==0){
				System.out.println("-- warning: empty candi list");
				System.out.println("-- still waiting");
				break;
			}
			System.out.println("-- I'm all set, what about you? Type \"YES>\" to start searching...");
			String ans = sc.nextLine().trim();
			if(ans.equals("YES>")){
				System.out.println("Manager starts to work. Don't disturb him... ");
				Thread comparing = new Thread(manager,"Search-Manager");
	    	    comparing.start();
			}else{
				System.out.println("-- still waiting");
				break;
			}
			break;
			
		case 13:
			draftPath="";
			manager.clearCandiList();
			System.out.println("-- all cleared");
			break;
		
		case 14:
			msg = manager.setCenterX(Double.parseDouble(para));
			if(!msg){
				System.out.println("-- invalid argument. Settings are not changed.");
			}
			System.out.println("-- "+manager.printSettings());
			break;
		case 15:
			msg = manager.setCenterY(Double.parseDouble(para));
			if(!msg){
				System.out.println("-- invalid argument. Settings are not changed.");
			}
			System.out.println("-- "+manager.printSettings());
			break;
		case 16:
			((ImprManager) manager).setPreprocessing(Integer.parseInt(para));
			//if(!msg){
				//System.out.println("-- invalid argument. Settings are not changed.");
			//}
			System.out.println("-- "+manager.printSettings());
			break;
	}
	}
	
	public static void main(String[] args){
		new SearchByTextDriver();
	}
}

package general;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;


public class TextSettingsReader {
	
	public HashMap<String, String> settings = new HashMap<String, String>();
	private Scanner sc;
	
	public TextSettingsReader(String path) throws FileNotFoundException{
		//try {
			
			sc=new Scanner(new File(path));
			String candiFolder = "";
			
			while(sc.hasNextLine()){
				String str = sc.nextLine();
				str = str.trim();
				
				if(str.indexOf("candiFolder")==0){ //in case if someone is called "candiFolder" 
					candiFolder +=str.substring(str.indexOf(":")+1).trim()+";";
					settings.put("candiFolder", candiFolder);
				}else{
					settings.put(str.substring(0, str.indexOf(":")), str.substring(str.indexOf(":")+1).trim());
				}
			}
		//} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		//}
		
	}
}

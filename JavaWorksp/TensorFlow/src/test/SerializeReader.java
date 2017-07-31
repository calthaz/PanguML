package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SerializeReader {
	public static void main(String[] args) {
		String path = "C:/tmp/hardware/b-carpet/fq-872161500638465765fq-data.txt";
		try {
			FileInputStream in = new FileInputStream(new File(path));
			byte[] c = new byte[1024];
			List<Byte> a = new ArrayList<Byte>();
			int len = 0;
			while((len=in.read(c)) != -1){
				
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

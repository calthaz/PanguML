/*
 * Class NativeUtils is published under the The MIT License:
 *
 * Copyright (c) 2012 Adam Heinrich <adam@adamh.cz>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package zym.tensorflow.tools;

import java.io.*;
 
/**
 * A simple library class which helps with loading dynamic libraries stored in the
 * JAR archive. These libraries usualy contain implementation of some methods in
 * native code (using JNI - Java Native Interface).
 * @see http://adamheinrich.com/blog/2012/how-to-load-native-jni-library-from-jar
 * @see https://github.com/adamheinrich/native-utils
 * @see https://www.adamheinrich.com/blog/2012/12/how-to-load-native-jni-library-from-jar/#comment-1760518031
 */
public class NativeUtils {
 
    /**
     * Private constructor - this class will never be instanced
     */
    private NativeUtils() {
    }
 
    /**
     * <div class="en">
     * Loads library from current JAR archive
     * <br>
     * The file from JAR is copied into system temporary directory and then loaded. The temporary file is deleted after exiting.
     * Method uses String as filename because the pathname is "abstract", not system-dependent.
     * </div>
     * <div class ="zh">
     * 从当前JAR中加载库文件<br>
     * JAR中的文件被复制到系统临时目录中，然后加载。 退出后临时文件被删除。
     * 方法使用String作为文件名，因为路径名是“抽象的”，而不是依赖于系统的。
     * </div>
     * @see https://www.adamheinrich.com/blog/2012/12/how-to-load-native-jni-library-from-jar/#comment-1760518031
     * @param path 
     * <span class="zh">JAR内文件的路径为绝对路径（以'/'开始），例如/package/File.ext</span>
     * <span class="en">The path of file inside JAR as absolute path (beginning with '/'), e.g. /package/File.ext</span>
     * @throws IOException 
     * <span class="zh">如果临时文件创建或读/写操作失败</span>
     * <span class="en">If temporary file creation or read/write operation fails</span>
     * @throws IllegalArgumentException 
     * <span class="zh">如果源文件（path）不存在</span>
     * <span class="en">If source file (path) does not exist</span>
     * @throws IllegalArgumentException 
     * <span class="zh">如果路径不是绝对路径，或者文件名
     * 短于三个字符(限制{@see File＃createTempFile(java.lang.String，java.lang.String)})。</span>
     * <span class="en">If the path is not absolute or if the filename is 
     * shorter than three characters (restriction of {@see File#createTempFile(java.lang.String, java.lang.String)}).</span>
     */
    public static void loadLibraryFromJar(String path) throws IOException {
 
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("The path has to be absolute (start with '/').");
        }
 
        // Obtain filename from path
        String[] parts = path.split("/");
        String filename = (parts.length > 1) ? parts[parts.length - 1] : null;
 
        // Split filename to prexif and suffix (extension)
        String prefix = "";
        String suffix = null;
        if (filename != null) {
            parts = filename.split("\\.", 2);
            prefix = parts[0];
            suffix = (parts.length > 1) ? "."+parts[parts.length - 1] : null; // Thanks, davs! :-)
        }
 
        // Check if the filename is okay
        if (filename == null || prefix.length() < 3) {
            throw new IllegalArgumentException("The filename has to be at least 3 characters long.");
        }
 
        // Prepare temporary file
        File temp = File.createTempFile(prefix, suffix);
        temp.deleteOnExit();
 
        if (!temp.exists()) {
            throw new FileNotFoundException("File " + temp.getAbsolutePath() + " does not exist.");
        }
 
        // Prepare buffer for data copying
        byte[] buffer = new byte[1024];
        int readBytes;
 
        // Open and check input stream
        InputStream is = NativeUtils.class.getResourceAsStream(path);
        if (is == null) {
            throw new FileNotFoundException("File " + path + " was not found inside JAR.");
        }
 
        // Open output stream and copy data between source file in JAR and the temporary file
        OutputStream os = new FileOutputStream(temp);
        try {
            while ((readBytes = is.read(buffer)) != -1) {
                os.write(buffer, 0, readBytes);
            }
        } finally {
            // If read/write fails, close streams safely before throwing an exception
            os.close();
            is.close();
        }
        //System.out.println(temp.getAbsolutePath());
        // Finally, load the library
        System.load(temp.getAbsolutePath());
        
        //the following part comes from the comment link above 
        //-- deals with library files that can't be deleted on exit
        final String libraryPrefix = prefix;
        final String lockSuffix = ".lock";
        
        // create lock file
        final File lock = new File( temp.getAbsolutePath() + lockSuffix);
        lock.createNewFile();
        lock.deleteOnExit();
     
        // file filter for library file (without .lock files)
        FileFilter tmpDirFilter =
          new FileFilter()
          {
            public boolean accept(File pathname)
            {
              return pathname.getName().startsWith( libraryPrefix) && !pathname.getName().endsWith( lockSuffix);
            }
          };
          
        // get all library files from temp folder  
        String tmpDirName = System.getProperty("java.io.tmpdir");
        File tmpDir = new File(tmpDirName);
        File[] tmpFiles = tmpDir.listFiles(tmpDirFilter);
        
        // delete all files which don't have n accompanying lock file
        for (int i = 0; i < tmpFiles.length; i++)
        {
          // Create a file to represent the lock and test.
          File lockFile = new File( tmpFiles[i].getAbsolutePath() + lockSuffix);
          if (!lockFile.exists())
          {
            System.out.println( "deleting: " + tmpFiles[i].getAbsolutePath());
            tmpFiles[i].delete();
          }
        } 
        
    }
    
    /**
     * <div class="en">
     * Loads files from current JAR archive
     * <br>
     * The file from JAR is copied into system temporary directory. The temporary file is deleted after exiting.
     * Method uses String as filename because the pathname is "abstract", not system-dependent.
     * </div>
     * <div class ="zh">
     * 从当前JAR中加载文件<br>
     * JAR中的文件被复制到系统临时目录中。 退出后临时文件被删除。
     * 方法使用String作为文件名，因为路径名是“抽象的”，而不是依赖于系统的。
     * </div>
     * @param path 
     * <span class="zh">JAR内文件的路径为绝对路径（以'/'开始），例如/package/File.ext</span>
     * <span class="en">The path of file inside JAR as absolute path (beginning with '/'), e.g. /package/File.ext</span>
     * @throws IOException 
     * <span class="zh">如果临时文件创建或读/写操作失败</span>
     * <span class="en">If temporary file creation or read/write operation fails</span>
     * @throws IllegalArgumentException 
     * <span class="zh">如果源文件（path）不存在</span>
     * <span class="en">If source file (path) does not exist</span>
     * @throws IllegalArgumentException 
     * <span class="zh">如果路径不是绝对路径，或者文件名
     * 短于三个字符(限制{@see File＃createTempFile(java.lang.String，java.lang.String)})。</span>
     * <span class="en">If the path is not absolute or if the filename is 
     * shorter than three characters (restriction of {@see File#createTempFile(java.lang.String, java.lang.String)}).</span>
     */
    public static String extractFileFromJar(String path) throws IOException {
 
        if (!path.startsWith("/")) {
            throw new IllegalArgumentException("The path has to be absolute (start with '/').");
        }
 
        // Obtain filename from path
        String[] parts = path.split("/");
        String filename = (parts.length > 1) ? parts[parts.length - 1] : null;
 
        // Split filename to prexif and suffix (extension)
        String prefix = "";
        String suffix = null;
        if (filename != null) {
            parts = filename.split("\\.", 2);
            prefix = parts[0];
            suffix = (parts.length > 1) ? "."+parts[parts.length - 1] : null; // Thanks, davs! :-)
        }
 
        // Check if the filename is okay
        if (filename == null || prefix.length() < 3) {
            throw new IllegalArgumentException("The filename has to be at least 3 characters long.");
        }
 
        // Prepare temporary file
        File temp = File.createTempFile(prefix, suffix);
        temp.deleteOnExit();
 
        if (!temp.exists()) {
            throw new FileNotFoundException("File " + temp.getAbsolutePath() + " does not exist.");
        }
 
        // Prepare buffer for data copying
        byte[] buffer = new byte[1024];
        int readBytes;
 
        // Open and check input stream
        InputStream is = NativeUtils.class.getResourceAsStream(path);
        if (is == null) {
            throw new FileNotFoundException("File " + path + " was not found inside JAR.");
        }
 
        // Open output stream and copy data between source file in JAR and the temporary file
        OutputStream os = new FileOutputStream(temp);
        try {
            while ((readBytes = is.read(buffer)) != -1) {
                os.write(buffer, 0, readBytes);
            }
        } finally {
            // If read/write fails, close streams safely before throwing an exception
            os.close();
            is.close();
        }
        
        // Finally, return the path
        return temp.getAbsolutePath();
        
    }
    /**
     * <div class="en">load the file from system if {@code loadPath} exists, else extract file from JAR using {@code extractPath}</div>
     * <div class="zh">如果存在{@code loadPath}，则从系统加载文件，否则使用{@code extractPath}从JAR提取文件</div>
     * 
     * @param loadPath
     * @param extractPath
     * @return path 
     * <span class="zh">指向所需文件的路径，如果加载失败，则返回null</span>
     * <span class="en">path to the desired file, null if failed to load</span>
     */
    public static String loadOrExtract(String loadPath, String extractPath){
    	File f = new File(loadPath);
		if(!f.exists()){
			try {
				loadPath = NativeUtils.extractFileFromJar(extractPath);
				return loadPath;
			} catch (IOException e) {
				System.err.println("Failed to load model "+extractPath);
				e.printStackTrace();
				return null;
			}
		}else{
			return loadPath;
		}
    }
}

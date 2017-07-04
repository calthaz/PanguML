package general;

import java.awt.image.BufferedImage;

public interface Query {
	BufferedImage getDraft();
	BufferedImage display();
	int getWidth();
	int getHeight();
	String getID();
	String toString();
}

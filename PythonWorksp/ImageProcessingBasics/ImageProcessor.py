from __future__ import print_function
import os
import numpy as np
from PIL import Image


class ImageProcessor:
	'''basic image processing'''
	def getPixelArray(file, maxSize=None):
		im = Image.open(file)
		print('open image', im.format, im.size, im.mode)
		if(maxSize): #???
			#self.ResizeFit(im.size, maxSize) don't want to write it again now
			im.thumbnail(maxSize)
			l = list(im.getdata())

			return np.asarray(l)

	def saveThumbnail(infile, size=(128, 128)):
		
	    outfile = os.path.splitext(infile)[0] + ".thumbnail"
	    if infile != outfile: #what does it mean? 
	        try:
	            im = Image.open(infile)
	            im.thumbnail(size)
	            im.save(outfile, "JPEG")
	        except IOError:
	            print("cannot create thumbnail for", infile)



def main():
	arr = ImageProcessor.getPixelArray("img\cat.jpg", (200,100))
	print(len(arr))
	im2 = Image.fromarray(arr)
	#try:
	im2.save("img\smallercat", "JPEG")
	#except IOError:
		#print(IOError,"cannot create thumbnail")


	#like this: RGB I think 
	'''
	 (195, 170, 130), (203, 180, 139), (212, 189, 148), (219, 197, 158), (224, 202, 163), (231, 214, 204), (238, 224, 213), 
	 (246, 234, 222), (245, 237, 224), (244, 241, 226), (247, 245, 230), (246, 249, 232), (244, 247, 230), (246, 249, 232), 
	 (246, 249, 232), (249, 247, 232), (250, 247, 232), (253, 245, 232), (255, 244, 232), (255, 243, 232), (255, 242, 232), 
	 continuous, flattened
	'''

if __name__ == '__main__': 
	main()
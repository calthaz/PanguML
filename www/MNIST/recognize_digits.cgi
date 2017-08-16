#!"C:\ProgramData\Anaconda3\envs\python2\python.exe"
import numpy as np
import os
import cv2
import json
import cgi
import cgitb; cgitb.enable()

MAX_IMG_SIZE = 500
MIN_IMG_SIZE = 300
BOX_PAD = 7;

#print(cv2.__version__)

#https://pythontips.com/2015/03/11/a-guide-to-finding-books-in-images-using-python-and-opencv/
def scale_uniform_fill(img, destiW,destiH):
	origH, origW, _ = img.shape
	ratio = float(origW)/origH;
	#print(ratio)
	#d = {height:0, width:0};
	if float(destiW)/destiH<ratio:
		height=destiH; 
		width=int(destiH*ratio);
	else:
		width=destiW;
		height=int(float(destiW)/ratio);
		#print(float(destiW)/destiH)
	
	return cv2.resize(img, (width,height))

def fill_square(img):
	height, width, _ = img.shape
	roi_x = 0;
	roi_y = 0;
	if ( width >= height ):
		roi_y = ( width - height ) / 2; 
	else:
		roi_x = ( height - width ) / 2;
	
	img= cv2.copyMakeBorder(img,roi_y+3,roi_y+3,roi_x+3,roi_x+3,cv2.BORDER_REPLICATE)

	return img;


def scale_uniform_fit(img, destiW,destiH):
	origH, origW, _ = img.shape
	ratio = float(origW)/origH;

	if float(destiW)/destiH<ratio:
		width=destiW;
		height=int(float(destiW)/ratio)
	else:
		height=destiH
		width=int (destiH*ratio)
	
	return tf.image.resize_images(orig_img, [height, width])

def crop(path):
	#print (cv2.__version__)
	img = cv2.imread(path)
	if img is not None:
		#print(img.shape)
		height, width, _ = img.shape
		if height<MIN_IMG_SIZE or width<MIN_IMG_SIZE:
			img = scale_uniform_fill(img, MIN_IMG_SIZE, MIN_IMG_SIZE)
		kernel = np.array([[0,-0.5,0], [-0.5,3,-0.5], [0,-0.5,0]])
		img = cv2.filter2D(img, -1, kernel)
		height, width, _ = img.shape

		canny_img = cv2.Canny(img, 10, 250)
		kernel = np.ones((3,3),np.uint8)
		dilate_img = cv2.dilate(canny_img, kernel)
		cv2.imwrite('img/test-dialate.png', dilate_img)
		kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5))
		closed = cv2.morphologyEx(dilate_img, cv2.MORPH_CLOSE, kernel)
		cv2.imwrite('img/test-closed.png', closed)
		_, contours, hierarchy = cv2.findContours(closed.copy(), cv2.RETR_EXTERNAL,cv2.CHAIN_APPROX_SIMPLE)
		img_after = cv2.drawContours(img.copy(), contours, -1, (0,0,255), 3)#img == img_after

		img_dirty = img.copy()
		# loop over the contours
		total = 0
		cropped_list = []
		dir_name = os.path.dirname(path)
		name = os.path.basename(path)
		for c in contours:
			# approximate the contour
			peri = cv2.arcLength(c, True)
			approx = cv2.approxPolyDP(c, 0.02 * peri, True)
			
			rect = cv2.minAreaRect(c)
			box = cv2.boxPoints(rect)
			box = np.int0(box)
			
			x,y,w,h = cv2.boundingRect(c)
			#cv2.rectangle(img,(x,y),(x+w,y+h),(0,255,0),2)
			s_box = np.array([[  x, y],[  x+w,   y],[x+w,   y+h],[x, y+h]])
			if (cv2.contourArea(c)>width*height/3500):
				#print box
				clip=[max(0, x-BOX_PAD), min(x+w+BOX_PAD, width), max(0, y-BOX_PAD), min(y+h+BOX_PAD, height)]
				cv2.drawContours(img_dirty, [s_box], -1, (255, 180, 255*(total%10)/10), 2)
				total += 1
				crop_img = img[clip[2]:clip[3], clip[0]:clip[1]]
				crop_img = fill_square(crop_img);
				crop_name = dir_name+"/crop-"+str(total)+name
				cv2.imwrite(crop_name, crop_img)
				info = (crop_name, clip)
				cropped_list.append(info)
		'''
		cv2.imshow('box.png', img_dirty)
		cv2.imwrite("img/digits/result-"+str(total)+name, img_dirty)
		cv2.waitKey(0)
		cv2.destroyAllWindows()
		'''
		ret = {"size":[height, width], "clips":cropped_list}
		print(json.dumps(ret))
	else:
		print("Can't open image!")

if __name__ == "__main__":
	form = cgi.FieldStorage()
	path = form.getvalue("requestPath")
	mystatus = "200 OK"

	print("Status: %s\n" % mystatus)
	#print("Content-Type: text/html; charset=UTF-8")
	#print("\n\n")
	#These lines above will be printed out
	crop(path)
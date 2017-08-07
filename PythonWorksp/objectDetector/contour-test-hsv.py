import numpy as np
import cv2

MAX_IMG_SIZE = 500
MIN_IMG_SIZE = 300
BOX_PAD = 7;
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

#print (cv2.__version__)
img = cv2.imread('img/baidu.png')
#print(img.shape)
height, width, _ = img.shape
if height>MAX_IMG_SIZE or width>MAX_IMG_SIZE:
	img = scale_uniform_fill(img, MAX_IMG_SIZE, MAX_IMG_SIZE)
if height<MIN_IMG_SIZE or width<MIN_IMG_SIZE:
	img = scale_uniform_fill(img, MIN_IMG_SIZE, MIN_IMG_SIZE)
#cv2.imshow("original", img)
kernel = np.array([[0,-0.5,0], [-0.5,3,-0.5], [0,-0.5,0]])
img = cv2.filter2D(img, -1, kernel)
#cv2.imshow("sharpened", img)
img= cv2.copyMakeBorder(img,10,10,10,10,cv2.BORDER_CONSTANT,value=(0,255,0))
hsv = cv2.cvtColor(img,cv2.COLOR_BGR2HSV)
height, width, _ = hsv.shape
'''
h, s, v = cv2.split(hsv)
shifted_h = h.copy()
#do it later since it doesn't make much sense
shift = 25 # in openCV hue values go from 0 to 180 (so have to be doubled to get to 0 .. 360) because of byte range from 0 to 255
for j in range(height):
	for i in range(width):
		shifted_h[j][i] = (shifted_h[j][i]+shift)%180

canny_h = cv2.Canny(shifted_h, 30, 20)
print(canny_h.shape)
#cv2.imshow("canny_h_raw_larger", canny_h)
cv2.imwrite('img/test-canny_h_raw.png', canny_h)


canny_s = cv2.Canny(s, 200, 100)
#cv2.imshow("canny_s_raw", canny_s)
cv2.imwrite('img/test-canny_s_raw.png', canny_s)

merged = np.ndarray((height,width));
for j in range(height):
	for i in range(width):
		if canny_h[j][i]!=0 or canny_s[j][i]!=0:
			merged[j][i] = 255
		else:
			merged[j][i] = 0

#canny_v = cv2.Canny(v, 100, 200)
cv2.imshow("canny_merged", merged)
cv2.imwrite('img/test-canny_merged.png', merged)
'''
canny_img = cv2.Canny(img, 10, 250)
kernel = np.ones((3,3),np.uint8)
#opening = cv2.morphologyEx(canny_img,cv2.MORPH_OPEN,kernel, iterations = 2)
dilate_img = cv2.dilate(canny_img, kernel)
cv2.imwrite('img/test-sharpened-canny-dialate-closed-10-250.png', dilate_img)
kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (5, 5))
closed = cv2.morphologyEx(dilate_img, cv2.MORPH_CLOSE, kernel)
cv2.imwrite('img/test-sharpened-padded-canny-dilated-closed-10-250.png', closed)

canny_img_dirty = closed.copy()#cv2.RETR_EXTERNAL
_, contours, hierarchy = cv2.findContours(canny_img_dirty,cv2.RETR_TREE,cv2.CHAIN_APPROX_SIMPLE)
canny_img_dirty = img.copy()
img_after = cv2.drawContours(canny_img_dirty, contours, -1, (0,0,255), 3)#img == img_after
#cv2.imwrite('img/test-padded-canny-dilated-closed-10-250-cont.png',img_after)
for x in range(len(contours)):
	if(cv2.contourArea(contours[x])>width*height/1000):
		cv2.drawContours(canny_img_dirty, contours, x, (0,255,255*(x%10)/10), 3)
cv2.imwrite('img/test-sharpened-padded-canny-dilated-closed-10-250-cont-big.png', canny_img_dirty)

img_dirty = img.copy()
# loop over the contours
total = 0; 
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

	#box = cv2.convexHull(c)
	# if the approximated contour has four points, then assume that the
	# contour is a book -- a book is a rectangle and thus has four vertices
	if len(approx) < 100 and cv2.contourArea(box)>width*height/300:
		#print box
		cv2.drawContours(img_dirty, [box], -1, (255, 180, 255*(total%10)/10), 2)
		total += 1
		crop_img = img[max(0, y-BOX_PAD):min(y+h+BOX_PAD, height), 
		max(0, x-BOX_PAD):min(x+w+BOX_PAD, width)]
		cv2.imwrite('img/crop'+str(total)+'.png', crop_img)

cv2.imshow('box.png', img_dirty)
cv2.imwrite('img/test-sharpened-padded-canny-dilated-closed-10-250-cont-100-vertices-300big-straight-box.png', img_dirty)
cv2.waitKey(0)
cv2.destroyAllWindows()
'''
cv2.imwrite('img/test-grey.png', imgray)



ret,thresh = cv2.threshold(imgray,127,255,0)
cv2.imwrite('img/test-thresh.png', thresh)
img, contours, hierarchy = cv2.findContours(thresh,cv2.RETR_TREE,cv2.CHAIN_APPROX_SIMPLE)#image, 
cv2.imwrite('img/test-thresh2.png', thresh)
print (type(contours))  
print (type(contours[0]))  
print (len(contours))  
print (len(contours[0]))  
print (len(contours[1]))  
cnt = contours[4]
img = cv2.drawContours(im, contours, -1, (0,255,0), 3)
print (img) 

#img = cv2.drawContours(im, contours, -1, (0,255,0), 3)
cv2.imwrite('img/test-cont.png', img)
'''
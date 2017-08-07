import numpy as np
import cv2

print (cv2.__version__)
im = cv2.imread('img/test.png')
imgray = cv2.cvtColor(im,cv2.COLOR_BGR2GRAY)
cv2.imwrite('img/test-grey.png', imgray)

imgray_c = cv2.Canny(imgray, 100, 200)

cv2.imwrite('img/test-canny.png', imgray_c)

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
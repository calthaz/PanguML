
import os
import numpy as np
from PIL import Image

def get_main_colors(col_list):
	main_colors = set()
	for index, color in col_list:
		main_colors.add(tuple(component >> 6 for component in color))
	return [tuple(component << 6 for component in color) for color in main_colors]

def main():
	img = Image.open("img\cat.jpg")

	im_rgb = img.convert('RGB')
	colors = im_rgb.getcolors()
	print(colors)

if __name__ == '__main__': 
	main()

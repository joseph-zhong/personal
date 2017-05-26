
# coding: utf-8

# In[1]:

"""

GazeFollow-Caffe
---

Model and weights taken from http://gazefollow.csail.mit.edu 
    Where are they looking?
    A. Recasens*, A. Khosla*, C. Vondrick and A. Torralba
    Advances in Neural Information Processing Systems (NIPS), 2015

Architecture Summary
    The goal of GazeFollow is discover head pose and gaze orientation 
    to identify the object(s) the subject(s) are looking at in an 
    image. 
    
    The network takes a total of three inputs.
        1. Image
        2. Image of Head
        3. Location of Head
    
    The image is fed through a network to determine eye fixations for
    any arbitrary observer of the image, producing a Saliency Map. The
    image of the head is fed through a network which learns gaze 
    orientation. The output of the second network is concatenated with
    the location of head, and fed through a series of fully connected
    layers to finally produce a Gaze Mask across the full image.
    The Gaze Mask and Saliency Mask undergo an element-wise product and
    are fed into another number of fully connected layers, into 
    "Shifted Grids" to finally produce the gaze prediction.

Notes
    The image of the head does not necessarily need to include a face, 
    but best results are produced when the image is centered and cropped, 
    with an accurate location.

"""

import matplotlib.pyplot as plt
import numpy as np
from PIL import Image

get_ipython().magic(u'matplotlib inline')


# In[2]:

# Extract imthe head using OpenCV and Haars Cascades.
import cv2

# Load upper body cascade.
upperBodyCascade = cv2.CascadeClassifier('/media/josephz/Data/cascades/haarcascade_mcs_upperbody.xml')

# Read image in RGB.
img = cv2.imread('toddler.jpg')
imgCopy = img.copy()
gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)

imgHeight = img.shape[0]
imgWidth = img.shape[1]

detections = []
rois = upperBodyCascade.detectMultiScale(gray, 1.05, 35)

# Store the detected heads and their pixel-coordinate location.
for (x,y,w,h) in rois:
    cv2.rectangle(imgCopy,(x,y),(x+w,y+h),(0,0,255),2)
    detections.append((img[y:y+h, x:x+w], 
                       ((x + float(w)/2)/imgWidth, (y + float(h)/2)/imgHeight)))

plt.imshow(imgCopy[...,::-1])
print "Detected a total of '{}' rectangles".format(len(detections))


# In[3]:

# Select one of the heads to compute Gaze.
headImg, headLoc = detections[0]
plt.imshow(headImg[...,::-1])
print "Detected at location '{}' by dimension percents.".format(headLoc)


# In[4]:

# Setup GazeFollow Model.
import caffe

caffe.set_mode_gpu()
model_def = 'deploy_demo.prototxt'
model_weights = 'binary_w.caffemodel'

# Load model in Test Mode.
gazeFollowNet = caffe.Net(model_def, model_weights, caffe.TEST)


# In[5]:

# Setup the inputs for inference.
fullImgMean = img.mean(1).mean(1) 
headImgMean = headImg.mean(1).mean(1)
print "Full Img Mean-subtracted values: {}".format(zip('BGR', fullImgMean))
print "Head Img Mean-subtracted values: {}".format(zip('BGR', headImgMean))


# In[6]:

# Create transformers for the model inputs.
dataTransformer = caffe.io.Transformer({'data': 
                                        gazeFollowNet.blobs['data'].data.shape})
faceTransformer = caffe.io.Transformer({'face': 
                                        gazeFollowNet.blobs['face'].data.shape})
eyesTransformer = caffe.io.Transformer({'eyes_grid': 
                                        gazeFollowNet.blobs['eyes_grid'].data.shape})

# # Move image channels to outermost dimension.
# dataTransformer.set_transpose('data', (2,0,1))  
# faceTransformer.set_transpose('face', (2,0,1))  

# # Subtract the dataset-mean value in each channel.
# dataTransformer.set_mean('data', fullImgMean)   
# faceTransformer.set_mean('face', headImgMean)   

# # Rescale from [0, 1] to [0, 255].
# dataTransform.set_raw_scale('data', 255)
# faceTransform.set_raw_scale('face', 255)

# # Swap channels from RGB to BGR.
# dataTransform.set_channel_swap('data', (2,1,0))
# faceTransform.set_channel_swap('face', (2,1,0))

# Reshape inputs.
transformedFullImg = dataTransformer.preprocess('data', img)
transformedFullImg = np.reshape(transformedFullImg,  (1,3,227,227))

transformedFaceImg = faceTransformer.preprocess('face', headImg)
transformedFaceImg = np.reshape(transformedFaceImg,  (1,3,227,227))

# Add face location to input.
f = np.zeros((1,1,169))
z = np.zeros((13,13))
x = int(headLoc[0]*13) + 1
y = int(headLoc[1]*13) + 1
z[x][y] = 1
z = np.ndarray.flatten(z)
z = np.reshape(z, (1, 169, 1, 1))

print "Input Shapes"
print transformedFullImg.shape
print transformedFaceImg.shape
print z.shape
print

# Set input values.
gazeFollowNet.blobs['data'].data[...] = transformedFullImg
gazeFollowNet.blobs['face'].data[...] = transformedFaceImg
gazeFollowNet.blobs['eyes_grid'].data[...] = z

# Perform inference.
fVals = gazeFollowNet.forward()

for fVal in fVals:
    print "fVal: '{}' of shape: '{}'".format(fVal, len(fVals[fVal][0]))
    print fVals[fVal][0]
# In[32]:

# Parse fully connected output.
alpha = 0.3
hm = np.zeros((15,15))
count_hm = np.zeros((15,15))

f_0_0 = np.reshape(fVals['fc_0_0'], (5, 5))
f_0_0 = np.exp(alpha*f_0_0)/sum(np.exp(alpha*f_0_0))

f_1_0 = np.reshape(fVals['fc_1_0'],(5, 5))
f_1_0 = np.exp(alpha*f_1_0)/sum(np.exp(alpha*f_1_0))

f_m1_0 = np.reshape(fVals['fc_m1_0'],(5, 5))
f_m1_0 = np.exp(alpha*f_m1_0)/sum(np.exp(alpha*f_m1_0))

f_0_m1 = np.reshape(fVals['fc_0_m1'],(5, 5))
f_0_m1 = np.exp(alpha*f_0_m1)/sum(np.exp(alpha*f_0_m1))

f_0_1 = np.reshape(fVals['fc_0_1'],(5, 5))
f_0_1 = np.exp(alpha*f_0_1)/sum(np.exp(alpha*f_0_1))

print f_0_0
print f_1_0
print f_m1_0
print f_0_m1
print f_0_1


# In[35]:

# TODO: Convert from Matlab.
# 
# f_cell = [f_0_0,f_1_0,f_m1_0,f_0_m1,f_0_1]
# v_x = [0, 1, -1, 0, 0]
# v_y = [0, 0, 0, -1, 1]

# for k in range(5):
#     delta_x = v_x[k]
#     delta_y = v_y[k]
#     f = f_cell[k]
#     for x in range(5):
#         for y in range(5):
#             i_x = 1+3*(x-1) - delta_x
#             i_x = max(i_x, 1)
            
#             if (x == 1):
#                 i_x = 1
            
#             i_y = 1+3*(y-1) - delta_y; i_y = max(i_y,1);
#             if (y==1):
#                 i_y = 1

#             f_x = 3*x-delta_x
#             f_x = min(15,f_x)
            
#             if (x==5):
#                 f_x = 15;

#             f_y = 3*y-delta_y
#             f_y = min(15,f_y)
#             if(y==5):
#                 f_y = 15

#             hm[i_x:f_x, i_y:f_y] = hm[i_x:f_x,i_y:f_y] + f(x,y)
#             count_hm[i_x:f_x, i_y:f_y] = count_hm[i_x:f_x,i_y:f_y] + 1

# hm_base = float(hm) / count_hm
# hm_results = cv2.imresize(hm_base, (size(img,1), size(img,2)), 'INTER_CUBIC')

# maxval,idx = max(hm_results)

# row, col = ind2sub(size(hm_results), idx)
# y_predict = row/size(hm_results,1)
# x_predict = col/size(hm_results,2)

# print y_predict
# print x_predict


# In[ ]:




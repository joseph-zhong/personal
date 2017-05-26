 % Demo script to use the gaze following model
 % Written by Adria Recasens (recasens@mit.edu)
 
 addpath(genpath('/data/vision/torralba/datasetbias/caffe-cudnn3/matlab/'));
 
 im = imread('test.jpg');
 
 %Plug here your favorite head detector
 e = [0.6  0.2679];

 % Compute Gaze 
 [x_predict,y_predict,heatmap,net] = predict_gaze(im,e);
 
 %Visualization
 g = floor([x_predict y_predict].*[size(im,2) size(im,1)]);
 e = floor(e.*[size(im,2) size(im,1)]);
 line = [e(1) e(2) g(1) g(2)];
 im = insertShape(im,'line',line,'Color','red','LineWidth',8);    
 
 image(im);
 
 
 
 

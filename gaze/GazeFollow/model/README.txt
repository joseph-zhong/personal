GazeFollow Source Code
http://gazefollow.csail.mit.edu

Installation:
The GazeFollow code uses Matcaffe for feature extraction. Before running the code, you have to install Caffe (https://github.com/BVLC/caffe), and compile Matcaffe. The predict_gaze function assumes the library has been added to the Matlab path.

Demo:
Sample code can be found in the script demo.m. This code requires knowing the location of the head. A head detector will be released in the near future to enable a fully-automatic pipeline.

predict_gaze function:
The predict_gaze function takes as input the full image and the position of the head, producing as output the most likely gaze position and the prediction heatmap. The function runs the input data through the network and computes the output from the five shifted grids.

Contact:
Please contact Adria Recasens (recasens@csail.mit.edu) in case you have any questions or comments.

Reference:
Please cite our paper if you find this code useful for your research:
@inproceedings{nips15_recasens,
 author = "Adria Recasens$^*$ and Aditya Khosla$^*$ and Carl Vondrick and Antonio Torralba",
 title = "Where are they looking?",
 booktitle = "Advances in Neural Information Processing Systems (NIPS)",
 year = "2015",
 note = "$^*$ indicates equal contribution"
}



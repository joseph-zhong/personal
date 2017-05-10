# Tensorflow with GPU Support

## For OSX 10.12.x and Cuda 8.0

### Summary

- Install CUDNN
- Install CUDA Driver
- Build TensorFlow
- Valiation of Installation
- Common Issues

### DNN 5.0 for Cuda 8.0

https://developer.nvidia.com/rdp/cudnn-download

Don't forget to unpack it and copy its files into `/usr/local/cuda`

``` 
sudo mv -v /path/to/cuda/lib/libcudnn* /usr/local/cuda/lib
sudo mv -v /path/to/cuda/include/cudnn.h /usr/local/cuda/include
sudo ln -s /usr/local/cuda/lib/libcudnn.5.dylib /usr/local/cuda/lib/libcudnn.5
```

**Important:** Add the following to your `.bash_profile` 
```
export CUDA_HOME=/usr/local/cuda
export DYLD_LIBRARY_PATH=`/usr/local/cuda/lib`:$DYLD_LIBRARY_PATH
export LD_LIBRARY_PATH=$DYLD_LIBRARY_PATH
export PATH=$DYLD_LIBRARY_PATH:$PATH
export PATH=/usr/local/cuda/bin:$PATH
```

The dynamically linking of `libcudnn.5.dylib` to `libcudnn.5` may or may not be necessary

### Verification

At this point we should test `nvcc` and the CUDA compiler

First install [gfxCardStatus](https://gfx.io/) disable dynamic graphics switching

If XCode is not installed yet, install it

For more see https://developer.apple.com/legacy/library/documentation/Darwin/Reference/ManPages/man1/xcode-select.1.html
```
sudo xcode-select -s /Applications/Xcode.app/
```

Verify that the CUDA Driver is installed correctly
```
kextstat | grep -i cuda
```

```
cd /usr/local/cuda/samples
sudo make -C 1_Utilities/deviceQuery
./bin/x86_64/darwin/release/deviceQuery
```

Verify that the output looks something like the following

```
 CUDA Device Query (Runtime API) version (CUDART static linking)

Detected 1 CUDA Capable device(s)

Device 0: "GeForce GT 750M"
  CUDA Driver Version / Runtime Version          8.0 / 8.0
  CUDA Capability Major/Minor version number:    3.0
  Total amount of global memory:                 2048 MBytes (2147024896 bytes)
  ( 2) Multiprocessors, (192) CUDA Cores/MP:     384 CUDA Cores
  GPU Max Clock rate:                            926 MHz (0.93 GHz)
  Memory Clock rate:                             2508 Mhz
  Memory Bus Width:                              128-bit
  L2 Cache Size:                                 262144 bytes
  Maximum Texture Dimension Size (x,y,z)         1D=(65536), 2D=(65536, 65536), 3D=(4096, 4096, 4096)
  Maximum Layered 1D Texture Size, (num) layers  1D=(16384), 2048 layers
  Maximum Layered 2D Texture Size, (num) layers  2D=(16384, 16384), 2048 layers
  Total amount of constant memory:               65536 bytes
  Total amount of shared memory per block:       49152 bytes
  Total number of registers available per block: 65536
  Warp size:                                     32
  Maximum number of threads per multiprocessor:  2048
  Maximum number of threads per block:           1024
  Max dimension size of a thread block (x,y,z): (1024, 1024, 64)
  Max dimension size of a grid size    (x,y,z): (2147483647, 65535, 65535)
  Maximum memory pitch:                          2147483647 bytes
  Texture alignment:                             512 bytes
  Concurrent copy and kernel execution:          Yes with 1 copy engine(s)
  Run time limit on kernels:                     Yes
  Integrated GPU sharing Host Memory:            No
  Support host page-locked memory mapping:       Yes
  Alignment requirement for Surfaces:            Yes
  Device has ECC support:                        Disabled
  Device supports Unified Addressing (UVA):      Yes
  Device PCI Domain ID / Bus ID / location ID:   0 / 1 / 0
  Compute Mode:
     < Default (multiple host threads can use ::cudaSetDevice() with device simultaneously) >

deviceQuery, CUDA Driver = CUDART, CUDA Driver Version = 8.0, CUDA Runtime Version = 8.0, NumDevs = 1, Device0 = GeForce GT 750M
Result = PASS
```

### Building Tensorflow

See https://www.tensorflow.org/install/install_mac#installing_with_virtualenv

### Validation of Installation

See https://www.tensorflow.org/install/install_mac#ValidateYourInstallation

As a quick sanity check you can run the following

```
python -c "import tensorflow;"
```

#### Simple Sample

```
import tensorflow as tf
node1 = tf.constant(3.0, tf.float32)
node2 = tf.constant(4.0) # also tf.float32 implicitly
print(node1, node2)

sess = tf.Session()
print(sess.run([node1, node2]))
```


### Common Issues

There apparently are issues with SIP and specific version of Bazel

- [Import Error Library not loaded: @rpath/libcudart.8.0.dylib](https://github.com/tensorflow/tensorflow/issues/5141)
- [bazel build: file not found: -lcublas.8.0](https://github.com/tensorflow/tensorflow/issues/7633)

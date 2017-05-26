#include <stdio.h>
#include "cutil.h"
#include "Mandelbrot_kernel.h"

// The dimensions of the thread block
#define BLOCKDIM_X 8
#define BLOCKDIM_Y 8

#define ABS(n) ((n) < 0 ? -(n) : (n))

// This function sets the DS number A equal to the double precision floating point number B. 
inline void dsdeq(float &a0, float &a1, double b)
{
    a0 = (float)b;
    a1 = (float)(b - a0);
} // dsdcp

// This function sets the DS number A equal to the single precision floating point number B. 
__device__ inline void dsfeq(float &a0, float &a1, float b)
{
    a0 = b;
    a1 = 0.0f;
} // dsfeq

// This function computes c = a + b.
__device__ inline void dsadd(float &c0, float &c1, const float a0, const float a1, const float b0, const float b1)
{
    // Compute dsa + dsb using Knuth's trick.
    float t1 = a0 + b0;
    float e = t1 - a0;
    float t2 = ((b0 - e) + (a0 - (t1 - e))) + a1 + b1;

    // The result is t1 + t2, after normalization.
    c0 = e = t1 + t2;
    c1 = t2 - (e - t1);
} // dsadd

// This function computes c = a - b.
__device__ inline void dssub(float &c0, float &c1, const float a0, const float a1, const float b0, const float b1)
{
    // Compute dsa - dsb using Knuth's trick.
    float t1 = a0 - b0;
    float e = t1 - a0;
    float t2 = ((-b0 - e) + (a0 - (t1 - e))) + a1 - b1;

    // The result is t1 + t2, after normalization.
    c0 = e = t1 + t2;
    c1 = t2 - (e - t1);
} // dssub

// This function multiplies DS numbers A and B to yield the DS product C.
__device__ inline void dsmul(float &c0, float &c1, const float a0, const float a1, const float b0, const float b1)
{
	// This splits dsa(1) and dsb(1) into high-order and low-order words.
	float cona = a0 * 8193.0f;
	float conb = b0 * 8193.0f;
	float sa1 = cona - (cona - a0);
	float sb1 = conb - (conb - b0);
	float sa2 = a0 - sa1;
	float sb2 = b0 - sb1;

	// Multilply a0 * b0 using Dekker's method.
	float c11 = a0 * b0;
	float c21 = (((sa1 * sb1 - c11) + sa1 * sb2) + sa2 * sb1) + sa2 * sb2;

    // Compute a0 * b1 + a1 * b0 (only high-order word is needed).
    float c2 = a0 * b1 + a1 * b0;

    // Compute (c11, c21) + c2 using Knuth's trick, also adding low-order product.
    float t1 = c11 + c2;
    float e = t1 - c11;
    float t2 = ((c2 - e) + (c11 - (t1 - e))) + c21 + a1 * b1;

    // The result is t1 + t2, after normalization.
    c0 = e = t1 + t2;
    c1 = t2 - (e - t1);
} // dsmul

// The core Mandelbrot CUDA GPU calculation function
#if 1
// Unrolled version
__device__ inline int CalcMandelbrot(const float xPos, const float yPos, const int crunch)
{
    float y = yPos;
    float x = xPos;
    float yy = y * y;
    float xx = x * x;
    int i = crunch;

    do {
		// Iteration 1
		if (xx + yy > 4.0f)
			return i - 1;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;

		// Iteration 2
		if (xx + yy > 4.0f)
			return i - 2;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;

		// Iteration 3
		if (xx + yy > 4.0f)
			return i - 3;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;

		// Iteration 4
		if (xx + yy > 4.0f)
			return i - 4;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;

		// Iteration 5
		if (xx + yy > 4.0f)
			return i - 5;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;

		// Iteration 6
		if (xx + yy > 4.0f)
			return i - 6;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;

		// Iteration 7
		if (xx + yy > 4.0f)
			return i - 7;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;

		// Iteration 8
		if (xx + yy > 4.0f)
			return i - 8;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;

		// Iteration 9
		if (xx + yy > 4.0f)
			return i - 9;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;

		// Iteration 10
		if (xx + yy > 4.0f)
			return i - 10;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;       

		// Iteration 11
		if (xx + yy > 4.0f)
			return i - 11;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;       

		// Iteration 12
		if (xx + yy > 4.0f)
			return i - 12;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;       

		// Iteration 13
		if (xx + yy > 4.0f)
			return i - 13;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;       

		// Iteration 14
		if (xx + yy > 4.0f)
			return i - 14;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;       

		// Iteration 15
		if (xx + yy > 4.0f)
			return i - 15;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;       

		// Iteration 16
		if (xx + yy > 4.0f)
			return i - 16;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;       

		// Iteration 17
		if (xx + yy > 4.0f)
			return i - 17;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;       

		// Iteration 18
		if (xx + yy > 4.0f)
			return i - 18;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;       

		// Iteration 19
		if (xx + yy > 4.0f)
			return i - 19;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;       

		// Iteration 20
        i -= 20;
		if ((i <= 0) || (xx + yy > 4.0f))
			return i;
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;       
    } while (1);
} // CalcMandelbrot
#else
__device__ inline int CalcMandelbrot(const float xPos, const float yPos, const int crunch)
{
    float y = yPos;
    float x = xPos;
    float yy = y * y;
    float xx = x * x;
    int i = crunch;

    while (--i && (xx + yy < 4.0f)) {
        y = x * y * 2.0f + yPos;
        x = xx - yy + xPos;
        yy = y * y;
        xx = x * x;
    }
    return i // i > 0 ? crunch - i : 0;
} // CalcMandelbrot
#endif

// The core Mandelbrot calculation function in double precision
__device__ inline int CalcMandelbrotDS(const float xPos0, const float xPos1, const float yPos0, const float yPos1, const int crunch)
{
    float xx0, xx1;
    float yy0, yy1;
    float sum0, sum1;
    int i = crunch;

	float y0 = yPos0;	// y = yPos;
	float y1 = yPos1;
	float x0 = xPos0;	// x = xPos;
	float x1 = xPos1;
    dsmul(yy0, yy1, y0, y1, y0, y1);    // yy = y * y;
    dsmul(xx0, xx1, x0, x1, x0, x1);	// xx = x * x;
    dsadd(sum0, sum1, xx0, xx1, yy0, yy1);	// sum = xx + yy;
    while (--i && (sum0 + sum1 < 4.0f)) {
        dsmul(y0, y1, x0, x1, y0, y1);		// y = x * y * 2.0f + yPos;
        dsadd(y0, y1, y0, y1, y0, y1);
        dsadd(y0, y1, y0, y1, yPos0, yPos1);

        dssub(x0, x1, xx0, xx1, yy0, yy1);	//  x = xx - yy + xPos;
        dsadd(x0, x1, x0, x1, xPos0, xPos1);

		dsmul(yy0, yy1, y0, y1, y0, y1);    // yy = y * y;
		dsmul(xx0, xx1, x0, x1, x0, x1);	// xx = x * x;
		dsadd(sum0, sum1, xx0, xx1, yy0, yy1);	// sum = xx + yy;
    }
    return i;
} // CalcMandelbrotDS

// The Mandelbrot CUDA GPU thread function
__global__ void Mandelbrot0(uchar4 *dst, const int imageW, const int imageH, const int crunch, const float xOff, const float yOff, const float scale, const uchar4 colors, const int frame, const int animationFrame)
{
    const int ix = blockDim.x * blockIdx.x + threadIdx.x;
    const int iy = blockDim.y * blockIdx.y + threadIdx.y;

    if ((ix < imageW) && (iy < imageH)) {
		// Calculate the location
		const float xPos = (float)ix * scale + xOff;
		const float yPos = (float)iy * scale + yOff;
		      
        // Calculate the Mandelbrot index for the current location
        int m = CalcMandelbrot(xPos, yPos, crunch);
        m = m > 0 ? crunch - m : 0;
			
        // Convert the Madelbrot index into a color
        uchar4 color;
        if (m) {
			m += animationFrame;
			color.x = m * colors.x;
			color.y = m * colors.y;
			color.z = m * colors.z;
		} else {
			color.x = 0;
			color.y = 0;
			color.z = 0;
		}
		
        // Output the pixel
 		int pixel = imageW * iy + ix;
        if (frame == 0) {
			color.w = 0;
			dst[pixel] = color;
        } else {
			int frame1 = frame + 1;
			int frame2 = frame1 / 2;
			dst[pixel].x = (dst[pixel].x * frame + color.x + frame2) / frame1;
			dst[pixel].y = (dst[pixel].y * frame + color.y + frame2) / frame1;
			dst[pixel].z = (dst[pixel].z * frame + color.z + frame2) / frame1;
        }
    }
} // Mandelbrot0

// The Mandelbrot CUDA GPU thread function
__global__ void MandelbrotDS0(uchar4 *dst, const int imageW, const int imageH, const int crunch, const float xOff0, const float xOff1, const float yOff0, const float yOff1, const float scale, const uchar4 colors, const int frame, const int animationFrame)
{
    const int ix = blockDim.x * blockIdx.x + threadIdx.x;
    const int iy = blockDim.y * blockIdx.y + threadIdx.y;

    if ((ix < imageW) && (iy < imageH)) {
		// Calculate the location
		float xPos0 = (float)ix * scale;
		float xPos1 = 0.0f;
		float yPos0 = (float)iy * scale;
		float yPos1 = 0.0f;
		dsadd(xPos0, xPos1, xPos0, xPos1, xOff0, xOff1);
		dsadd(yPos0, yPos1, yPos0, yPos1, yOff0, yOff1);

        // Calculate the Mandelbrot index for the current location
        int m = CalcMandelbrotDS(xPos0, xPos1, yPos0, yPos1, crunch);
        m = m > 0 ? crunch - m : 0;
			
        // Convert the Madelbrot index into a color
        uchar4 color;
        if (m) {
			m += animationFrame;
			color.x = m * colors.x;
			color.y = m * colors.y;
			color.z = m * colors.z;
		} else {
			color.x = 0;
			color.y = 0;
			color.z = 0;
		}
		
        // Output the pixel
 		int pixel = imageW * iy + ix;
        if (frame == 0) {
			color.w = 0;
			dst[pixel] = color;
        } else {
			int frame1 = frame + 1;
			int frame2 = frame1 / 2;
			dst[pixel].x = (dst[pixel].x * frame + color.x + frame2) / frame1;
			dst[pixel].y = (dst[pixel].y * frame + color.y + frame2) / frame1;
			dst[pixel].z = (dst[pixel].z * frame + color.z + frame2) / frame1;
        }
    }
} // MandelbrotDS0

// Determine if two pixel colors are within tolerance
__device__ inline int CheckColors(const uchar4 &color0, const uchar4 &color1)
{
	int x = color1.x - color0.x;
	int y = color1.y - color0.y;
	int z = color1.z - color0.z;
	return (ABS(x) > 10) || (ABS(y) > 10) || (ABS(z) > 10);
} // CheckColors

// The Mandelbrot secondary AA pass CUDA GPU thread function
__global__ void Mandelbrot1(uchar4 *dst, const int imageW, const int imageH, const int crunch, const float xOff, const float yOff, const float scale, const uchar4 colors, const int frame, const int animationFrame)
{
    const int ix = blockDim.x * blockIdx.x + threadIdx.x;
    const int iy = blockDim.y * blockIdx.y + threadIdx.y;

    if ((ix < imageW) && (iy < imageH)) {
		// Get the current pixel color
 		int pixel = imageW * iy + ix;
		uchar4 pixelColor = dst[pixel];
		int count = 0;
		
		// Search for pixels out of tolerance surrounding the current pixel
		if (ix > 0)
			count += CheckColors(pixelColor, dst[pixel - 1]);
		if (ix + 1 < imageW)
			count += CheckColors(pixelColor, dst[pixel + 1]);
		if (iy > 0)
			count += CheckColors(pixelColor, dst[pixel - imageW]);
		if (iy + 1 < imageH)
			count += CheckColors(pixelColor, dst[pixel + imageW]);
		if (count) {
			// Calculate the location
			const float xPos = (float)ix * scale + xOff;
			const float yPos = (float)iy * scale + yOff;
			      
			// Calculate the Mandelbrot index for the current location
			int m = CalcMandelbrot(xPos, yPos, crunch);
			m = m > 0 ? crunch - m : 0;
	        
			// Convert the Madelbrot index into a color
			uchar4 color;
			if (m) {
				m += animationFrame;
				color.x = m * colors.x;
				color.y = m * colors.y;
				color.z = m * colors.z;
			} else {
				color.x = 0;
				color.y = 0;
				color.z = 0;
			}
			
			// Output the pixel
			int frame1 = frame + 1;
			int frame2 = frame1 / 2;
			dst[pixel].x = (pixelColor.x * frame + color.x + frame2) / frame1;
			dst[pixel].y = (pixelColor.y * frame + color.y + frame2) / frame1;
			dst[pixel].z = (pixelColor.z * frame + color.z + frame2) / frame1;
		}
    }
} // Mandelbrot1

// The Mandelbrot secondary AA pass CUDA GPU thread function
__global__ void MandelbrotDS1(uchar4 *dst, const int imageW, const int imageH, const int crunch, const float xOff0, const float xOff1, const float yOff0, const float yOff1, const float scale, const uchar4 colors, const int frame, const int animationFrame)
{
    const int ix = blockDim.x * blockIdx.x + threadIdx.x;
    const int iy = blockDim.y * blockIdx.y + threadIdx.y;

    if ((ix < imageW) && (iy < imageH)) {
		// Get the current pixel color
 		int pixel = imageW * iy + ix;
		uchar4 pixelColor = dst[pixel];
		int count = 0;
		
		// Search for pixels out of tolerance surrounding the current pixel
		if (ix > 0)
			count += CheckColors(pixelColor, dst[pixel - 1]);
		if (ix + 1 < imageW)
			count += CheckColors(pixelColor, dst[pixel + 1]);
		if (iy > 0)
			count += CheckColors(pixelColor, dst[pixel - imageW]);
		if (iy + 1 < imageH)
			count += CheckColors(pixelColor, dst[pixel + imageW]);
		if (count) {
			// Calculate the location
			float xPos0 = (float)ix * scale;
			float xPos1 = 0.0f;
			float yPos0 = (float)iy * scale;
			float yPos1 = 0.0f;
			dsadd(xPos0, xPos1, xPos0, xPos1, xOff0, xOff1);
			dsadd(yPos0, yPos1, yPos0, yPos1, yOff0, yOff1);
			      
			// Calculate the Mandelbrot index for the current location
			int m = CalcMandelbrotDS(xPos0, xPos1, yPos0, yPos1, crunch);
			m = m > 0 ? crunch - m : 0;
	        
			// Convert the Madelbrot index into a color
			uchar4 color;
			if (m) {
				m += animationFrame;
				color.x = m * colors.x;
				color.y = m * colors.y;
				color.z = m * colors.z;
			} else {
				color.x = 0;
				color.y = 0;
				color.z = 0;
			}
			
			// Output the pixel
			int frame1 = frame + 1;
			int frame2 = frame1 / 2;
			dst[pixel].x = (pixelColor.x * frame + color.x + frame2) / frame1;
			dst[pixel].y = (pixelColor.y * frame + color.y + frame2) / frame1;
			dst[pixel].z = (pixelColor.z * frame + color.z + frame2) / frame1;
		}
    }
} // MandelbrotDS1

// Increase the grid size by 1 if the image width or height does not divide evenly
// by the thread block dimensions
inline int iDivUp(int a, int b)
{
    return ((a % b) != 0) ? (a / b + 1) : (a / b);
} // iDivUp

// The host CPU Mandebrot thread spawner
void RunMandelbrot0(uchar4 *dst, const int imageW, const int imageH, const int crunch, const double xOff, const double yOff, const double scale, const uchar4 colors, const int frame, const int animationFrame)
{
    dim3 threads(BLOCKDIM_X, BLOCKDIM_Y);
    dim3 grid(iDivUp(imageW, BLOCKDIM_X), iDivUp(imageH, BLOCKDIM_Y));

	if (scale < 0.0000002f) {
		float x0, x1, y0, y1;
		dsdeq(x0, x1, xOff);
		dsdeq(y0, y1, yOff);
		MandelbrotDS0<<<grid, threads>>>(dst, imageW, imageH, crunch, x0, x1, y0, y1, (float)scale, colors, frame, animationFrame);
	} else
		Mandelbrot0<<<grid, threads>>>(dst, imageW, imageH, crunch, (float)xOff, (float)yOff, (float)scale, colors, frame, animationFrame);
    CUT_CHECK_ERROR("Mandelbrot kernel execution failed.\n");
} // RunMandelbrot0

// The host CPU Mandebrot thread spawner
void RunMandelbrot1(uchar4 *dst, const int imageW, const int imageH, const int crunch, const double xOff, const double yOff, const double scale, const uchar4 colors, const int frame, const int animationFrame)
{
    dim3 threads(BLOCKDIM_X, BLOCKDIM_Y);
    dim3 grid(iDivUp(imageW, BLOCKDIM_X), iDivUp(imageH, BLOCKDIM_Y));

	if (scale < 0.0000002f) {
		float x0, x1, y0, y1;
		dsdeq(x0, x1, xOff);
		dsdeq(y0, y1, yOff);
		MandelbrotDS1<<<grid, threads>>>(dst, imageW, imageH, crunch, x0, x1, y0, y1, (float)scale, colors, frame, animationFrame);
	} else
		Mandelbrot1<<<grid, threads>>>(dst, imageW, imageH, crunch, (float)xOff, (float)yOff, (float)scale, colors, frame, animationFrame);
    CUT_CHECK_ERROR("Mandelbrot kernel execution failed.\n");
} // RunMandelbrot1

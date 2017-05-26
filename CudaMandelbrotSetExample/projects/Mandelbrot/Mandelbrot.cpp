// Mandelbrot sample
// submitted by Mark Granger, NewTek

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <GL/glew.h>
#include <GL/glut.h>
#include <cuda_runtime_api.h>
#include <cuda_gl_interop.h>
#include "cutil.h"
#include "Mandelbrot_kernel.h"
#include "Mandelbrot_gold.h"

// Set to 1 to run on the CPU instead of the GPU for timing comparison.
#define RUN_CPU 0

// Set to 1 to time frame generation
#define RUN_TIMING 0

// Random number macros
#define RANDOMSEED(seed) ((seed) = ((seed) * 1103515245 + 12345))
#define RANDOMBITS(seed, bits) ((unsigned int)RANDOMSEED(seed) >> (32 - (bits)))

//OpenGL PBO and texture "names"
GLuint gl_PBO, gl_Tex;

//Source image on the host side
uchar4 *h_Src;

//Original image width and height
int imageW, imageH;

// Starting iteration limit
int crunch = 512;

// Starting position and scale
double xOff = -0.5;
double yOff = 0.0;
double scale = 3.2;

// Starting stationary position and scale motion
double xdOff = 0.0;
double ydOff = 0.0;
double dscale = 1.0;

// Starting animation frame and anti-aliasing pass 
int animationFrame = 0;
int animationStep = 0;
int pass = 0;

// Starting color multipliers and random seed
int colorSeed = 0;
uchar4 colors;

// Timer ID
unsigned int hTimer;

// User interface variables
int lastx = 0;
int lasty = 0;
bool leftClicked = false;
bool rightClicked = false;

#define BUFFER_DATA(i) ((char *)0 + i)

// Get a sub-pixel sample location
void GetSample(int sampleIndex, float &x, float &y)
{    
    static const unsigned char pairData[128][2] = {
        { 64,  64}, {  0,   0}, {  1,  63}, { 63,   1}, { 96,  32}, { 97,  95}, { 36,  96}, { 30,  31},
        { 95, 127}, {  4,  97}, { 33,  62}, { 62,  33}, { 31, 126}, { 67,  99}, { 99,  65}, {  2,  34},
        { 81,  49}, { 19,  80}, {113,  17}, {112, 112}, { 80,  16}, {115,  81}, { 46,  15}, { 82,  79},
        { 48,  78}, { 16,  14}, { 49, 113}, {114,  48}, { 45,  45}, { 18,  47}, { 20, 109}, { 79, 115},
        { 65,  82}, { 52,  94}, { 15, 124}, { 94, 111}, { 61,  18}, { 47,  30}, { 83, 100}, { 98,  50},
        {110,   2}, {117,  98}, { 50,  59}, { 77,  35}, {  3, 114}, {  5,  77}, { 17,  66}, { 32,  13},
        {127,  20}, { 34,  76}, { 35, 110}, {100,  12}, {116,  67}, { 66,  46}, { 14,  28}, { 23,  93},
        {102,  83}, { 86,  61}, { 44, 125}, { 76,   3}, {109,  36}, {  6,  51}, { 75,  89}, { 91,  21},
        { 60, 117}, { 29,  43}, {119,  29}, { 74,  70}, {126,  87}, { 93,  75}, { 71,  24}, {106, 102},
        {108,  58}, { 89,   9}, {103,  23}, { 72,  56}, {120,   8}, { 88,  40}, { 11,  88}, {104, 120},
        { 57, 105}, {118, 122}, { 53,   6}, {125,  44}, { 43,  68}, { 58,  73}, { 24,  22}, { 22,   5},
        { 40,  86}, {122, 108}, { 87,  90}, { 56,  42}, { 70, 121}, {  8,   7}, { 37,  52}, { 25,  55},
        { 69,  11}, { 10, 106}, { 12,  38}, { 26,  69}, { 27, 116}, { 38,  25}, { 59,  54}, {107,  72},
        {121,  57}, { 39,  37}, { 73, 107}, { 85, 123}, { 28, 103}, {123,  74}, { 55,  85}, {101,  41},
        { 42, 104}, { 84,  27}, {111,  91}, {  9,  19}, { 21,  39}, { 90,  53}, { 41,  60}, { 54,  26},
        { 92, 119}, { 51,  71}, {124, 101}, { 68,  92}, { 78,  10}, { 13, 118}, {  7,  84}, {105,   4}
    };

    x = (1.0f / 128.0f) * (0.5f + (float)pairData[sampleIndex][0]);
    y = (1.0f / 128.0f) * (0.5f + (float)pairData[sampleIndex][1]);
} // GetSample

// OpenGL display function
void displayFunc(void)
{
    if ((xdOff != 0.0) || (ydOff != 0.0)) {
        xOff += xdOff;
        yOff += ydOff;
        pass = 0;
    }
    if (dscale != 1.0) {
        scale *= dscale;
        pass = 0;
    }
    if (animationStep) {
        animationFrame -= animationStep;
        pass = 0;
    }
#if RUN_TIMING
    pass = 0;
#endif
#if RUN_CPU
    if (pass < 128) {
        int startPass = pass;
        uchar4 *d_dst = NULL;
        float xs, ys;
        cutResetTimer(hTimer);
        CUDA_SAFE_CALL(cudaGLMapBufferObject((void**)&d_dst, gl_PBO));

        // Get the anti-alias sub-pixel sample location
        GetSample(pass & 127, xs, ys);

        // Get the pixel scale and offset
        double s = scale / (double)imageW;
        double x = (xs - (double)imageW * 0.5f) * s + xOff;
        double y = (ys - (double)imageH * 0.5f) * s + yOff;

        // Run the mandelbrot generator
        if (pass && !startPass) // Use the adaptive sampling version when animating.
            RunMandelbrotDSGold1(h_Src, imageW, imageH, crunch, x, y, s, colors, pass++, animationFrame);
        else
            RunMandelbrotDSGold0(h_Src, imageW, imageH, crunch, x, y, s, colors, pass++, animationFrame);
        CUDA_SAFE_CALL(cudaMemcpy(d_dst, h_Src, imageW * imageH * sizeof(uchar4), cudaMemcpyHostToDevice));
        CUDA_SAFE_CALL(cudaGLUnmapBufferObject(gl_PBO));
#if RUN_TIMING
        printf("CPU = %5.8f\n", 0.001f * cutGetTimerValue(hTimer));
#endif
    }

#else
    if (pass < 128) {
        float timeEstimate;
        int startPass = pass;
        uchar4 *d_dst = NULL;
        cutResetTimer(hTimer);
        CUDA_SAFE_CALL(cudaGLMapBufferObject((void**)&d_dst, gl_PBO));

        // Render anti-aliasing passes until we run out time (60fps approximately)
        do {
            float xs, ys;

            // Get the anti-alias sub-pixel sample location
            GetSample(pass & 127, xs, ys);

            // Get the pixel scale and offset
            double s = scale / (float)imageW;
            double x = (xs - (double)imageW * 0.5f) * s + xOff;
            double y = (ys - (double)imageH * 0.5f) * s + yOff;

            // Run the mandelbrot generator
            if (pass && !startPass) // Use the adaptive sampling version when animating.
                RunMandelbrot1(d_dst, imageW, imageH, crunch, x, y, s, colors, pass++, animationFrame);
            else
                RunMandelbrot0(d_dst, imageW, imageH, crunch, x, y, s, colors, pass++, animationFrame);
            cudaThreadSynchronize();

            // Estimate the total time of the frame if one more pass is rendered
            timeEstimate = 0.001f * cutGetTimerValue(hTimer) * ((float)(pass + 1 - startPass) / (float)(pass - startPass));
        } while ((pass < 128) && (timeEstimate < 1.0f / 60.0f) && !RUN_TIMING);
        CUDA_SAFE_CALL(cudaGLUnmapBufferObject(gl_PBO));
#if RUN_TIMING
        printf("GPU = %5.8f\n", 0.001f * cutGetTimerValue(hTimer));
#endif
    }
#endif

    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, imageW, imageH, GL_RGBA, GL_UNSIGNED_BYTE, BUFFER_DATA(0));
    glBegin(GL_TRIANGLES);
    glTexCoord2f(0.0f, 0.0f);
    glVertex2f(-1.0f, -1.0f);
    glTexCoord2f(2.0f, 0.0f);
    glVertex2f(3.0f, -1.0f);
    glTexCoord2f(0.0f, 2.0f);
    glVertex2f(-1.0f, 3.0f);
    glEnd();

	glutSwapBuffers();
} // displayFunc

// OpenGL keyboard function
void keyboardFunc(unsigned char k, int, int)
{
    int seed;
    switch (k){
        case '\033':
        case 'q':
        case 'Q':
            printf("Shutting down...\n");
                CUT_SAFE_CALL(cutStopTimer(hTimer) );
                CUT_SAFE_CALL(cutDeleteTimer(hTimer));
                CUDA_SAFE_CALL(cudaGLUnregisterBufferObject(gl_PBO));
                glBindBuffer(GL_PIXEL_UNPACK_BUFFER_ARB, 0);
                glDeleteBuffers(1, &gl_PBO);
                glDeleteTextures(1, &gl_Tex);
                free(h_Src);
            printf("Shutdown done.\n");
            exit(0);
            break;

        case '?':
            printf("xOff = %5.8f\n", xOff);
            printf("yOff = %5.8f\n", yOff);
            printf("scale = %5.8f\n", scale);
            printf("detail = %d\n", crunch);
            printf("color = %d\n", colorSeed);
            printf("\n");
            break;
        
        case 'r': case 'R':
            // Reset all values to their defaults
            xOff = -0.5;
            yOff = 0.0;
            scale = 3.2;
            xdOff = 0.0;
            ydOff = 0.0;
            dscale = 1.0;
            colorSeed = 0;
            colors.x = 3;
            colors.y = 5;
            colors.z = 7;
            crunch = 512;
            animationFrame = 0;
            animationStep = 0;
            pass = 0;
            break;

        case 'c':
            seed = ++colorSeed;
            if (seed) {
                colors.x = RANDOMBITS(seed, 4);
                colors.y = RANDOMBITS(seed, 4);
                colors.z = RANDOMBITS(seed, 4);
            } else {
                colors.x = 3;
                colors.y = 5;
                colors.z = 7;
            }
            pass = 0;
            break;
        
        case 'C':
            seed = --colorSeed;
            if (seed) {
                colors.x = RANDOMBITS(seed, 4);
                colors.y = RANDOMBITS(seed, 4);
                colors.z = RANDOMBITS(seed, 4);
            } else {
                colors.x = 3;
                colors.y = 5;
                colors.z = 7;
            }
            pass = 0;
            break;

        case 'a':
            if (animationStep < 0)
                animationStep = 0;
            else {
                animationStep++;
                if (animationStep > 8)
                    animationStep = 8;
            }
            break;

        case 'A':
            if (animationStep > 0)
                animationStep = 0;
            else {
                animationStep--;
                if (animationStep < -8)
                    animationStep = -8;
            }
            break;

        case 'd':
            if (crunch < 0x40000000) {
                crunch *= 2;
                pass = 0;
            }
            break;

        case 'D':
            if (crunch > 2) {
                crunch /= 2;
                pass = 0;
            }
            break;

        case '4':	// Left arrow key
			xOff -= 0.05f * scale;
            pass = 0;
		    break;
         
        case '8':	// Up arrow key
			yOff += 0.05f * scale;
            pass = 0;
		    break;
        
        case '6':	// Right arrow key
			xOff += 0.05f * scale;
            pass = 0;
		    break;
         
        case '2':	// Down arrow key
			yOff -= 0.05f * scale;
            pass = 0;
		    break;
		
        case '+':
			scale /= 1.1f;
            pass = 0;
		    break;
         
        case '-':
			scale *= 1.1f;
            pass = 0;
		    break;
		
		default:
		    break;
   }
} // keyboardFunc

// OpenGL mouse click function
void clickFunc(int button, int, int x, int y)
{
    if (button == 0)
        leftClicked = !leftClicked;
    if (button == 2)
        rightClicked = !rightClicked;
    lastx = x;
    lasty = y;
    xdOff = 0.0;
    ydOff = 0.0;
    dscale = 1.0;
} // clickFunc

// OpenGL mouse motion function
void motionFunc(int x, int y)
{
    double fx = (double)((x - lastx) / 10) / (double)(imageW);        
    double fy = (double)((lasty - y) / 10) / (double)(imageW);

    if (leftClicked) {
        xdOff = fx * scale;
        ydOff = fy * scale;
    } else {
        xdOff = 0.0f;
        ydOff = 0.0f;
    }
        
    if (rightClicked)
        if (fy > 0.0f) {
            dscale = 1.0 - fy;
            dscale = dscale < 1.05 ? dscale : 1.05;
        } else {
            dscale = 1.0 / (1.0 + fy);
            dscale = dscale > (1.0 / 1.05) ? dscale : (1.0 / 1.05);
        }
    else
        dscale = 1.0;
} // motionFunc

void idleFunc()
{
	glutPostRedisplay();
}

////////////////////////////////////////////////////////////////////////////////
// Main program
////////////////////////////////////////////////////////////////////////////////
int main(int argc, char **argv)
{
    CUT_DEVICE_INIT();
	imageW = 800;
	imageH = 600;
	h_Src = (uchar4*)malloc(imageW * imageH * 4);
    colors.w = 0;
    colors.x = 3;
    colors.y = 5;
    colors.z = 7;
    printf("Data init done.\n");

    printf("Initializing GLUT...\n");
        glutInit(&argc, argv);
        glutInitDisplayMode(GLUT_RGBA | GLUT_DOUBLE);
        glutInitWindowSize(imageW, imageH);
        glutInitWindowPosition(512 - imageW / 2, 384 - imageH / 2);
        glutCreateWindow(argv[0]);
        printf("Loading extensions: %s\n", glewGetErrorString(glewInit()));
        if(!glewIsSupported(
            "GL_VERSION_2_0 " 
            "GL_ARB_pixel_buffer_object "
            "GL_EXT_framebuffer_object "
        )){
            fprintf(stderr, "ERROR: Support for necessary OpenGL extensions missing.");
            fflush(stderr);
            return CUTFalse;
        }
    printf("OpenGL window created.\n");

    printf("Creating GL texture...\n");
        glEnable(GL_TEXTURE_2D);
        glGenTextures(1, &gl_Tex);
        glBindTexture(GL_TEXTURE_2D, gl_Tex);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, imageW, imageH, 0, GL_RGBA, GL_UNSIGNED_BYTE, h_Src);
    printf("Texture created.\n");

    printf("Creating PBO...\n");
        glGenBuffers(1, &gl_PBO);
        glBindBuffer(GL_PIXEL_UNPACK_BUFFER_ARB, gl_PBO);
        glBufferData(GL_PIXEL_UNPACK_BUFFER_ARB, imageW * imageH * 4, h_Src, GL_STREAM_COPY);
        //While a PBO is registered to CUDA, it can't be used 
        //as the destination for OpenGL drawing calls.
        //But in our particular case OpenGL is only used 
        //to display the content of the PBO, specified by CUDA kernels,
        //so we need to register/unregister it only once.
        CUDA_SAFE_CALL( cudaGLRegisterBufferObject(gl_PBO) );
    printf("PBO created.\n");

    printf("Starting GLUT main loop...\n");
    printf("\n");
    printf("Press [?] to print location and scale\n");
    printf("Press [q] to exit\n");
    printf("Press [r] to reset\n");
    printf("Press [a] or [A] to animate the colors\n");
    printf("Press [c] or [C] to change the colors\n");
    printf("Press [d] or [D] to increase/decrease the detail\n");
    printf("Left mouse button + drag = Scroll\n");
    printf("Right mouse button + drag = Zoom\n");
    printf("\n");
    glutDisplayFunc(displayFunc);
    glutIdleFunc(idleFunc);
    glutKeyboardFunc(keyboardFunc);
    glutMouseFunc(clickFunc);
    glutMotionFunc(motionFunc);
    CUT_SAFE_CALL(cutCreateTimer(&hTimer));
    CUT_SAFE_CALL(cutStartTimer(hTimer));
    glutMainLoop();

    CUT_EXIT(argc, argv);
} // main

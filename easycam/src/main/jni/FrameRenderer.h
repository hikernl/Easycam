//
// Created by Eric on 12/31/2015.
//

#ifndef EASYCAM_FRAMERENDERER_H
#define EASYCAM_FRAMERENDERER_H

#include "util.h"
#include <android/native_window_jni.h>
#include <RenderScript.h>
#include "ScriptC_convert.h"

using namespace android::RSC;
/**
 * Class: FrameRenderer
 *        This class performs all color conversion (through renderscript) and output
 *        to the applications window
 */
class FrameRenderer {
public:
	FrameRenderer(JNIEnv* jenv, jstring rsPath, DeviceSettings* dSets);
    ~FrameRenderer();

	void renderFrame(JNIEnv* jenv, jobject surface, CaptureBuffer* inBuffer);

private:

	// The the first element of a frame in an allocation.  If allocation contains two interleaved
	// frames, there are two indices
    int firstFrameElementIndex;
    int secondFrameElementIndex;
	
	int32_t frameWindowFormat;  // The pixel format used by ANativeWindow
	int outputWindowHeight;		// Geometry for ANativeWindow
	int outputWindowWidth;

	sp<RS> rs;
    sp<Allocation> inputAlloc;			// Allocation containing the frame input
    sp<Allocation> scriptOutputAlloc;	// Allocation produced by a renderscript kernel
    sp<Allocation> pixelAlloc;			// Allocation that represents each pixel in the input buffer
	sp<Allocation> intrinsOutAlloc;		// Allocation produced by a YUV Intrinsic

    ScriptC_convert* script;
	sp<ScriptIntrinsicYuvToRGB> intrinsic;

	void initRenderscript(int inputFrameWidth,int inputFrameHeight, bool interleaved, int pixelsPerElement,  RSYuvFormat yuvFmt);
	void setupPixelAlloc(int pixelBufWidth, int pixelBufHeight, int pixelsPerElement, bool interleaved);

    // Function pointer to store which call we need to make, depending on YUV type
    void (FrameRenderer::* processFrame)(CaptureBuffer*, ANativeWindow*);

    // TODO: 3/25/2016 - Implement BLEND function stubs

	// YUYV functions.  The label after the underscore represents the kind of deinterlacing
    void processYUYV_SCAN(CaptureBuffer* inBuffer, ANativeWindow* window);
    void processYUYV_DISCARD(CaptureBuffer* inBuffer, ANativeWindow* window);
    void processYUYV_BOB(CaptureBuffer* inBuffer, ANativeWindow* window);
    void processYUYV_BLEND(CaptureBuffer* inBuffer, ANativeWindow* window);  // STUB

    // UYVY functions
    void processUYVY_SCAN(CaptureBuffer* inBuffer, ANativeWindow* window);
    void processUYVY_DISCARD(CaptureBuffer* inBuffer, ANativeWindow* window);
    void processUYVY_BOB(CaptureBuffer* inBuffer, ANativeWindow* window);
    void processUYVY_BLEND(CaptureBuffer* inBuffer, ANativeWindow* window); // STUB

    // RGB needs no color conversion, and with no deinterlacing it needs no calls to renderscript
    void processRGB_SCAN(CaptureBuffer* inBuffer, ANativeWindow* window);
    void processRGB_DISCARD(CaptureBuffer* inBuffer, ANativeWindow* window);
    void processRGB_BOB(CaptureBuffer* inBuffer, ANativeWindow* window);
    void processRGB_BLEND(CaptureBuffer* inBuffer, ANativeWindow* window);  // STUB

    // The below is for NV21 and YV12, which use YUV intrinsic
    void processIntrinsic_SCAN(CaptureBuffer* inBuffer, ANativeWindow* window);
    void processIntrinsic_DISCARD(CaptureBuffer* inBuffer, ANativeWindow* window);
    void processIntrinsic_BOB(CaptureBuffer* inBuffer, ANativeWindow* window);
    void processIntrinsic_BLEND(CaptureBuffer* inBuffer, ANativeWindow* window);  // STUB

};


#endif //EASYCAM_FRAMERENDERER_H

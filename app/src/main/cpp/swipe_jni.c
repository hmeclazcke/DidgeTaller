/* Bounded JNI adapter for SPTK SWIPE' with its documented boundary correction. */
#include <jni.h>
#include <math.h>
#include <pthread.h>
#include <setjmp.h>
#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>
#include <string.h>

#define MEMORY_LIMIT (32u * 1024u * 1024u)
#define INPUT_LIMIT 262144

typedef union Block Block;
union Block {
    struct { Block *previous, *next; size_t size; } value;
    max_align_t alignment;
};
typedef struct { Block *first; size_t used, peak; jmp_buf failure; } Allocation;
static _Thread_local Allocation *allocation;
static pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
static size_t last_peak;
extern double *_sintbl;
extern int maxfftsize;
void swipe(double *, double *, int, int, int, double, double, double, int);

void *swipe_alloc(size_t size) {
    if (!size) size = 1;
    if (MEMORY_LIMIT - allocation->used < sizeof(Block) ||
        size > MEMORY_LIMIT - allocation->used - sizeof(Block)) longjmp(allocation->failure, 1);
    Block *block = malloc(sizeof(Block) + size);
    if (!block) longjmp(allocation->failure, 1);
    block->value.previous = NULL;
    block->value.next = allocation->first;
    block->value.size = sizeof(Block) + size;
    if (allocation->first) allocation->first->value.previous = block;
    allocation->first = block;
    allocation->used += block->value.size;
    if (allocation->used > allocation->peak) allocation->peak = allocation->used;
    return block + 1;
}
void *swipe_calloc(size_t count, size_t size) {
    if (count && size > SIZE_MAX / count) longjmp(allocation->failure, 1);
    size_t bytes = count * size;
    void *result = swipe_alloc(bytes);
    memset(result, 0, bytes);
    return result;
}
void swipe_free(void *pointer) {
    if (!pointer) return;
    Block *block = (Block *)pointer - 1;
    if (block->value.previous) block->value.previous->value.next = block->value.next;
    else allocation->first = block->value.next;
    if (block->value.next) block->value.next->value.previous = block->value.previous;
    allocation->used -= block->value.size;
    free(block);
}

JNIEXPORT jdoubleArray JNICALL
Java_ar_didgetaller_core_SwipeNative_estimate(JNIEnv *env, jobject self, jshortArray samples,
        jint count, jint rate, jint hop, jint minimum, jint maximum) {
    (void) self;
    if (!samples || count < 1 || count > INPUT_LIMIT || count > (*env)->GetArrayLength(env, samples) ||
        rate != 44100 || hop != 8192 || minimum != 35 || maximum != 140) {
        (*env)->ThrowNew(env, (*env)->FindClass(env,"java/lang/IllegalArgumentException"), "Invalid SWIPE input");
        return NULL;
    }
    int window = (int)pow(2.0, round(log2(8.0 * rate / minimum)));
    if (count < window / 2) {
        (*env)->ThrowNew(env, (*env)->FindClass(env,"java/lang/IllegalArgumentException"), "SWIPE input is too short");
        return NULL;
    }
    pthread_mutex_lock(&mutex);
    Allocation *context = calloc(1, sizeof(Allocation));
    if (!context) {
        pthread_mutex_unlock(&mutex);
        (*env)->ThrowNew(env, (*env)->FindClass(env,"java/lang/IllegalStateException"), "Native working memory unavailable");
        return NULL;
    }
    allocation = context;
    jdoubleArray result = NULL;
    int failed = setjmp(context->failure);
    if (!failed) {
        jshort *pcm = swipe_alloc((size_t)count * sizeof(jshort));
        (*env)->GetShortArrayRegion(env, samples, 0, count, pcm);
        if (!(*env)->ExceptionCheck(env)) {
            double *input = swipe_alloc((size_t)count * sizeof(double));
            int frames = (count + hop - 1) / hop;
            double *output = swipe_calloc((size_t)frames, sizeof(double));
            for (int i=0; i<count; i++) input[i] = pcm[i];
            swipe(input, output, count, rate, hop, minimum, maximum, 0.3, 1);
            result = (*env)->NewDoubleArray(env, frames);
            if (result) (*env)->SetDoubleArrayRegion(env, result, 0, frames, output);
        }
    }
    last_peak = context->peak;
    _sintbl = NULL;
    maxfftsize = 0;
    while (context->first) swipe_free(context->first + 1);
    allocation = NULL;
    free(context);
    pthread_mutex_unlock(&mutex);
    if (failed) {
        (*env)->ThrowNew(env, (*env)->FindClass(env,"java/lang/IllegalStateException"), "SWIPE memory budget exceeded");
        return NULL;
    }
    return result;
}

JNIEXPORT jlong JNICALL
Java_ar_didgetaller_core_SwipeNative_peakBytes(JNIEnv *env, jobject self) {
    (void)env; (void)self;
    pthread_mutex_lock(&mutex);
    size_t result = last_peak;
    pthread_mutex_unlock(&mutex);
    return (jlong)result;
}

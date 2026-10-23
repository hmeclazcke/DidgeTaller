#ifndef DIDGE_TRACKED_ALLOC_H
#define DIDGE_TRACKED_ALLOC_H
#include <stdlib.h>
void *swipe_alloc(size_t size);
void *swipe_calloc(size_t count, size_t size);
void swipe_free(void *pointer);
#define malloc swipe_alloc
#define calloc swipe_calloc
#define free swipe_free
#endif

/* Minimal compatibility declarations for the vendored SPTK FFT sources.
 * Numerical constants and signatures match the original SPTK.h.
 * Unrelated SPTK tool APIs are intentionally not included. */
#ifndef DIDGE_SPTK_FFT_API_H
#define DIDGE_SPTK_FFT_API_H
#define PI 3.14159265358979323846
int fft(double *x, double *y, const int m);
double *dgetmem(const int length);
#endif

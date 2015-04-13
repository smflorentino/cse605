#ifndef FP_FIVMR_CONFIG_H
#define FP_FIVMR_CONFIG_H

#if FIVMBUILD_SPECIALIZING
#include <fivmc_config.h>
#else
#include <fivmc_def_config.h>
#endif

#if FIVMC_COMPILE
#include <fivmc_extra_includes.h>
#endif

#define FIVMR_COPYRIGHT "(c) 2009, 2010, 2011, 2012, 2013, 2014 Fiji Systems Inc."
#define FIVMR_VENDOR "Fiji Systems Inc."
#define FIVMR_BASE_VERSION "v0.9.1"

#endif


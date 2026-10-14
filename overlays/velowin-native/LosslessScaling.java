package com.winlator.cmod.runtime.display.lsfg;

/*
 * Legacy API facade only: VeloWin no longer installs, loads or redistributes
 * Lossless Scaling DLLs or its Vulkan shader chain. Kept to avoid breaking
 * Winlator-derived Activity ABI until that monolithic code is split.
 *
 * Copyright (c) VeloWin contributors. GPL-3.0-or-later.
 */
import android.content.Context;
import android.net.Uri;
import com.winlator.cmod.runtime.container.Container;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class LosslessScaling {
    public static final int STATUS_OK=0, STATUS_NOT_INSTALLED=1, STATUS_UNREADABLE_FILE=2,
        STATUS_NOT_PORTABLE_EXECUTABLE=3, STATUS_MISSING_SHADERS=4,
        STATUS_TRANSLATION_FAILED=5, STATUS_CACHE_UNUSABLE=6;
    public static final int VARIANT_NONE=0, VARIANT_FP16=1, VARIANT_FP32=2, VARIANT_DXBC=3;
    private LosslessScaling() {}
    public static File getStoreDir(Context c){return new File(c.getFilesDir(),"lsfg-disabled");}
    public static File getCacheFile(Context c,boolean fp16){return null;}
    public static File resolveCacheFile(Context c,boolean fp16){return null;}
    public static boolean isInstalled(Context c){return false;}
    public static int getStatus(Context c,boolean fp16){return STATUS_NOT_INSTALLED;}
    public static boolean isCacheStale(Context c,File dll){return false;}
    public static int getVariant(Context c,boolean fp16){return VARIANT_NONE;}
    public static int dllVariant(File dll){return VARIANT_NONE;}
    public static int variantRank(int variant){return 0;}
    public static String variantName(int variant){return "disabled"; }
    public static int validate(File dll){return STATUS_NOT_INSTALLED;}
    public static int installFrom(Context c,File dll){return STATUS_NOT_INSTALLED;}
    public static int installFrom(Context c,Uri uri){return STATUS_NOT_INSTALLED;}
    public static boolean remove(Context c){return false;}
    public static List<File> findInContainers(Collection<Container> cs){return Collections.emptyList();}
    public static boolean isSupportedByGpu(Context c,String driver){return false;}
    public static void invalidateGpuSupport(){}
}

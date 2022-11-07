/*
 * Tencent is pleased to support the open source community by making Tinker available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yy.sdk.installer.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import java.io.Closeable;
import java.io.File;
import java.util.zip.ZipFile;

public class SharePatchFileUtil {

    private static final String DEX_SUFFIX = ".dex";
    private static final String ABOOSTER_DIR = "abooster";
    private static final String PATCH_DIR = "patch";

    /**
     * data dir,
     *
     * @param context
     * @return
     */
    public static File getWorkspaceDirectory(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        if (applicationInfo == null) {
            // Looks like running on a test Context, so just return without patching.
            return null;
        }

        return new File(applicationInfo.dataDir, ABOOSTER_DIR);
    }

    /**
     * 获取上一次源码发生变化的日期
     *
     * @param context
     * @return
     */
    public static long getLastSourceModified(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        if (applicationInfo == null) {
            return 0;
        }

        return new File(applicationInfo.sourceDir).lastModified();
    }

    /**
     * data dir,
     *
     * @param context
     * @return
     */
    public static File getPatchDirectory(Context context) {
        return new File(getWorkspaceDirectory(context), PATCH_DIR);
    }

    /**
     * change the jar file path as the makeDexElements do
     *
     * @param path
     * @param optimizedDirectory
     * @return
     */
    public static String optimizedPathFor(File path, File optimizedDirectory) {
        String fileName = path.getName();
        if (!fileName.endsWith(DEX_SUFFIX)) {
            int lastDot = fileName.lastIndexOf(".");
            if (lastDot < 0) {
                fileName += DEX_SUFFIX;
            } else {
                StringBuilder sb = new StringBuilder(lastDot + 4);
                sb.append(fileName, 0, lastDot);
                sb.append(DEX_SUFFIX);
                fileName = sb.toString();
            }
        }

        File result = new File(optimizedDirectory, fileName);
        return result.getPath();
    }

    /**
     * Closes the given {@code obj}. Suppresses any exceptions.
     */
    @SuppressLint("NewApi")
    public static void closeQuietly(Object obj) {
        if (obj == null) {
            return;
        }
        if (obj instanceof Closeable) {
            try {
                ((Closeable) obj).close();
            } catch (Throwable ignored) {
                // Ignored.
            }
        } else if (Build.VERSION.SDK_INT >= 19 && obj instanceof AutoCloseable) {
            try {
                ((AutoCloseable) obj).close();
            } catch (Throwable ignored) {
                // Ignored.
            }
        } else if (obj instanceof ZipFile) {
            try {
                ((ZipFile) obj).close();
            } catch (Throwable ignored) {
                // Ignored.
            }
        } else {
            throw new IllegalArgumentException("obj: " + obj + " cannot be closed.");
        }
    }


    public static void deleteDir(String path) {
        File pathFile = new File(path);
        if (!pathFile.exists()) return;
        File[] files = pathFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isFile()) {
                files[i].delete();
            } else if (files[i].isDirectory()) {
                deleteDir(files[i].getAbsolutePath());
                files[i].delete();
            }
        }
    }
}


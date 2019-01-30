package com.github.xch168.hotfixdemo;


import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;

import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HotfixHelper {

    public static void loadPatch(Context context, OnPatchLoadListener listener) {
        File patchFile = new File(context.getCacheDir() + "/hotfix.dex");
        if (patchFile.exists()) {
            patchFile.delete();
        }

        downloadPatch(patchFile, listener);
    }

    private static void downloadPatch(final File patchFile, final OnPatchLoadListener listener) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://pm3fh7vxn.bkt.clouddn.com/hotfix.dex")
                .get()
                .build();
        client.newCall(request)
              .enqueue(new Callback() {
                  @Override
                  public void onFailure(Call call, IOException e) {
                      if (listener != null) {
                          listener.onFailure();
                      }
                      e.printStackTrace();
                  }

                  @Override
                  public void onResponse(Call call, Response response) throws IOException {
                      if (response.code() == 200) {
                          FileOutputStream fos = new FileOutputStream(patchFile);
                          fos.write(response.body().bytes());
                          fos.close();
                          if (listener != null) {
                              listener.onSuccess();
                          }
                      } else {
                          if (listener != null) {
                              listener.onFailure();
                          }
                      }

                  }
              });
    }

    public static void applyPatch(Context context) {
        ClassLoader classLoader = context.getClassLoader();
        Class loaderClass = BaseDexClassLoader.class;
        try {
            Object hostPathList = ReflectUtil.getField(loaderClass, classLoader, "pathList");
            Object hostDexElement = ReflectUtil.getField(hostPathList.getClass(), hostPathList, "dexElements");

            File optimizeDir = new File(context.getCacheDir() + "/optimize");
            if (!optimizeDir.exists()) {
                optimizeDir.mkdir();
            }

            DexClassLoader patchClassLoader = new DexClassLoader(context.getCacheDir() + "/hotfix.dex", optimizeDir.getPath(), null, classLoader);
            Object patchPathList = ReflectUtil.getField(loaderClass, patchClassLoader, "pathList");
            Object patchDexElement = ReflectUtil.getField(patchPathList.getClass(), patchPathList, "dexElements");

            Object newDexElements = combineArray(hostDexElement, patchDexElement);
            ReflectUtil.setField(hostPathList.getClass(), hostPathList, "dexElements", newDexElements);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static Object combineArray(Object hostElements, Object patchElements) {
        Class<?> componentType = hostElements.getClass().getComponentType();
        int i = Array.getLength(hostElements);
        int j = Array.getLength(patchElements);
        int k = i + j;
        Object result = Array.newInstance(componentType, k);
        System.arraycopy(patchElements, 0, result, 0, j);
        System.arraycopy(hostElements, 0, result, j, i);
        return result;
    }

    public static boolean hasPatch(Context context) {
        return new File(context.getCacheDir() + "/hotfix.dex").exists();
    }

    public interface OnPatchLoadListener {
        void onSuccess();
        void onFailure();
    }
}

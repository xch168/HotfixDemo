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
        File patchFile = new File(context.getCacheDir() + "/patch.dex");
        if (patchFile.exists()) {
            patchFile.delete();
        }

        downloadPatch(patchFile, listener);
    }

    private static void downloadPatch(final File patchFile, final OnPatchLoadListener listener) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("http://pm3fh7vxn.bkt.clouddn.com/patch.dex")
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
        // 获取宿主的ClassLoader
        ClassLoader classLoader = context.getClassLoader();
        Class loaderClass = BaseDexClassLoader.class;
        try {
            // 获取宿主ClassLoader的pathList对象
            Object hostPathList = ReflectUtil.getField(loaderClass, classLoader, "pathList");
            // 获取宿主pathList对象中的dexElements数组对象
            Object hostDexElement = ReflectUtil.getField(hostPathList.getClass(), hostPathList, "dexElements");

            File optimizeDir = new File(context.getCacheDir() + "/optimize");
            if (!optimizeDir.exists()) {
                optimizeDir.mkdir();
            }
            // 创建补丁包的类加载器
            DexClassLoader patchClassLoader = new DexClassLoader(context.getCacheDir() + "/patch.dex", optimizeDir.getPath(), null, classLoader);
            // 获取补丁ClassLoader中的pathList对象
            Object patchPathList = ReflectUtil.getField(loaderClass, patchClassLoader, "pathList");
            // 获取补丁pathList对象中的dexElements数组对象
            Object patchDexElement = ReflectUtil.getField(patchPathList.getClass(), patchPathList, "dexElements");

            // 合并宿主中的dexElements和补丁中的dexElements，并把补丁的dexElements放在数组的头部
            Object newDexElements = combineArray(hostDexElement, patchDexElement);
            // 将合并完成的dexElements设置到宿主ClassLoader中去
            ReflectUtil.setField(hostPathList.getClass(), hostPathList, "dexElements", newDexElements);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param hostElements    宿主中的dexElements
     * @param patchElements   补丁包中的dexElements
     * @return Object         合并成的dexElements
     */
    private static Object combineArray(Object hostElements, Object patchElements) {
        Class<?> componentType = hostElements.getClass().getComponentType();
        int i = Array.getLength(hostElements);
        int j = Array.getLength(patchElements);
        int k = i + j;
        Object result = Array.newInstance(componentType, k);
        // 将补丁包的dexElements合并到头部
        System.arraycopy(patchElements, 0, result, 0, j);
        System.arraycopy(hostElements, 0, result, j, i);
        return result;
    }

    public static boolean hasPatch(Context context) {
        return new File(context.getCacheDir() + "/patch.dex").exists();
    }

    public interface OnPatchLoadListener {
        void onSuccess();
        void onFailure();
    }
}

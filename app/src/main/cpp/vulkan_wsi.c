/*
 * Vulkan WSI proof-of-life on an Android Surface. No Windows window server,
 * game renderer, DXVK or VKD3D. It performs read-only swapchain capability checks
 * after creating/destroying a VkSurfaceKHR from the Java SurfaceView.
 */
#define VK_USE_PLATFORM_ANDROID_KHR 1
#include <jni.h>
#include <vulkan/vulkan.h>
#include <android/native_window_jni.h>
#include <stdio.h>
#include <string.h>
#include <stdint.h>

static jstring result(JNIEnv *env, const char *s) {
    return (*env)->NewStringUTF(env, s);
}

JNIEXPORT jstring JNICALL
Java_dev_velowin_app_hardware_VulkanWsiLabActivity_probeSurface(
    JNIEnv *env, jclass clazz, jobject androidSurface) {
    (void)clazz;
    if (!androidSurface) return result(env, "Surface do Android não disponível.");
    ANativeWindow *window = ANativeWindow_fromSurface(env, androidSurface);
    if (!window) return result(env, "ANativeWindow_fromSurface falhou.");

    char report[1200];
    VkInstance instance = VK_NULL_HANDLE;
    VkSurfaceKHR surface = VK_NULL_HANDLE;
    VkResult code = VK_ERROR_INITIALIZATION_FAILED;
    uint32_t extensionCount = 0;
    int hasSurface = 0, hasAndroidSurface = 0;
    code = vkEnumerateInstanceExtensionProperties(NULL, &extensionCount, NULL);
    if (code != VK_SUCCESS || extensionCount > 1024) {
        snprintf(report,sizeof(report),"Falha listando extensões Vulkan (%d)",(int)code);
        goto finish;
    }
    VkExtensionProperties extension[1024];
    if (extensionCount > 0) {
        code = vkEnumerateInstanceExtensionProperties(NULL, &extensionCount, extension);
        if (code != VK_SUCCESS) {
            snprintf(report,sizeof(report),"Falha consultando extensões Vulkan (%d)",(int)code);
            goto finish;
        }
        for (uint32_t i=0; i<extensionCount; ++i) {
            if (!strcmp(extension[i].extensionName,VK_KHR_SURFACE_EXTENSION_NAME)) hasSurface=1;
            if (!strcmp(extension[i].extensionName,VK_KHR_ANDROID_SURFACE_EXTENSION_NAME)) hasAndroidSurface=1;
        }
    }
    if (!hasSurface || !hasAndroidSurface) {
        snprintf(report,sizeof(report),"WSI indisponível: VK_KHR_surface=%s, VK_KHR_android_surface=%s.",
            hasSurface?"sim":"não", hasAndroidSurface?"sim":"não");
        goto finish;
    }
    const char *required[] = {
        VK_KHR_SURFACE_EXTENSION_NAME,
        VK_KHR_ANDROID_SURFACE_EXTENSION_NAME
    };
    VkApplicationInfo application = {0};
    application.sType=VK_STRUCTURE_TYPE_APPLICATION_INFO;
    application.pApplicationName="VeloWin WSI Probe";
    application.apiVersion=VK_API_VERSION_1_0;
    VkInstanceCreateInfo create = {0};
    create.sType=VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    create.pApplicationInfo=&application;
    create.enabledExtensionCount=2;
    create.ppEnabledExtensionNames=required;
    code=vkCreateInstance(&create,NULL,&instance);
    if (code!=VK_SUCCESS) {
        snprintf(report,sizeof(report),"Falha vkCreateInstance WSI (%d).",(int)code);
        goto finish;
    }
    VkAndroidSurfaceCreateInfoKHR surfaceInfo = {0};
    surfaceInfo.sType=VK_STRUCTURE_TYPE_ANDROID_SURFACE_CREATE_INFO_KHR;
    surfaceInfo.window=window;
    code=vkCreateAndroidSurfaceKHR(instance,&surfaceInfo,NULL,&surface);
    if (code!=VK_SUCCESS) {
        snprintf(report,sizeof(report),"Falha ao criar superfície Vulkan Android (%d).",(int)code);
        goto finish;
    }
    uint32_t gpuCount=0;
    code=vkEnumeratePhysicalDevices(instance,&gpuCount,NULL);
    if (code!=VK_SUCCESS || gpuCount==0) {
        snprintf(report,sizeof(report),"Nenhuma GPU Vulkan disponível para Surface (%d).",(int)code);
        goto finish;
    }
    if (gpuCount>16)gpuCount=16;
    VkPhysicalDevice gpus[16]={0};
    code=vkEnumeratePhysicalDevices(instance,&gpuCount,gpus);
    if (code!=VK_SUCCESS) {
        snprintf(report,sizeof(report),"Falha enumerando GPU Vulkan (%d).",(int)code);
        goto finish;
    }
    int usable=0;
    for (uint32_t i=0; i<gpuCount && !usable; ++i) {
        VkPhysicalDeviceProperties props;
        vkGetPhysicalDeviceProperties(gpus[i],&props);
        uint32_t familyCount=0;
        vkGetPhysicalDeviceQueueFamilyProperties(gpus[i],&familyCount,NULL);
        if (familyCount==0 || familyCount>128)continue;
        VkQueueFamilyProperties families[128];
        vkGetPhysicalDeviceQueueFamilyProperties(gpus[i],&familyCount,families);
        int chosenFamily=-1;
        for (uint32_t j=0; j<familyCount; ++j) {
            VkBool32 present=VK_FALSE;
            code=vkGetPhysicalDeviceSurfaceSupportKHR(gpus[i],j,surface,&present);
            if (code==VK_SUCCESS && present && families[j].queueCount
                && (families[j].queueFlags&VK_QUEUE_GRAPHICS_BIT)) {
                chosenFamily=(int)j;
                break;
            }
        }
        if (chosenFamily<0)continue;
        uint32_t deviceExtensions=0;
        int hasSwapchain=0;
        if (vkEnumerateDeviceExtensionProperties(gpus[i],NULL,&deviceExtensions,NULL)==VK_SUCCESS
            && deviceExtensions<=1024) {
            VkExtensionProperties ext[1024];
            if (deviceExtensions &&
                vkEnumerateDeviceExtensionProperties(gpus[i],NULL,&deviceExtensions,ext)==VK_SUCCESS) {
                for (uint32_t j=0;j<deviceExtensions;++j)
                    if(!strcmp(ext[j].extensionName,VK_KHR_SWAPCHAIN_EXTENSION_NAME))hasSwapchain=1;
            }
        }
        uint32_t formats=0, modes=0;
        VkSurfaceCapabilitiesKHR caps={0};
        VkResult capCode=vkGetPhysicalDeviceSurfaceCapabilitiesKHR(gpus[i],surface,&caps);
        VkResult formatCode=vkGetPhysicalDeviceSurfaceFormatsKHR(gpus[i],surface,&formats,NULL);
        VkResult modeCode=vkGetPhysicalDeviceSurfacePresentModesKHR(gpus[i],surface,&modes,NULL);
        usable=capCode==VK_SUCCESS && formatCode==VK_SUCCESS && modeCode==VK_SUCCESS
            && hasSwapchain && formats>0 && modes>0;
        snprintf(report,sizeof(report),
            "GPU: %.160s\nVulkan WSI: %s\nFila gráfica+present: %d\n"
            "VK_KHR_android_surface: sim\nVK_KHR_swapchain: %s\n"
            "Formatos de superfície: %u\nModos de apresentação: %u\n"
            "Extent atual: %ux%u\nImagens swapchain: %u–%u\n"
            "Resultado: %s\n\n"
            "Teste de superfície, não de swapchain criada, renderização, DXVK ou Wine.",
            props.deviceName,usable?"capacidade detectada":"requisitos incompletos",
            chosenFamily,hasSwapchain?"sim":"não",formats,modes,
            caps.currentExtent.width,caps.currentExtent.height,
            caps.minImageCount,caps.maxImageCount,
            usable?"pode avançar para teste de apresentação":"renderização não habilitada");
    }
    if (!usable) {
        snprintf(report,sizeof(report),
            "Nenhuma GPU apresentou fila gráfica com suporte de apresentação "
            "e VK_KHR_swapchain para esta Surface Android.");
    }
finish:
    if (surface!=VK_NULL_HANDLE) vkDestroySurfaceKHR(instance,surface,NULL);
    if (instance!=VK_NULL_HANDLE) vkDestroyInstance(instance,NULL);
    ANativeWindow_release(window);
    return result(env,report);
}

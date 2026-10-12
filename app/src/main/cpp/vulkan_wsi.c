/*
 * Vulkan WSI proof-of-life on an Android Surface. No Windows window server,
 * game renderer, DXVK or VKD3D. Creates and destroys an actual Vulkan swapchain
 * without drawing or presenting, against an Android SurfaceView.
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
        int capsReady=capCode==VK_SUCCESS && formatCode==VK_SUCCESS
            && modeCode==VK_SUCCESS && hasSwapchain && formats>0 && modes>0;
        int created=0;
        int imagesAvailable=0;
        VkResult creationCode=VK_ERROR_INITIALIZATION_FAILED;
        if(capsReady && (caps.supportedUsageFlags & VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)) {
            float priority=1.0f;
            VkDeviceQueueCreateInfo queueInfo={0};
            queueInfo.sType=VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
            queueInfo.queueFamilyIndex=(uint32_t)chosenFamily;
            queueInfo.queueCount=1;
            queueInfo.pQueuePriorities=&priority;
            const char *deviceExt[]={VK_KHR_SWAPCHAIN_EXTENSION_NAME};
            VkDeviceCreateInfo deviceInfo={0};
            deviceInfo.sType=VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
            deviceInfo.queueCreateInfoCount=1;
            deviceInfo.pQueueCreateInfos=&queueInfo;
            deviceInfo.enabledExtensionCount=1;
            deviceInfo.ppEnabledExtensionNames=deviceExt;
            VkDevice device=VK_NULL_HANDLE;
            creationCode=vkCreateDevice(gpus[i],&deviceInfo,NULL,&device);
            if(creationCode==VK_SUCCESS) {
                VkSurfaceFormatKHR supportedFormats[32];
                uint32_t countFormats=formats>32?32:formats;
                VkResult fetchFormat=vkGetPhysicalDeviceSurfaceFormatsKHR(gpus[i],surface,
                    &countFormats,supportedFormats);
                if((fetchFormat==VK_SUCCESS || fetchFormat==VK_INCOMPLETE)
                    && countFormats>0) {
                    VkExtent2D extent=caps.currentExtent;
                    if(extent.width==UINT32_MAX) {
                        int32_t width=ANativeWindow_getWidth(window);
                        int32_t height=ANativeWindow_getHeight(window);
                        extent.width=width>0?(uint32_t)width:1u;
                        extent.height=height>0?(uint32_t)height:1u;
                        if(extent.width<caps.minImageExtent.width)
                            extent.width=caps.minImageExtent.width;
                        if(extent.height<caps.minImageExtent.height)
                            extent.height=caps.minImageExtent.height;
                        if(extent.width>caps.maxImageExtent.width)
                            extent.width=caps.maxImageExtent.width;
                        if(extent.height>caps.maxImageExtent.height)
                            extent.height=caps.maxImageExtent.height;
                    }
                    uint32_t images=caps.minImageCount+1;
                    if(caps.maxImageCount && images>caps.maxImageCount)
                        images=caps.maxImageCount;
                    VkCompositeAlphaFlagBitsKHR alpha=VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
                    const VkCompositeAlphaFlagBitsKHR choices[]={
                        VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR,
                        VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR,
                        VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR,
                        VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR
                    };
                    int supportedAlpha=0;
                    for(unsigned a=0;a<4;++a) {
                        if(caps.supportedCompositeAlpha & choices[a]) {
                            alpha=choices[a];
                            supportedAlpha=1;
                            break;
                        }
                    }
                    if(supportedAlpha && extent.width>0 && extent.height>0
                       && images>=caps.minImageCount) {
                        VkSwapchainCreateInfoKHR sc={0};
                        sc.sType=VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
                        sc.surface=surface;
                        sc.minImageCount=images;
                        sc.imageFormat=supportedFormats[0].format;
                        sc.imageColorSpace=supportedFormats[0].colorSpace;
                        sc.imageExtent=extent;
                        sc.imageArrayLayers=1;
                        sc.imageUsage=VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
                        sc.imageSharingMode=VK_SHARING_MODE_EXCLUSIVE;
                        sc.preTransform=caps.currentTransform;
                        sc.compositeAlpha=alpha;
                        sc.presentMode=VK_PRESENT_MODE_FIFO_KHR;
                        sc.clipped=VK_TRUE;
                        VkSwapchainKHR swapchain=VK_NULL_HANDLE;
                        creationCode=vkCreateSwapchainKHR(device,&sc,NULL,&swapchain);
                        if(creationCode==VK_SUCCESS) {
                            created=1;
                            uint32_t available=0;
                            if(vkGetSwapchainImagesKHR(device,swapchain,&available,NULL)
                                ==VK_SUCCESS && available>0)imagesAvailable=(int)available;
                            vkDestroySwapchainKHR(device,swapchain,NULL);
                        }
                    }
                }
                vkDestroyDevice(device,NULL);
            }
        }
        usable=capsReady && created && imagesAvailable>0;
        snprintf(report,sizeof(report),
            "GPU: %.160s\nVulkan WSI: %s\nFila gráfica+present: %d\n"
            "VK_KHR_android_surface: sim\nVK_KHR_swapchain: %s\n"
            "Formatos de superfície: %u\nModos de apresentação: %u\n"
            "Extent atual: %ux%u\nImagens swapchain: %d\n"
            "Criação VkSwapchainKHR: %s (VkResult %d)\n"
            "Resultado: %s\n\n"
            "Smoke test real de criação e destruição de swapchain na Surface Android. "
            "Ainda NÃO apresenta quadros, executa Wine, DXVK ou jogos.",
            props.deviceName,capsReady?"suporte indicado":"requisitos incompletos",
            chosenFamily,hasSwapchain?"sim":"não",formats,modes,
            caps.currentExtent.width,caps.currentExtent.height,imagesAvailable,
            created?"sucesso":"falhou",(int)creationCode,
            usable?"swapchain criada e destruída":"driver/WSI ainda não validado");
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

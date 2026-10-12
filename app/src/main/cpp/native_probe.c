#include <jni.h>
#include <vulkan/vulkan.h>
#include <stdio.h>
#include <string.h>

JNIEXPORT jstring JNICALL
Java_dev_velowin_app_hardware_NativeCapabilities_probeVulkan(JNIEnv* env, jclass clazz) {
    (void)clazz;
    VkApplicationInfo app = {0};
    app.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    app.pApplicationName = "VeloWin";
    app.apiVersion = VK_API_VERSION_1_0;
    VkInstanceCreateInfo info = {0};
    info.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    info.pApplicationInfo = &app;
    VkInstance instance = VK_NULL_HANDLE;
    VkResult rc = vkCreateInstance(&info, NULL, &instance);
    char report[1400];
    if (rc != VK_SUCCESS) {
        snprintf(report, sizeof(report), "Vulkan: vkCreateInstance falhou: %d.", (int)rc);
        return (*env)->NewStringUTF(env, report);
    }
    uint32_t count = 0;
    rc = vkEnumeratePhysicalDevices(instance, &count, NULL);
    if (rc != VK_SUCCESS || count == 0) {
        snprintf(report, sizeof(report), "Vulkan: dispositivo indisponível (%d).", (int)rc);
        vkDestroyInstance(instance, NULL);
        return (*env)->NewStringUTF(env, report);
    }
    VkPhysicalDevice devices[8] = {0};
    if (count > 8) count=8;
    rc = vkEnumeratePhysicalDevices(instance, &count, devices);
    if (rc != VK_SUCCESS) {
        vkDestroyInstance(instance, NULL);
        return (*env)->NewStringUTF(env, "Falha ao enumerar GPUs Vulkan.");
    }
    VkPhysicalDeviceProperties properties;
    VkPhysicalDeviceFeatures features;
    vkGetPhysicalDeviceProperties(devices[0], &properties);
    vkGetPhysicalDeviceFeatures(devices[0], &features);
    uint32_t extensionCount=0;
    int swapchain=0, timeline=0, budget=0;
    if (vkEnumerateDeviceExtensionProperties(devices[0], NULL, &extensionCount, NULL) == VK_SUCCESS
        && extensionCount && extensionCount <= 512) {
        VkExtensionProperties extensions[512];
        if (vkEnumerateDeviceExtensionProperties(devices[0], NULL, &extensionCount, extensions)
            == VK_SUCCESS) {
            for (uint32_t i=0; i<extensionCount; i++) {
                if (!strcmp(extensions[i].extensionName, "VK_KHR_swapchain")) swapchain=1;
                if (!strcmp(extensions[i].extensionName, "VK_KHR_timeline_semaphore")) timeline=1;
                if (!strcmp(extensions[i].extensionName, "VK_EXT_memory_budget")) budget=1;
            }
        }
    }
    /* Active Android Vulkan probe: create a logical device and queue without
     * presenting or modifying system GPU state. This does NOT test DXVK/WSI. */
    int logicalDevice=0, commandPool=0;
    VkResult deviceCode=VK_ERROR_INITIALIZATION_FAILED;
    uint32_t queues=0;
    vkGetPhysicalDeviceQueueFamilyProperties(devices[0], &queues, NULL);
    if(queues>0 && queues<=64) {
        VkQueueFamilyProperties families[64];
        vkGetPhysicalDeviceQueueFamilyProperties(devices[0],&queues,families);
        uint32_t family=queues;
        for(uint32_t i=0;i<queues;i++){
            if(families[i].queueCount && (families[i].queueFlags&VK_QUEUE_GRAPHICS_BIT)){
                family=i;break;
            }
        }
        if(family<queues) {
            float priority=1.0f;
            VkDeviceQueueCreateInfo qi={0};
            qi.sType=VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
            qi.queueFamilyIndex=family;
            qi.queueCount=1;
            qi.pQueuePriorities=&priority;
            VkDeviceCreateInfo di={0};
            di.sType=VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
            di.queueCreateInfoCount=1;
            di.pQueueCreateInfos=&qi;
            VkDevice device=VK_NULL_HANDLE;
            deviceCode=vkCreateDevice(devices[0],&di,NULL,&device);
            if(deviceCode==VK_SUCCESS) {
                VkQueue queue=VK_NULL_HANDLE;
                vkGetDeviceQueue(device,family,0,&queue);
                logicalDevice=(queue!=VK_NULL_HANDLE);
                VkCommandPoolCreateInfo poolInfo={0};
                poolInfo.sType=VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
                poolInfo.queueFamilyIndex=family;
                poolInfo.flags=VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
                VkCommandPool pool=VK_NULL_HANDLE;
                VkResult poolCode=vkCreateCommandPool(device,&poolInfo,NULL,&pool);
                if(poolCode==VK_SUCCESS){
                    commandPool=1;
                    vkDestroyCommandPool(device,pool,NULL);
                }
                vkDestroyDevice(device,NULL);
            }
        }
    }
    snprintf(report, sizeof(report),
        "GPU: %.190s\nVulkan API: %u.%u.%u\nVendor ID: 0x%04x\n"
        "maxImageDimension2D: %u\n"
        "Vulkan logical device: %s (VkResult %d)\nCommand pool: %s\n"
        "VK_KHR_swapchain: %s\nVK_KHR_timeline_semaphore: %s\n"
        "VK_EXT_memory_budget: %s\nSampler anisotropy: %s\n"
        "DXVK e VKD3D exigem análise adicional de extensões e rendering.",
        properties.deviceName,
        VK_VERSION_MAJOR(properties.apiVersion), VK_VERSION_MINOR(properties.apiVersion),
        VK_VERSION_PATCH(properties.apiVersion), properties.vendorID,
        properties.limits.maxImageDimension2D,
        logicalDevice?"criado":"indisponível",(int)deviceCode,
        commandPool?"criado":"indisponível",
        swapchain ? "sim" : "não", timeline ? "sim" : "não",
        budget ? "sim" : "não", features.samplerAnisotropy ? "sim" : "não");
    vkDestroyInstance(instance, NULL);
    return (*env)->NewStringUTF(env, report);
}

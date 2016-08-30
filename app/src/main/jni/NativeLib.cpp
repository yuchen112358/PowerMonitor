//
// Created by yuchen on 16-7-2.
//
//
// Created by yuchen on 16-7-2.
//
/**
 * 参数JNIEnv *env在.cpp文件和.c文件中的用法不同：
 * .cpp文件中的`(*env).NewStringUTF("write error!");`
 * 等价于.c文件中的`(*env)->NewStringUTF(env,"write error!");`
 */
#include "io_github_wzzju_powermonitor_NativeLib.h"
#include <stdio.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <memory.h>
#include <string.h>

#define GPUFREQ_NODE    "/sys/devices/11800000.mali/clock"
#define TEMP_NODE       "/sys/devices/10060000.tmu/temp"

typedef struct ina231_iocreg__t {
    char name[20];
    unsigned int enable;
    unsigned int cur_uV;
    unsigned int cur_uA;
    unsigned int cur_uW;
} ina231_iocreg_t;

typedef struct sensor__t {
    int fd;
    ina231_iocreg_t data;
} sensor_t;

#define INA231_IOCGREG      _IOR('i', 1, ina231_iocreg_t *)
#define INA231_IOCSSTATUS   _IOW('i', 2, ina231_iocreg_t *)
#define INA231_IOCGSTATUS   _IOR('i', 3, ina231_iocreg_t *)

#define DEV_SENSOR_ARM  "/dev/sensor_arm"
#define DEV_SENSOR_MEM  "/dev/sensor_mem"
#define DEV_SENSOR_KFC  "/dev/sensor_kfc"
#define DEV_SENSOR_G3D  "/dev/sensor_g3d"

enum {
    SENSOR_ARM = 0,
    SENSOR_MEM,
    SENSOR_KFC,
    SENSOR_G3D,
    SENSOR_MAX
};

char cpu_node_list[8][100];
float armuV, armuA, armuW;
float g3duV, g3duA, g3duW;
float kfcuV, kfcuA, kfcuW;
float memuV, memuA, memuW;
sensor_t sensor[SENSOR_MAX];


int open_sensor(const char *node, sensor_t *sensor) {
    if ((sensor->fd = open(node, O_RDWR)) < 0)
        return -1;
    return sensor->fd;
}

int read_sensor_status(sensor_t *sensor) {
    if (sensor->fd > 0) {
        if (ioctl(sensor->fd, INA231_IOCGSTATUS, &sensor->data) < 0)
            return -1;
    }
    return 0;
}

void enable_sensor(sensor_t *sensor, unsigned char enable) {
    if (sensor->fd > 0) {
        sensor->data.enable = enable ? 1 : 0;
        if (ioctl(sensor->fd, INA231_IOCSSTATUS, &sensor->data) < 0)
            return;
    }
}

void close_sensor(sensor_t *sensor)
{
    if (sensor->fd > 0)
        close(sensor->fd);
}

void read_sensor(sensor_t *sensor)
{
    if (sensor->fd > 0) {
        if (ioctl(sensor->fd, INA231_IOCGREG, &sensor->data) < 0)
            return;
    }
}

JNIEXPORT void JNICALL Java_io_github_wzzju_powermonitor_NativeLib_Init
        (JNIEnv *env, jclass cls) {
    int i;
    char temp[100];
    for (i = 0; i < 8; i++) {
        sprintf(temp, "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_cur_freq", i);
        ///sys/devices/system/cpu/cpu0d/cpufreq/cpuinfo_cur_freq
        strcpy(cpu_node_list[i], temp);
    }
}
//得到的GPU频率单位是MHz
JNIEXPORT jstring JNICALL Java_io_github_wzzju_powermonitor_NativeLib_GetGPUCurFreq
        (JNIEnv *env, jclass cls) {
    int fd, fr;

    char freq[20];
    memset(freq, '\0', 20);

    fd = open("/storage/sdcard0/GPUFreq.csv", O_WRONLY | O_APPEND | O_CREAT,
              S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
    if (fd == -1) {
        return (*env).NewStringUTF("write error!");
    }
    fr = open(GPUFREQ_NODE, O_RDONLY);
    if (fr == -1) {
        return (*env).NewStringUTF("read error!");
    }
    read(fr, freq, sizeof(freq));
    write(fd, freq, sizeof(freq));
    close(fd);
    close(fr);
    return (*env).NewStringUTF(freq);
}

//得到的CPU频率单位是KHz
JNIEXPORT jstring JNICALL Java_io_github_wzzju_powermonitor_NativeLib_GetCPUCurFreq
        (JNIEnv *env, jclass cls, jint num) {
    int fd, fr;

    char freq[20];
    memset(freq, '\0', 20);
    char fileP[50];
    sprintf(fileP, "/storage/sdcard0/CPU%dFreq.csv", num);
    fd = open(fileP, O_WRONLY | O_APPEND | O_CREAT, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
    if (fd == -1) {
        return (*env).NewStringUTF("write error!");
    }
    fr = open(cpu_node_list[num], O_RDONLY);
    if (fr == -1) {
        return (*env).NewStringUTF("read error!");
    }
    read(fr, freq, sizeof(freq));
    write(fd, freq, sizeof(freq));
    close(fd);
    close(fr);
    return (*env).NewStringUTF(freq);
}


JNIEXPORT jstring JNICALL Java_io_github_wzzju_powermonitor_NativeLib_GetCPUCurLoad
  (JNIEnv *env, jclass cls, jint cpu){
	int fd, fr;

	char fileP[60];

	char load[40];

	const char delim[] = "\n";
	const char delimCPU[] = ",";
	char *token, *cur = load;
	char *target[8];
	char result[5];
	int index ;
	memset(load, '\0', 40);
	memset(result, '\0', 5);
	sprintf(fileP, "/storage/sdcard0/CPU%dLoad.csv", cpu);
	fd = open(fileP, O_WRONLY | O_APPEND | O_CREAT, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
	if (fd == -1) {
		return (*env).NewStringUTF("0");
	}
	fr = open("/proc/cpu_load_dir/cpu_load", O_RDONLY);
	if (fr == -1) {
		return (*env).NewStringUTF("0");
	}
	read(fr, load, sizeof(load));
	load[40] = '\0';
	token = strsep(&cur, delim);
	cur = token;
	index = 0;
	while ((token = strsep(&cur, delimCPU)) && (index < 8)) {
		target[index] = token;
		index++;
	}
	strcpy(result,target[cpu]);
	strcat(result,"\n");
	write(fd, result, sizeof(result));
	close(fd);
	close(fr);
    return (*env).NewStringUTF(target[cpu]);
  }

JNIEXPORT jstring JNICALL Java_io_github_wzzju_powermonitor_NativeLib_GetAllCPUCurLoad
    (JNIEnv *env, jclass cls){
    	int fd, fr;

    	char fileP[] = "/storage/sdcard0/AllCPULoad.csv";

    	char load[100];
    	/*//只记录频率的方法所需的变量
    	const char delim[] = "\n";
    	char *token, *cur = load;
    	*/

    	memset(load, '\0', 100);

    	fd = open(fileP, O_WRONLY | O_APPEND | O_CREAT, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
    	if (fd == -1) {
    		return (*env).NewStringUTF("0");
    	}
    	fr = open("/proc/cpu_load_dir/cpu_load", O_RDONLY);
    	if (fr == -1) {
    		return (*env).NewStringUTF("0");
    	}
    	read(fr, load, sizeof(load));
    	/*//只记录频率的方法
    	token = strsep(&cur, delim);
    	write(fd, token, strlen(token));//记录负载值
    	write(fd, delim, sizeof(delim));
    	*/
    	write(fd, load, sizeof(load));
    	close(fd);
    	close(fr);
        return (*env).NewStringUTF(load);
    }

JNIEXPORT jstring JNICALL Java_io_github_wzzju_powermonitor_NativeLib_GetCPUTemp
        (JNIEnv *env, jclass cls, jint num) {
    int fd, fr;

    char fileP[50];
    char buf[16];
    int id = 0;
    if(num<4){
        id = num + 4;
        sprintf(fileP, "/storage/sdcard0/CPU%dTemp.csv", id);
    }else {
        strcpy(fileP, "/storage/sdcard0/GPUTemp.csv");
    }
    fd = open(fileP, O_WRONLY | O_APPEND | O_CREAT, S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
    if (fd == -1) {
        return (*env).NewStringUTF("write error!");
    }
    fr = open(TEMP_NODE, O_RDONLY);
    if (fr == -1) {
        return (*env).NewStringUTF("read error!");
    }
    for (int i = 0; i < num + 1; i++)
        read(fr, buf, 16);
    buf[12] = '\0';
    write(fd, &buf[9], sizeof(&buf[9]));
    close(fd);
    close(fr);
    return (*env).NewStringUTF(&buf[9]);
}


JNIEXPORT jint JNICALL Java_io_github_wzzju_powermonitor_NativeLib_OpenINA231
        (JNIEnv *env, jclass cls) {
    if (open_sensor(DEV_SENSOR_ARM, &sensor[SENSOR_ARM]) < 0)
        return -1;
    if (open_sensor(DEV_SENSOR_MEM, &sensor[SENSOR_MEM]) < 0)
        return -1;
    if (open_sensor(DEV_SENSOR_KFC, &sensor[SENSOR_KFC]) < 0)
        return -1;
    if (open_sensor(DEV_SENSOR_G3D, &sensor[SENSOR_G3D]) < 0)
        return -1;

    if (read_sensor_status(&sensor[SENSOR_ARM]))
        return -1;
    if (read_sensor_status(&sensor[SENSOR_MEM]))
        return -1;
    if (read_sensor_status(&sensor[SENSOR_KFC]))
        return -1;
    if (read_sensor_status(&sensor[SENSOR_G3D]))
        return -1;

    if (!sensor[SENSOR_ARM].data.enable)
        enable_sensor(&sensor[SENSOR_ARM], 1);
    if (!sensor[SENSOR_MEM].data.enable)
        enable_sensor(&sensor[SENSOR_MEM], 1);
    if (!sensor[SENSOR_KFC].data.enable)
        enable_sensor(&sensor[SENSOR_KFC], 1);
    if (!sensor[SENSOR_G3D].data.enable)
        enable_sensor(&sensor[SENSOR_G3D], 1);

    return 0;
}

JNIEXPORT void JNICALL Java_io_github_wzzju_powermonitor_NativeLib_CloseINA231
        (JNIEnv *env, jclass cls) {
    if (sensor[SENSOR_ARM].data.enable)
        enable_sensor(&sensor[SENSOR_ARM], 0);
    if (sensor[SENSOR_MEM].data.enable)
        enable_sensor(&sensor[SENSOR_MEM], 0);
    if (sensor[SENSOR_KFC].data.enable)
        enable_sensor(&sensor[SENSOR_KFC], 0);
    if (sensor[SENSOR_G3D].data.enable)
        enable_sensor(&sensor[SENSOR_G3D], 0);

    close_sensor(&sensor[SENSOR_ARM]);
    close_sensor(&sensor[SENSOR_MEM]);
    close_sensor(&sensor[SENSOR_KFC]);
    close_sensor(&sensor[SENSOR_G3D]);
}


JNIEXPORT jstring JNICALL Java_io_github_wzzju_powermonitor_NativeLib_GetINA231
        (JNIEnv *env, jclass cls) {
    int fd;
    char res[80];

    read_sensor(&sensor[SENSOR_ARM]);
    read_sensor(&sensor[SENSOR_MEM]);
    read_sensor(&sensor[SENSOR_KFC]);
    read_sensor(&sensor[SENSOR_G3D]);
    // A15
    armuV = (float)(sensor[SENSOR_ARM].data.cur_uV / 100000) / 10;
    armuA = (float)(sensor[SENSOR_ARM].data.cur_uA / 1000) / 1000;
    armuW = (float)(sensor[SENSOR_ARM].data.cur_uW / 1000) / 1000;
    //Memory
    memuV = (float)(sensor[SENSOR_MEM].data.cur_uV / 100000) / 10;
    memuA = (float)(sensor[SENSOR_MEM].data.cur_uA / 1000) / 1000;
    memuW = (float)(sensor[SENSOR_MEM].data.cur_uW / 1000) / 1000;
    //A7
    kfcuV = (float)(sensor[SENSOR_KFC].data.cur_uV / 100000) / 10;
    kfcuA = (float)(sensor[SENSOR_KFC].data.cur_uA / 1000) / 1000;
    kfcuW = (float)(sensor[SENSOR_KFC].data.cur_uW / 1000) / 1000;
    //GPU
    g3duV = (float)(sensor[SENSOR_G3D].data.cur_uV / 100000) / 10;
    g3duA = (float)(sensor[SENSOR_G3D].data.cur_uA / 1000) / 1000;
    g3duW = (float)(sensor[SENSOR_G3D].data.cur_uW / 1000) / 1000;

    memset(res, '\0', 80);

    fd = open("/storage/sdcard0/Power.csv", O_WRONLY | O_APPEND | O_CREAT,
              S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH);
    if (fd == -1) {
       return (*env).NewStringUTF("write error");
    }
    //res:="a15V,a15A,a15W,a7V,a7A,a7W,gpuV,gpuA,gpuW,memV,memA,memW\n"
    sprintf(res,"%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f\n",
    armuV,armuA,armuW,kfcuV,kfcuA,kfcuW,
    g3duV,g3duA,g3duW,memuV,memuA,memuW);
    write(fd, res, sizeof(res));
    close(fd);
    return (*env).NewStringUTF(res);
}

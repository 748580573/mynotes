# java生成echart图

## 需要用到的组件

| 名称            | 描述                                                         |
| --------------- | ------------------------------------------------------------ |
| phantomjs       | 官方介绍：PhantomJS是一个基于 WebKit 的服务器端JavaScript API。它全面支持web而不需浏览器支持，支持各种Web标准：DOM处理，CSS选择器, JSON，Canvas，和SVG。 PhantomJS常用于页面自动化，网络监测，网页截屏，以及无界面测试等。 |
| echarts-convert | 顾名思义将输入的数据解析转换为echart图                       |

## 使用phantomjs与echarts-convert生成echart图


````shell
#命令格式 phantomjs.exe路径   echarts-convert.js路径 -infile option.json文件路径 -outfile  chart图文件路径
# option.json就是echart图需要传入的options
phantomjs.exe  echarts-convert.js -infile  option.json  -outfile  echart.png 
````



## 使用java生成echart图

使用java生成echart图，本质上就是调用上面的命令生成echart图。在本文同路径下`java生成ehcart图.zip`文件下有 phantomjs.exe、 echarts-convert.js

````java
import com.heng.echart.options.LineEchar;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class EcharApiTest {

    public static String projectFilePath;

    public static String classPath = new File(EcharApiTest.class.getResource("/").getPath()).getPath();

    public static String phantomjs =  classPath + File.separator + "phantomjs" + File.separator + "bin" + File.separator + "phantomjs.exe";

    public static String JSpath = classPath + File.separator + "echarts-convert" + File.separator + "echarts-convert.js";
    static {
        try {
            projectFilePath = new File("").getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test() throws IOException {
        List<String> xData = new ArrayList<>();
        String[] xString = new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (String str : xString) {
            xData.add(str);
        }

        List<Integer> seriesData = new ArrayList<>();
        Random random = new Random(1000);
        for (int i = 0; i < 7; i++) {
            seriesData.add(random.nextInt(1000+i));
        }
        LineEchar lineEchar = new LineEchar(xData, seriesData);
        File tempFile = createOptionFile("bbb");
        writeOptionData(tempFile,lineEchar.obationOptions());
        generateEcharts(phantomjs,JSpath,tempFile.getPath(),"E:/tmp/" + UUID.randomUUID() + ".png");
    }

    public File createOptionFile(String fileName) {
        File dir = new File(projectFilePath);
        File tempFile = null;
        try {
            tempFile = File.createTempFile(fileName, ".json", dir);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
//            tempFile.deleteOnExit();
        }
        return tempFile;
    }

    public boolean writeOptionData(File file,String option){
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
            bw.write(option);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void generateEcharts(String phantomPath,String convertJsPath,String optionFilePath,String echartPath) throws IOException {
        String blank = " ";
        String arg1 = " -infile ";
        String arg2 = " -outfile ";
        String cmd = phantomPath + blank + convertJsPath + arg1 + optionFilePath + arg2 + echartPath;

        System.out.println(cmd);
        Process process = Runtime.getRuntime().exec(cmd);
    }
}

````


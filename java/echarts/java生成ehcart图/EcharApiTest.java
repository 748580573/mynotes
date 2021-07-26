package com.hneg.test;

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

package com.leonsoft.research.propsConverter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

public class Json2PojoMainProgram {

    public static void main(String[] args) throws IOException {
        System.out.println("Start....");


        String sourceJsonFileName = "C:\\wks\\leonbookinsKarpathos\\src\\main\\resources\\props\\source\\eReq.json";

        String target = "C:\\wks\\leonbookinsKarpathos\\src\\main\\resources\\props\\target\\out.properties";
        String targetSecure = "C:\\wks\\leonbookinsKarpathos\\src\\main\\resources\\props\\target\\out-secure.properties";


        Path path = Paths.get(sourceJsonFileName);                         // load input
        String json = Files.readString(path, Charset.defaultCharset());
      //  System.out.println("**********  INPUT ********");
      //  System.out.println(json);

        // read the jsio  into java pojos


       ObjectMapper om =
                new ObjectMapper()
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


        JsonData[] temp = om.readValue(json, JsonData[].class);
        List<JsonData> jsonArrayData = Arrays.asList(temp);

       int  countOriginal  =jsonArrayData.size();

        // System.out.println(jsonArrayData);

        // load  Props Object with the vales from the json ojos
        Properties props = new Properties();

//        System.out.println();
        for (JsonData num : jsonArrayData) {
            props.put(num.getSYP_NAME_NM(), num.getSYP_VALUE_NM());
        }
        System.out.println("**********  OUTPUT Prop file   >> see file "+  target   );
        props.store(new FileOutputStream(new File(target)), "");

//
//
//        int  count = 0;
//        int  textCount = 0;
//        int  sceCount = 0;
//        try {
//            FileWriter myWriter = new FileWriter(target);
//            FileWriter targetSecureWritter = new FileWriter(targetSecure);
//
//            for (JsonData num : jsonArrayData) {
//                String name = num.getSYP_NAME_NM();
//                String val = num.getSYP_VALUE_NM();
//
//
////                myWriter.write(name + "=" + val  + "\n" );
//
//                if ( StringUtils.containsAnyIgnoreCase(name ,"password", "clientsecret") ||
//                StringUtils.startsWith(val ,"{5AES256}")
//
//                ){
//                    targetSecureWritter.write(name + "=" + val  + "\n" );
//                    sceCount++;
//                }else {
//                    myWriter.write(name + "=" + val  + "\n" );
//                    textCount++;
//                }
//
//                count++;
//            }
//
//            myWriter.close();
//            targetSecureWritter.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        System.out.println("Finished     ************************"  );
//        System.out.println("Source       total count "  + countOriginal);
//
//        System.out.println("Target       textCount "  + textCount);
//        System.out.println("Target       sceCount "  + sceCount);
//        System.out.println("Target total count "  + count);


    }

}

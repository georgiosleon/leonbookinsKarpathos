package com.leonsoft.research.pdf;

//package eu.europa.ec.rdg.compass.processes.pm.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfReader;
import org.springframework.core.io.ClassPathResource;

//import eu.europa.ec.rdg.compass.processes.pm.api.exceptions.PMException;

public class PMPdfUtils {


    public static String getDocumentText(InputStream pdfIs) {

        PDDocument doc = null;
        try {
            doc = PDDocument.load(pdfIs);
            String content = new PDFTextStripper().getText(doc);
            content = content.replaceAll("[ |\\n|\\r]+", "");
            return content;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (doc != null) {
                try {
                    doc.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static boolean doesContainPlaceHolder(InputStream pdfIs, String placeHolderName) {
        String pdfText = getDocumentText(pdfIs);
        return pdfText.contains("TGSMark#"+placeHolderName);
    }

    public static boolean placeholderContained(InputStream pdfIs, String placeHolderName) {
        PdfReader reader=null;
        try {
            reader = new PdfReader(pdfIs);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        AcroFields fields = reader.getAcroFields();
        boolean hasFormFields = (fields != null && fields.getFields() != null && fields.getFields().size() > 0);
        if (hasFormFields) {
            for (String acrofieldKey:fields.getFields().keySet()){
                if (acrofieldKey.equals(placeHolderName)) {
                    return true;
                }
            }
        }
        return false;
    }


    private static InputStream getInputStreamFromFullFilePath(String fullFilePath) throws FileNotFoundException {
        String log = "readFile(fullFilePath"+fullFilePath+")";
        File f = new File(fullFilePath);
        return new FileInputStream(f);
    }

    public static void main(String[] args) throws IOException {


//        ClassPathResource resourceSuccess = new ClassPathResource("/pdf/EN+-+Award+notification+-+successful+tenderer 1.pdf");
//        System.out.println("file1 contains placeholder : " +PMPdfUtils.doesContainPlaceHolder(resourceSuccess.getInputStream(), "acrofield-authorising-officer"));
//        ClassPathResource resourceSFail = new ClassPathResource("/pdf/EN+-+Award+notification+-+unsuccessful+tenderer 1.pdf");
//        System.out.println("file2 contains placeholder : " +PMPdfUtils.doesContainPlaceHolder(resourceSFail.getInputStream(), "acrofield-authorising-officer"));
//
//
//        String file1 = "C:\\wks\\leonbookinsKarpathos\\src\\main\\resources\\pdf\\EN+-+Award+notification+-+successful+tenderer 1.pdf";
//        System.out.println("file1 contains placeholder : " +PMPdfUtils.doesContainPlaceHolder(PMPdfUtils.getInputStreamFromFullFilePath(file1), "acrofield-authorising-officer"));
//        String file2 = "C:\\wks\\leonbookinsKarpathos\\src\\main\\resources\\pdf\\EN+-+Award+notification+-+unsuccessful+tenderer 1.pdf";
//        System.out.println("file2 contains placeholder : " +PMPdfUtils.doesContainPlaceHolder(PMPdfUtils.getInputStreamFromFullFilePath(file2), "acrofield-authorising-officer"));




//        String f1 = "C:\\wks\\leonbookinsKarpathos\\src\\main\\resources\\pdf\\EN+-+Award+notification+-+successful+tenderer 1.pdf";
//        System.out.println("file1 contains placeholder : " +PMPdfUtils.placeholderContained(getInputStreamFromFullFilePath(f1), "acrofield-authorising-officer"));
//        String f2 = "C:\\wks\\leonbookinsKarpathos\\src\\main\\resources\\pdf\\EN+-+Award+notification+-+unsuccessful+tenderer 1.pdf";
//        System.out.println("file2 contains placeholder : " +PMPdfUtils.placeholderContained(getInputStreamFromFullFilePath(f2), "acrofield-authorising-officer"));


        String f3 = "C:\\wks\\leonbookinsKarpathos\\src\\main\\resources\\pdf\\EN+-+Award+notification+-+successful+tenderer 5.pdf";
        System.out.println("file1 contains placeholder : " +PMPdfUtils.doesContainPlaceHolder(getInputStreamFromFullFilePath(f3), "acrofield-authorising-officer"));

//
//        ClassPathResource resourceSuccess1 = new ClassPathResource("/pdf/EN+-+Award+notification+-+successful+tenderer 1.pdf");
//        System.out.println("file1 contains placeholder : " +PMPdfUtils.placeholderContained(resourceSuccess1.getInputStream(), "acrofield-authorising-officer"));
//        ClassPathResource resourceSFail1 = new ClassPathResource("/pdf/EN+-+Award+notification+-+unsuccessful+tenderer 1.pdf");
//        System.out.println("file2 contains placeholder : " +PMPdfUtils.placeholderContained(resourceSFail1.getInputStream(), "acrofield-authorising-officer"));



    }

}

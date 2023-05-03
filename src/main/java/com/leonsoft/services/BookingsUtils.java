package com.leonsoft.services;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

public class BookingsUtils {

    public static DateTimeFormatter formatterEURO = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static DateTimeFormatter formatterISO = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public  static final DecimalFormat df = new DecimalFormat("0.00");


}

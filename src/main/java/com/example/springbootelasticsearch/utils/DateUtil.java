package com.example.springbootelasticsearch.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {

    public static String formatDate(Date date){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhssmm");
        return sdf.format(date);
    }
}

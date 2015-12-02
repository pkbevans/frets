package com.example;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyClass {

    public static void main(String[] args) {
        String input="<event> xy=\"holy shmolly\" dt=\"54\" na=\"239898sdfsdf9bs9b \"</event>";
        String mytag="xy";
        System.out.println("tag:"+mytag+"=["+getTagString(input, mytag)+"]");
        mytag="dt";
        System.out.println("tag:"+mytag+"=["+getTagString(input, mytag)+"]");
        mytag="na";
        System.out.println("tag:"+mytag+"=["+getTagString(input, mytag)+"]");
    }
    static String getTagString(String ev, String tag){
        String ret="";
        Pattern tagPattern = Pattern.compile(tag+"=\"(.*?)[\"<]",
                Pattern.DOTALL|Pattern.CASE_INSENSITIVE);
        Matcher m = tagPattern.matcher(ev);
        if (m.find()) { // Find each match in turn;
            ret = m.group(1); // Access a submatch group;
        }
        return ret;
    }

}
